package se.sics.ktoolbox.election;

import java.security.PublicKey;
import java.util.*;
import java.util.UUID;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.ktoolbox.election.aggregation.LeaderHistoryReducer;
import se.sics.ktoolbox.election.aggregation.LeaderUpdatePacket;
import se.sics.ktoolbox.election.util.LCPeerView;
import se.sics.ktoolbox.election.util.LEContainer;
import se.sics.ktoolbox.election.event.ExtensionUpdate;
import se.sics.ktoolbox.election.event.LeaderState;
import se.sics.ktoolbox.election.event.ViewUpdate;
import se.sics.ktoolbox.election.junk.MockedGradientUpdate;
import se.sics.ktoolbox.election.api.ports.LeaderElectionPort;
import se.sics.ktoolbox.election.junk.TestPort;
import se.sics.ktoolbox.election.rules.LCRuleSet;
import se.sics.ktoolbox.election.msg.ExtensionRequest;
import se.sics.ktoolbox.election.msg.LeaseCommitUpdated;
import se.sics.ktoolbox.election.msg.Promise;
import se.sics.ktoolbox.election.util.ElectionHelper;
import se.sics.ktoolbox.election.util.PromiseResponseTracker;
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.gradient.event.GradientSample;
import se.sics.ktoolbox.util.address.AddressUpdate;
import se.sics.ktoolbox.util.address.AddressUpdatePort;
import se.sics.ktoolbox.util.aggregation.CompTracker;
import se.sics.ktoolbox.util.aggregation.CompTrackerImpl;
import se.sics.ktoolbox.util.identifiable.Identifiable;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.other.Container;

/**
 * Leader Election Component.
 * <p/>
 * This is the core component which is responsible for election and the
 * maintenance of the leader in the system. The nodes are constantly analyzing
 * the samples from the sampling service and based on the convergence tries to
 * assert themselves as the leader.
 * <p/>
 * <br/><br/>
 * <p/>
 * In addition to this, this component works on leases in which the leader
 * generates a lease and adds could happen only for that lease. The leader, when
 * the lease is on the verge of expiring tries to renew the lease by sending a
 * special message to the nodes in its view.
 * <p/>
 * <br/><br/>
 * <p/>
 * <b>NOTE: </b> The lease needs to be short enough so that if the leader dies,
 * the system is not in a transitive state.
 * <p/>
 * <b>CAUTION: </b> Under development, so unstable to use as it is.
 */
public class ElectionLeader extends ComponentDefinition {

    Logger LOG = LoggerFactory.getLogger(ElectionLeader.class);
    String logPrefix = "";

    private LCPeerView selfLCView;
    private LEContainer selfLEContainer;
    private ElectionConfig config;
    private LCRuleSet lcRuleSet;
    private KAddress self;
    private Map<Identifier, LEContainer> addressContainerMap = new HashMap<>();
    private int leaderGroupSize;

    // Promise Sub Protocol.
    private UUID electionRoundId;
    private UUID promisePhaseTimeout;
    private UUID leaseCommitPhaseTimeout;
    private PromiseResponseTracker electionRoundTracker;
    private PublicKey publicKey;

    private UUID leaseTimeoutId;

    // Convergence Variables.
    int convergenceCounter;
    boolean isConverged;
    boolean inElection = false;
    boolean applicationAck = false;

    // LE Container View.
    private SortedSet<LEContainer> higherUtilityNodes;
    private SortedSet<LEContainer> lowerUtilityNodes;

    private Comparator<LCPeerView> lcPeerViewComparator;
    private Comparator<LEContainer> leContainerComparator;

    // Ports.
    Positive timer = requires(Timer.class);
    Positive network = requires(Network.class);
    Positive gradient = requires(GradientPort.class);
    Positive addressUpdate = requires(AddressUpdatePort.class);
    Negative<LeaderElectionPort> election = provides(LeaderElectionPort.class);
    Negative<TestPort> testPortNegative = provides(TestPort.class);

    private final ElectionKCWrapper electionConfig;
    private CompTracker compTracker;

    public ElectionLeader(ElectionInit<ElectionLeader> init) {

        config = init.electionConfig;
        electionConfig = new ElectionKCWrapper(config());
        lcRuleSet = init.lcRuleSet;
        self = init.selfAddress;
        publicKey = init.publicKey;
        logPrefix = "<nid:" + self.getId() + "> ";

        LOG.info("{}Election Leader component initialized", logPrefix);
        LOG.info("{}Election Config: {}", logPrefix, config);

        // voting protocol.
        isConverged = false;
        electionRoundTracker = new PromiseResponseTracker();

        leaderGroupSize = Math.min(config.getViewSize() / 2 + 1, config.getMaxLeaderGroupSize());
        selfLCView = init.initialView;
        selfLEContainer = new LEContainer(self, selfLCView);

        lcPeerViewComparator = init.comparator;
        this.leContainerComparator = new Comparator<LEContainer>() {
            @Override
            public int compare(LEContainer o1, LEContainer o2) {

                if (o1 == null || o2 == null) {
                    throw new IllegalArgumentException("Can't compare null values");
                }

                LCPeerView view1 = o1.getLCPeerView();
                LCPeerView view2 = o2.getLCPeerView();

                return lcPeerViewComparator.compare(view1, view2);
            }
        };

        lowerUtilityNodes = new TreeSet<LEContainer>(leContainerComparator);
        higherUtilityNodes = new TreeSet<LEContainer>(leContainerComparator);

        setCompTracker();

        subscribe(handleStart, control);
        subscribe(handleSelfAddressUpdate, addressUpdate);

        subscribe(gradientSampleHandler, gradient);
        subscribe(viewUpdateHandler, election);
        subscribe(periodicVotingHandler, timer);

        // Test Sample
        subscribe(mockedUpdateHandler, testPortNegative);

        // Promise Subscriptions.
        subscribe(promiseResponse, network);
        subscribe(promiseRoundTimeoutHandler, timer);

        // Lease Subscriptions.
        subscribe(leaseTimeoutHandler, timer);
        subscribe(leaseCommitResponseHandler, network);
        subscribe(leaseResponseTimeoutHandler, timer);
    }

    //**************************CONTROL*****************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);

            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(0, 8000);
            spt.setTimeoutEvent(new TimeoutCollection.PeriodicVoting(spt));
            trigger(spt, timer);

            compTracker.start();
        }
    };

    Handler handleSelfAddressUpdate = new Handler<AddressUpdate.Indication>() {
        @Override
        public void handle(AddressUpdate.Indication update) {
            LOG.info("{}update self address:{}", logPrefix, update.localAddress);
            self = update.localAddress;
        }
    };

    //***************************STATE TRACKING*********************************
    private void setCompTracker() {
        switch (electionConfig.electionAggLevel) {
            case NONE:
                compTracker = new CompTrackerImpl(proxy, Pair.with(LOG, logPrefix), electionConfig.electionAggPeriod);
                break;
            case BASIC:
                compTracker = new CompTrackerImpl(proxy, Pair.with(LOG, logPrefix), electionConfig.electionAggPeriod);
                setEventTracking();
                break;
            case FULL:
                compTracker = new CompTrackerImpl(proxy, Pair.with(LOG, logPrefix), electionConfig.electionAggPeriod);
                setStateTracking();
                setEventTracking();
                break;
            default:
                throw new RuntimeException("Undefined:" + electionConfig.electionAggLevel);
        }
    }

    private void setStateTracking() {
        compTracker.registerReducer(new LeaderHistoryReducer());
    }

    private void setEventTracking() {
        compTracker.registerPositivePort(network);
        compTracker.registerPositivePort(timer);
        compTracker.registerNegativePort(gradient);
        compTracker.registerPositivePort(addressUpdate);
        compTracker.registerNegativePort(election);
        //TODO Alex - what is all this testing code doing here? - cleanup when time
        compTracker.registerNegativePort(testPortNegative);
        //TODO Alex - shouldn't I have a view update here as well
//        compTracker.registerPositivePort(viewUpdate);
    }

    //**************************************************************************
    /**
     * Check if the periodic voting needs to be started based on the data in
     * terms of the convergence protocols.
     *
     */
    Handler periodicVotingHandler = new Handler<TimeoutCollection.PeriodicVoting>() {
        @Override
        public void handle(TimeoutCollection.PeriodicVoting periodicVoting) {

            LOG.debug("Triggering periodic voting timeout ");
            checkIfLeader();
        }
    };

    Handler mockedUpdateHandler = new Handler<MockedGradientUpdate>() {
        @Override
        public void handle(MockedGradientUpdate event) {

            // Incorporate the new sample.
            Map<Identifier, LEContainer> oldContainerMap = addressContainerMap;
            addressContainerMap = ElectionHelper.addGradientSample(event.collection);

            // Check how much the sample changed.
            if (ElectionHelper.isRoundConverged(oldContainerMap.keySet(), addressContainerMap.keySet(), config.getConvergenceTest())) {
                if (!isConverged) {

                    convergenceCounter++;
                    if (convergenceCounter >= config.getConvergenceRounds()) {
                        isConverged = true;
                    }
                }
            } else {
                convergenceCounter = 0;
                if (isConverged) {
                    isConverged = false;
                }
            }

            // Update the views.
            Pair<SortedSet<LEContainer>, SortedSet<LEContainer>> lowerAndHigherViewPair = ElectionHelper.getHigherAndLowerViews(addressContainerMap.values(), leContainerComparator, selfLEContainer);
            lowerUtilityNodes = lowerAndHigherViewPair.getValue0();
            higherUtilityNodes = lowerAndHigherViewPair.getValue1();

            checkIfLeader();
        }
    };

    Handler viewUpdateHandler = new Handler<ViewUpdate>() {
        @Override
        public void handle(ViewUpdate viewUpdate) {

            LCPeerView oldView = selfLCView;
            selfLCView = viewUpdate.selfPv;
            selfLEContainer = new LEContainer(self, selfLCView);

            LOG.trace("{}: Received view update from the application: {} ", logPrefix, selfLCView.toString());

            isConverged = false;
            convergenceCounter = 0;

            // This part of tricky to understand. The follower component works independent of the leader component.
            // In order to prevent the leader from successive tries while waiting on the update from the application regarding being in the group membership or not, currently
            // the node starts an election round with a unique id and in case it reaches the lease commit phase the outcome is not deterministic as the responses might be late or something.
            // So we reset the election round only when we receive an update from the application with the same roundid.
            //
            // ElectionLeader -> ElectionFollower -> Application : broadcast (ElectionFollower || ElectionLeader).
            // Got some view update from the application and I am currently in election.
            if (viewUpdate.electionRoundId != null && inElection) {

                if (electionRoundId != null && electionRoundId.equals(viewUpdate.electionRoundId)) {

                    if (electionRoundTracker.getRoundId() != null && electionRoundTracker.getRoundId().equals(viewUpdate.electionRoundId)) {
                        applicationAck = true;  // I am currently tracking the round and as application being fast I received the ack for the round from application.
                    } else {
                        resetElectionMetaData(); // Finally the application update has arrived and now I can let go of the election round.
                    }
                }
            }

            // The application using the protocol can direct it to forcefully terminate the leadership in case 
            // a specific event happens at the application end. The decision is implemented using the filter which the application injects in the
            // protocol during the booting up.
            if (lcRuleSet.terminateLeadership(oldView, selfLCView)) {

                CancelTimeout ct = new CancelTimeout(leaseTimeoutId);
                trigger(ct, timer);
                leaseTimeoutId = null;

                terminateBeingLeader();
            }

        }
    };

    /**
     * Handler for the gradient sample that we receive from the gradient in the
     * system. Incorporate the gradient sample to recalculate the convergence
     * and update the view of higher or lower utility nodes.
     */
    Handler gradientSampleHandler = new Handler<GradientSample>() {

        @Override
        public void handle(GradientSample event) {
            StringBuilder sb = new StringBuilder();
            Set<Identifier> ids = new TreeSet<>();
            for (Container<Identifiable, ?> c : (List<Container>) event.gradientSample) {
                sb.append("\nlec: gradient:" + c);
                ids.add(c.getSource().getId());
            }
            LOG.trace("{} gradient sample:{}", logPrefix, ids);
            LOG.trace("{}", sb);

            // Incorporate the new sample.
            Map<Identifier, LEContainer> oldContainerMap = addressContainerMap;
            addressContainerMap = ElectionHelper.addGradientSample(event.gradientSample);

            // Check how much the sample changed.
            if (ElectionHelper.isRoundConverged(oldContainerMap.keySet(), addressContainerMap.keySet(), config.getConvergenceTest())) {

                if (!isConverged) {

                    convergenceCounter++;
                    if (convergenceCounter >= config.getConvergenceRounds()) {
                        isConverged = true;
                    }
                }
            } else {
                convergenceCounter = 0;
                if (isConverged) {
                    isConverged = false;
                }
            }

            // Update the views.
            Pair<SortedSet<LEContainer>, SortedSet<LEContainer>> lowerAndHigherViewPair = ElectionHelper.getHigherAndLowerViews(addressContainerMap.values(), leContainerComparator, selfLEContainer);
            lowerUtilityNodes = lowerAndHigherViewPair.getValue0();
            higherUtilityNodes = lowerAndHigherViewPair.getValue1();

        }
    };

    /**
     * The node after incorporating the sample, checks if it is in a position to
     * assert itself as a leader.
     */
    private void checkIfLeader() {

        // I don't see anybody above me, so should start voting.
        // Addition lease check is required because for the nodes which are in the node group will be acting under
        // lease of the leader, with special variable check.
        if (isConverged && higherUtilityNodes.size() == 0 && !inElection && !selfLCView.isLeaderGroupMember()) {
            if (addressContainerMap.size() < config.getViewSize()) {
                LOG.info("{}: I think I am leader but the view:{} less than the minimum requirement, so returning.", logPrefix, addressContainerMap.size());
                return;
            }

            startVoting();
        }
    }

    /**
     * In case the node sees itself a candidate for being the leader, it
     * initiates voting.
     */
    private void startVoting() {

        LOG.info("{}Starting with the voting .. ", logPrefix);
        electionRoundId = UUID.randomUUID();
        applicationAck = false;

        Collection<KAddress> leaderGroupAddress = lcRuleSet.initiateLeadership(new LEContainer(self, selfLCView), addressContainerMap.values(), leaderGroupSize);

        if (leaderGroupAddress.isEmpty() || leaderGroupAddress.size() < leaderGroupSize) {
            LOG.info("{}Not asserting self as leader as the leader group size:{},  is less than required.", logPrefix, leaderGroupAddress.size());
            return;
        }

        Promise.Request request = new Promise.Request(UUIDIdentifier.randomId(), self, selfLCView, electionRoundId);
        // Add SELF to the leader group nodes.
        leaderGroupAddress.add(self);

        inElection = true;
        for (KAddress address : leaderGroupAddress) {
            LOG.debug("Sending promise request to : {}", logPrefix);
            BasicHeader header = new BasicHeader(self, address, Transport.UDP);
            BasicContentMsg promiseRequest = new BasicContentMsg(header, request);
            trigger(promiseRequest, network);
        }

        electionRoundTracker.startTracking(electionRoundId, leaderGroupAddress);

        ScheduleTimeout st = new ScheduleTimeout(5000);
        st.setTimeoutEvent(new TimeoutCollection.PromiseRoundTimeout(st));
        promisePhaseTimeout = st.getTimeoutEvent().getTimeoutId();

        trigger(st, timer);
    }

    ClassMatchedHandler promiseResponse
            = new ClassMatchedHandler<Promise.Response, BasicContentMsg<KAddress, BasicHeader<KAddress>, Promise.Response>>() {

                @Override
                public void handle(Promise.Response response, BasicContentMsg<KAddress, BasicHeader<KAddress>, Promise.Response> event) {

                    LOG.debug("{}Received Promise Response from:{} ", logPrefix, event.getSource().getId());
                    int numPromises = electionRoundTracker.addPromiseResponseAndGetSize(response);

                    if (numPromises >= electionRoundTracker.getLeaderGroupInformationSize()) {

                        CancelTimeout cancelTimeout = new CancelTimeout(promisePhaseTimeout);
                        trigger(cancelTimeout, timer);
                        promisePhaseTimeout = null;

                        if (electionRoundTracker.isAccepted()) {

                            LOG.info("{}: All the leader group nodes have promised.", logPrefix);
                            LeaseCommitUpdated.Request request = new LeaseCommitUpdated.Request(UUIDIdentifier.randomId(), self,
                                    publicKey, selfLCView, electionRoundTracker.getRoundId());

                            for (KAddress address : electionRoundTracker.getLeaderGroupInformation()) {
                                LOG.debug("{}Sending Commit Request to:{}", logPrefix, address.getId());

                                BasicHeader decoratedHeader = new BasicHeader(self, address, Transport.UDP);
                                BasicContentMsg commitRequest = new BasicContentMsg(decoratedHeader, request);
                                trigger(commitRequest, network);
                            }

                            ScheduleTimeout st = new ScheduleTimeout(5000);
                            st.setTimeoutEvent(new TimeoutCollection.LeaseCommitResponseTimeout(st));
                            leaseCommitPhaseTimeout = st.getTimeoutEvent().getTimeoutId();
                            trigger(st, timer);
                        } else {

                            isConverged = false;
                            convergenceCounter = 0;
                            inElection = false;
                            electionRoundTracker.resetTracker();
                        }
                    }

                }
            };

    /**
     * Handler for the response that the node receives as part of the lease
     * commit phase. Aggregate the responses and check if every node has
     * committed.
     */
    ClassMatchedHandler leaseCommitResponseHandler
            = new ClassMatchedHandler<LeaseCommitUpdated.Response, BasicContentMsg<KAddress, BasicHeader<KAddress>, LeaseCommitUpdated.Response>>() {

                @Override
                public void handle(LeaseCommitUpdated.Response response, BasicContentMsg<KAddress, BasicHeader<KAddress>, LeaseCommitUpdated.Response> event) {

                    LOG.debug("{}Received lease commit response from the node:{}, response:{}",
                            new Object[]{logPrefix, event.getSource().getId(), response.isCommit});

                    int commitResponses = electionRoundTracker.addLeaseCommitResponseAndgetSize(event.getContent());
                    if (commitResponses >= electionRoundTracker.getLeaderGroupInformationSize()) {

                        CancelTimeout cancelTimeout = new CancelTimeout(leaseCommitPhaseTimeout);
                        trigger(cancelTimeout, timer);
                        leaseCommitPhaseTimeout = null;

                        if (electionRoundTracker.isLeaseCommitAccepted()) {

                            LOG.warn("{}: All the leader group nodes have committed the lease.", logPrefix);
                            trigger(new LeaderState.ElectedAsLeader(UUIDIdentifier.randomId(), electionRoundTracker.getLeaderGroupInformation()), election);
                            compTracker.updateState(LeaderUpdatePacket.update(self.getId(), new ArrayList<>(electionRoundTracker.getLeaderGroupInformation())));

                            ScheduleTimeout st = new ScheduleTimeout(config.getLeaderLeaseTime());
                            st.setTimeoutEvent(new TimeoutCollection.LeaseTimeout(st));

                            leaseTimeoutId = st.getTimeoutEvent().getTimeoutId();
                            trigger(st, timer);

                            LOG.error("Setting self as leader complete.");
                        }

                        if (applicationAck) {
                            applicationAck = false;
                            resetElectionMetaData();
                        }

                        // Seems my application component is kind of running late and therefore I still have not received
                        // ack from the application, even though the follower seems to have sent it to the application.
                        electionRoundTracker.resetTracker();
                    }
                }
            };

    /**
     * The round for getting the promises from the nodes in the system, timed
     * out and therefore there is no need to wait for them. Reset the round
     * tracker variable and the election phase.
     */
    Handler promiseRoundTimeoutHandler = new Handler<TimeoutCollection.PromiseRoundTimeout>() {
        @Override
        public void handle(TimeoutCollection.PromiseRoundTimeout event) {

            if (promisePhaseTimeout != null && promisePhaseTimeout.equals(event.getTimeoutId())) {
                LOG.warn("{}Election Round Timed Out in the promise phase.", logPrefix);
                resetElectionMetaData();
                electionRoundTracker.resetTracker();
            } else {
                LOG.warn("Promise already supposed to be fulfilled but timeout triggered");
            }

        }
    };

    private void resetElectionMetaData() {
        inElection = false;
        electionRoundId = null;
    }

    /**
     * Handler on the leader component indicating that node couldn't receive all
     * the commit responses associated with the lease were not received on time
     * and therefore it has to reset the state information.
     */
    Handler leaseResponseTimeoutHandler = new Handler<TimeoutCollection.LeaseCommitResponseTimeout>() {
        @Override
        public void handle(TimeoutCollection.LeaseCommitResponseTimeout event) {

            LOG.info("{}Election Round timed out in the lease commit phase.", logPrefix);
            if (leaseCommitPhaseTimeout != null && leaseCommitPhaseTimeout.equals(event.getTimeoutId())) {

                electionRoundTracker.resetTracker();

                if (applicationAck) {

                    applicationAck = false;
                    resetElectionMetaData(); // Reset election phase if already received ack for the commit that I sent to local follower component.
                }
            } else {
                LOG.debug("{}Received the timeout after being cancelled.", logPrefix);
            }

        }
    };

    /**
     * Lease for the current round timed out. Now we need to reset some
     * parameters in order to let other nodes to try and assert themselves as
     * leader.
     */
    Handler leaseTimeoutHandler = new Handler<TimeoutCollection.LeaseTimeout>() {
        @Override
        public void handle(TimeoutCollection.LeaseTimeout event) {
            LOG.debug("{}Special : Lease Timed out at Leader End: {}, trying to extend the lease", logPrefix);

            if (leaseTimeoutId != null && leaseTimeoutId.equals(event.getTimeoutId())) {

                Collection<KAddress> lgNodes = lcRuleSet.continueLeadership(new LEContainer(self, selfLCView), addressContainerMap.values(), leaderGroupSize);

                Set<Identifier> lgIds = new TreeSet<>();
                for (KAddress lgNode : lgNodes) {
                    lgIds.add(lgNode.getId());
                }
                Set<Identifier> lecIds = new TreeSet<>();
                StringBuilder lecSb = new StringBuilder("");
                for (LEContainer lecNode : addressContainerMap.values()) {
                    lecIds.add(lecNode.getSource().getId());
                    lecSb.append("\n lec:" + lecNode);
                }
                LOG.trace("{}lec: lg nodes:{} lec nodes:{}", new Object[]{logPrefix, lgIds, lecIds});
                LOG.trace("{}", lecSb.toString());
                
                if (lgNodes.isEmpty() || lgNodes.size() < leaderGroupSize) {
                    LOG.info("{}: Will Not extend the lease anymore.", logPrefix);
                    terminateBeingLeader();
                } else {

                    LOG.info("{}: Trying to extend the leadership.", logPrefix);

                    lgNodes.add(self);
                    UUID roundId = UUID.randomUUID();
                    ExtensionRequest request = new ExtensionRequest(UUIDIdentifier.randomId(), self, publicKey, selfLCView, roundId);

                    for (KAddress memberAddress : lgNodes) {
                        BasicHeader header = new BasicHeader(self, memberAddress, Transport.UDP);
                        BasicContentMsg extensionRequest = new BasicContentMsg(header, request);
                        trigger(extensionRequest, network);
                    }

                    //TODO Alex - inform app about leadergroup before they answer?
                    // Inform the application about the updated group membership.
                    ExtensionUpdate extensionUpdate = new ExtensionUpdate(UUIDIdentifier.randomId(), new ArrayList<>(lgNodes));
                    trigger(extensionUpdate, election);
                    compTracker.updateState(LeaderUpdatePacket.update(self.getId(), new ArrayList<>(lgNodes)));

                    // Extend the lease.
                    ScheduleTimeout st = new ScheduleTimeout(config.getLeaderLeaseTime());
                    st.setTimeoutEvent(new TimeoutCollection.LeaseTimeout(st));
                    leaseTimeoutId = st.getTimeoutEvent().getTimeoutId();
                    trigger(st, timer);
                }
            }
        }
    };

    /**
     * In case the leader sees some node above himself, then keeping in mind the
     * fairness policy the node should terminate.
     */
    private void terminateBeingLeader() {

        // Disable leadership and membership.
        resetElectionMetaData();
        trigger(new LeaderState.TerminateBeingLeader(), election);
    }

}

package se.sics.p2ptoolbox.election.core;

import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.p2ptoolbox.election.api.LCPeerView;
import se.sics.p2ptoolbox.election.api.LEContainer;
import se.sics.p2ptoolbox.election.api.msg.ExtensionUpdate;
import se.sics.p2ptoolbox.election.api.msg.LeaderState;
import se.sics.p2ptoolbox.election.api.msg.ViewUpdate;
import se.sics.p2ptoolbox.election.api.msg.mock.MockedGradientUpdate;
import se.sics.p2ptoolbox.election.api.ports.LeaderElectionPort;
import se.sics.p2ptoolbox.election.api.ports.TestPort;
import se.sics.p2ptoolbox.election.api.rules.LCRuleSet;
import se.sics.p2ptoolbox.election.core.data.ExtensionRequest;
import se.sics.p2ptoolbox.election.core.data.LeaseCommitUpdated;
import se.sics.p2ptoolbox.election.core.data.Promise;
import se.sics.p2ptoolbox.election.core.util.ElectionHelper;
import se.sics.p2ptoolbox.election.core.util.LeaderFilter;
import se.sics.p2ptoolbox.election.core.util.PromiseResponseTracker;
import se.sics.p2ptoolbox.election.core.util.TimeoutCollection;
import se.sics.p2ptoolbox.gradient.GradientPort;
import se.sics.p2ptoolbox.gradient.msg.GradientSample;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;

import java.security.PublicKey;
import java.util.*;
import java.util.UUID;
import se.sics.p2ptoolbox.util.update.SelfAddressUpdate;
import se.sics.p2ptoolbox.util.update.SelfAddressUpdatePort;

/**
 * Leader Election Component.
 * <p/>
 * This is the core component which is responsible for election and the maintenance of the leader in the system.
 * The nodes are constantly analyzing the samples from the sampling service and based on the convergence tries to assert themselves
 * as the leader.
 * <p/>
 * <br/><br/>
 * <p/>
 * In addition to this, this component works on leases in which the leader generates a lease
 * and adds could happen only for that lease. The leader,  when the lease is on the verge of expiring tries to renew the lease by sending a special message to the
 * nodes in its view.
 * <p/>
 * <br/><br/>
 * <p/>
 * <b>NOTE: </b> The lease needs to be short enough so that if the leader dies, the system is not in a transitive state.
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
    private DecoratedAddress self;
    private Map<BasicAddress, LEContainer> addressContainerMap;
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
    Positive gradientPort = requires(GradientPort.class);
    Positive addressUpdate = requires(SelfAddressUpdatePort.class);
    Negative<LeaderElectionPort> electionPort = provides(LeaderElectionPort.class);
    Negative<TestPort> testPortNegative = provides(TestPort.class);

    public ElectionLeader(ElectionInit<ElectionLeader> init) {

        this.config = init.electionConfig;
        this.lcRuleSet = init.lcRuleSet;
        this.self = init.selfAddress;
        this.publicKey = init.publicKey;
        this.logPrefix = self.getBase() + " ";

        LOG.warn("{}: Election Leader component initialized", self.getId());
        LOG.warn("{}: Election Config: {}", self.getId(), this.config);

        // voting protocol.
        isConverged = false;
        electionRoundTracker = new PromiseResponseTracker();

        this.leaderGroupSize = Math.min(config.getViewSize() / 2 + 1, config.getMaxLeaderGroupSize());
        this.selfLCView = init.initialView;
        this.selfLEContainer = new LEContainer(self, selfLCView);
        this.addressContainerMap = new HashMap<BasicAddress, LEContainer>();


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
        
        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleSelfAddressUpdate, addressUpdate);
        
        subscribe(gradientSampleHandler, gradientPort);
        subscribe(viewUpdateHandler, electionPort);
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
        }
    };
    
    Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            LOG.info("{}stopping...", logPrefix);
        }
    };
    
    Handler handleSelfAddressUpdate = new Handler<SelfAddressUpdate>() {
        @Override
        public void handle(SelfAddressUpdate update) {
            LOG.info("{}update self address:{}", logPrefix, update.self);
            self = update.self;
        }
    };

    //**************************************************************************

    /**
     * Check if the periodic voting needs to be started based on the data in terms of the convergence
     * protocols.
     *
     */
    Handler<TimeoutCollection.PeriodicVoting> periodicVotingHandler = new Handler<TimeoutCollection.PeriodicVoting>() {
        @Override
        public void handle(TimeoutCollection.PeriodicVoting periodicVoting) {

            LOG.debug("Triggering periodic voting timeout ");
            checkIfLeader();
        }
    };




    Handler<MockedGradientUpdate> mockedUpdateHandler = new Handler<MockedGradientUpdate>() {
        @Override
        public void handle(MockedGradientUpdate event) {

            // Incorporate the new sample.
            Map<BasicAddress, LEContainer> oldContainerMap = addressContainerMap;
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


    Handler<ViewUpdate> viewUpdateHandler = new Handler<ViewUpdate>() {
        @Override
        public void handle(ViewUpdate viewUpdate) {

            LCPeerView oldView = selfLCView;
            selfLCView = viewUpdate.selfPv;
            selfLEContainer = new LEContainer(self, selfLCView);

            LOG.trace(" {}: Received view update from the application: {} ", self.getId(), selfLCView.toString());

            isConverged= false;
            convergenceCounter =0;

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
     * Handler for the gradient sample that we receive from the gradient in the system.
     * Incorporate the gradient sample to recalculate the convergence and update the view of higher or lower utility nodes.
     */
    Handler<GradientSample> gradientSampleHandler = new Handler<GradientSample>() {

        @Override
        public void handle(GradientSample event) {

            LOG.trace("{}: Received sample from gradient", self.getId());

            // Incorporate the new sample.
            Map<BasicAddress, LEContainer> oldContainerMap = addressContainerMap;
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
     * The node after incorporating the sample, checks if it
     * is in a position to assert itself as a leader.
     */
    private void checkIfLeader() {

        // I don't see anybody above me, so should start voting.
        // Addition lease check is required because for the nodes which are in the node group will be acting under
        // lease of the leader, with special variable check.

        if (isConverged && higherUtilityNodes.size() == 0 && !inElection && !selfLCView.isLeaderGroupMember()) {
            if (addressContainerMap.size() < config.getViewSize()) {
                LOG.warn(" {}: I think I am leader but the view :{} less than the minimum requirement, so returning.", self.getId(), addressContainerMap.size());
                return;
            }

            startVoting();
        }
    }


    /**
     * In case the node sees itself a candidate for being the leader,
     * it initiates voting.
     */
    private void startVoting() {

        LOG.warn("{}: Starting with the voting .. ", self.getId());
        electionRoundId = UUID.randomUUID();
        applicationAck = false;

        Collection<DecoratedAddress> leaderGroupAddress = lcRuleSet.initiateLeadership(new LEContainer(self, selfLCView), addressContainerMap.values(), leaderGroupSize);

        if (leaderGroupAddress.isEmpty() || leaderGroupAddress.size() < leaderGroupSize) {
            LOG.error(" {} : Not asserting self as leader as the leader group size :{},  is less than required.", self.getId(), leaderGroupAddress.size());
            return;
        }

        Promise.Request request = new Promise.Request(self, selfLCView, electionRoundId);
        // Add SELF to the leader group nodes.
        leaderGroupAddress.add(self);

        inElection = true;
        for(DecoratedAddress address : leaderGroupAddress){

            LOG.debug("Sending promise request to : {}", address.getId());
            DecoratedHeader<DecoratedAddress> header = new DecoratedHeader<DecoratedAddress>(self, address, Transport.UDP);
            BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, Promise.Request> promiseRequest = new BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, Promise.Request>(header, request);
            trigger(promiseRequest, network);
        }

        electionRoundTracker.startTracking(electionRoundId, leaderGroupAddress);

        ScheduleTimeout st = new ScheduleTimeout(5000);
        st.setTimeoutEvent(new TimeoutCollection.PromiseRoundTimeout(st));
        promisePhaseTimeout = st.getTimeoutEvent().getTimeoutId();

        trigger(st, timer);
    }


    ClassMatchedHandler<Promise.Response, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, Promise.Response>> promiseResponse =
            new ClassMatchedHandler<Promise.Response, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, Promise.Response>>() {

                @Override
                public void handle(Promise.Response response, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, Promise.Response> event) {

                    LOG.warn("{}: Received Promise Response from : {} ", self.getId(), event.getSource().getId());
                    int numPromises = electionRoundTracker.addPromiseResponseAndGetSize(response);

                    if (numPromises >= electionRoundTracker.getLeaderGroupInformationSize()) {

                        CancelTimeout cancelTimeout = new CancelTimeout(promisePhaseTimeout);
                        trigger(cancelTimeout, timer);
                        promisePhaseTimeout = null;

                        if (electionRoundTracker.isAccepted()) {

                            LOG.warn("{}: All the leader group nodes have promised.", self.getId());
                            LeaseCommitUpdated.Request request = new LeaseCommitUpdated.Request(self,
                                    publicKey, selfLCView, electionRoundTracker.getRoundId());

                            for (DecoratedAddress address : electionRoundTracker.getLeaderGroupInformation()) {
                                LOG.warn("Sending Commit Request to : " + address.getId());

                                DecoratedHeader<DecoratedAddress> decoratedHeader = new DecoratedHeader<DecoratedAddress>(self, address, Transport.UDP);
                                BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, LeaseCommitUpdated.Request> commitRequest =
                                        new BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, LeaseCommitUpdated.Request>(decoratedHeader, request);

                                trigger(commitRequest, network);
                            }

                            ScheduleTimeout st = new ScheduleTimeout(5000);
                            st.setTimeoutEvent(new TimeoutCollection.LeaseCommitResponseTimeout(st));
                            leaseCommitPhaseTimeout = st.getTimeoutEvent().getTimeoutId();

                            trigger(st, timer);

                        } 
                        else {
                            
                            isConverged= false;
                            convergenceCounter =0;
                            inElection = false;
                            electionRoundTracker.resetTracker();
                        }
                    }

                }
            };


    /**
     * Handler for the response that the node receives as part of the lease commit phase. Aggregate the responses and check if every node has
     * committed.
     */
    ClassMatchedHandler<LeaseCommitUpdated.Response, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, LeaseCommitUpdated.Response>> leaseCommitResponseHandler =
            new ClassMatchedHandler<LeaseCommitUpdated.Response, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, LeaseCommitUpdated.Response>>() {

        @Override
        public void handle(LeaseCommitUpdated.Response response, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, LeaseCommitUpdated.Response> event) {

            LOG.warn("Received lease commit response from the node: {} , response: {}", event.getSource().getId(), response.isCommit);

            int commitResponses = electionRoundTracker.addLeaseCommitResponseAndgetSize(event.getContent());
            if (commitResponses >= electionRoundTracker.getLeaderGroupInformationSize()) {

                CancelTimeout cancelTimeout = new CancelTimeout(leaseCommitPhaseTimeout);
                trigger(cancelTimeout, timer);
                leaseCommitPhaseTimeout = null;

                if (electionRoundTracker.isLeaseCommitAccepted()) {

                    LOG.warn("{}: All the leader group nodes have committed the lease.", self.getId());
                    trigger(new LeaderState.ElectedAsLeader(electionRoundTracker.getLeaderGroupInformation()), electionPort);

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
     * The round for getting the promises from the nodes in the system, timed out and therefore there is no need to wait for them.
     * Reset the round tracker variable and the election phase.
     */
    Handler<TimeoutCollection.PromiseRoundTimeout> promiseRoundTimeoutHandler = new Handler<TimeoutCollection.PromiseRoundTimeout>() {
        @Override
        public void handle(TimeoutCollection.PromiseRoundTimeout event) {

            if (promisePhaseTimeout != null && promisePhaseTimeout.equals(event.getTimeoutId())) {
                LOG.warn("{}: Election Round Timed Out in the promise phase.", self.getId());
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
     * Handler on the leader component indicating that node couldn't receive all the
     * commit responses associated with the lease were not received on time and therefore it has to reset the state information.
     */
    Handler<TimeoutCollection.LeaseCommitResponseTimeout> leaseResponseTimeoutHandler = new Handler<TimeoutCollection.LeaseCommitResponseTimeout>() {
        @Override
        public void handle(TimeoutCollection.LeaseCommitResponseTimeout event) {

            LOG.warn("{}: Election Round timed out in the lease commit phase.", self.getId());
            if (leaseCommitPhaseTimeout != null && leaseCommitPhaseTimeout.equals(event.getTimeoutId())) {

                electionRoundTracker.resetTracker();

                if (applicationAck) {

                    applicationAck = false;
                    resetElectionMetaData(); // Reset election phase if already received ack for the commit that I sent to local follower component.
                }
            } else {
                LOG.warn("{}: Received the timeout after being cancelled.", self.getId());
            }

        }
    };

    /**
     * Lease for the current round timed out.
     * Now we need to reset some parameters in order to let other nodes to try and assert themselves as leader.
     */

    Handler<TimeoutCollection.LeaseTimeout> leaseTimeoutHandler = new Handler<TimeoutCollection.LeaseTimeout>() {
        @Override
        public void handle(TimeoutCollection.LeaseTimeout event) {

            LOG.debug(" Special : Lease Timed out at Leader End: {} , trying to extend the lease", self.getId());
            
            if(leaseTimeoutId != null && leaseTimeoutId.equals(event.getTimeoutId())){

                Collection<DecoratedAddress> lgNodes = lcRuleSet.continueLeadership(new LEContainer(self, selfLCView), addressContainerMap.values(), leaderGroupSize);

                if(lgNodes.isEmpty() || lgNodes.size()< leaderGroupSize){
                    LOG.warn("{}: Will Not extend the lease anymore.", self.getId());
                    terminateBeingLeader();
                }
                else {

                    LOG.warn("{}: Trying to extend the leadership.", self.getId());

                    lgNodes.add(self);
                    UUID roundId = UUID.randomUUID();
                    ExtensionRequest request = new ExtensionRequest(self, publicKey, selfLCView, roundId);

                    for (DecoratedAddress memberAddress : lgNodes) {

                        DecoratedHeader<DecoratedAddress> header = new DecoratedHeader<DecoratedAddress>(self, memberAddress, Transport.UDP);
                        BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, ExtensionRequest> extensionRequest = new BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, ExtensionRequest>(header, request);
                        trigger(extensionRequest, network);
                    }

                    // Inform the application about the updated group membership.
                    ExtensionUpdate extensionUpdate = new ExtensionUpdate(new ArrayList<DecoratedAddress>(lgNodes));
                    trigger(extensionUpdate, electionPort);

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
     * In case the leader sees some node above himself, then
     * keeping in mind the fairness policy the node should terminate.
     */
    private void terminateBeingLeader() {

        // Disable leadership and membership.
        resetElectionMetaData();
        trigger(new LeaderState.TerminateBeingLeader(), electionPort);
    }


    /**
     * Check if the leader is still suited to be the leader.
     *
     * @return extension possible
     */
    private boolean isExtensionPossible() {

        boolean extensionPossible = true;

        if (addressContainerMap.size() < config.getViewSize()) {
            extensionPossible = false;
        } else {

            SortedSet<LCPeerView> updatedSortedContainerSet = new TreeSet<LCPeerView>(lcPeerViewComparator);

            for( LEContainer container : addressContainerMap.values() ) {
                updatedSortedContainerSet.add( container.getLCPeerView().disableLGMembership());
            }

            LCPeerView updatedTempSelfView = selfLCView.disableLGMembership();
            if (updatedSortedContainerSet.tailSet(updatedTempSelfView).size() != 0) {
                extensionPossible = false;
            }
        }

        return extensionPossible;
    }


    /**
     * The leader needs to inform the application and other nodes about the
     * updated group membership. This method constructs the updated group member based on
     * the utility.
     *
     * @param groupSize group size.
     * @return leader group nodes.
     */
    private Collection<DecoratedAddress> constructLGNodes (int groupSize){

        Collection<DecoratedAddress> groupNodes = new ArrayList<DecoratedAddress>();
        TreeSet<LEContainer> lgDisabledSet = disableLGMembership(addressContainerMap.values());

        Iterator<LEContainer> itr = lgDisabledSet.descendingIterator();
        while(itr.hasNext() && groupSize > 0){
            groupNodes.add(itr.next().getSource());
            groupSize --;
        }

        return groupNodes;
    }


    /**
     * Construct an a sorted set with nodes which has the leader group membership
     * checked off.
     *
     * @return Sorted Set.
     */
    private TreeSet<LEContainer> disableLGMembership(Collection<LEContainer> collection){

        TreeSet<LEContainer> set = new TreeSet<LEContainer>(leContainerComparator);

        LEContainer update;
        for(LEContainer container : collection) {

            update = new LEContainer( container.getSource(),
                    container.getLCPeerView().disableLGMembership());

            set.add(update);
        }

        return set;
    }



    /**
     * Construct a collection of nodes which the leader thinks should be in the leader group
     *
     * @param size size of the leader group
     * @return Leader Group Collection.
     */
    private Collection<LEContainer> createLeaderGroupNodes(int size) {

        Collection<LEContainer> collection = new ArrayList<LEContainer>();

        if (size <= lowerUtilityNodes.size()) {
            Iterator<LEContainer> iterator = ((TreeSet) lowerUtilityNodes).descendingIterator();
            while (iterator.hasNext() && size > 0) {

                collection.add(iterator.next());
                size--;
            }
        }

        return collection;
    }

}

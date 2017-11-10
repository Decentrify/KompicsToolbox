package se.sics.ktoolbox.election;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
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
import se.sics.kompics.util.Identifier;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.election.aggregation.LeaderGroupHistoryReducer;
import se.sics.ktoolbox.election.aggregation.LeaderGroupUpdatePacket;
import se.sics.ktoolbox.election.api.ports.LeaderElectionPort;
import se.sics.ktoolbox.election.event.ElectionState;
import se.sics.ktoolbox.election.event.LeaderUpdate;
import se.sics.ktoolbox.election.event.ViewUpdate;
import se.sics.ktoolbox.election.junk.MockedGradientUpdate;
import se.sics.ktoolbox.election.junk.TestPort;
import se.sics.ktoolbox.election.msg.ExtensionRequest;
import se.sics.ktoolbox.election.msg.LeaseCommitUpdated;
import se.sics.ktoolbox.election.msg.Promise;
import se.sics.ktoolbox.election.rules.CohortsRuleSet;
import se.sics.ktoolbox.election.util.ElectionHelper;
import se.sics.ktoolbox.election.util.LCPeerView;
import se.sics.ktoolbox.election.util.LEContainer;
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.gradient.event.GradientSample;
import se.sics.ktoolbox.util.aggregation.CompTracker;
import se.sics.ktoolbox.util.aggregation.CompTrackerImpl;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;

/**
 * Election Follower part of the Leader Election Protocol.
 */
public class ElectionFollower extends ComponentDefinition {

    Logger LOG = LoggerFactory.getLogger(ElectionFollower.class);
    private String logPrefix;

    private final ElectionKCWrapper electionConfig;
    KAddress self;
    LCPeerView selfLCView;
    LEContainer selfContainer;
    private CohortsRuleSet cohortsRuleSet;
    private SortedSet<LEContainer> higherUtilityNodes;
    private SortedSet<LEContainer> lowerUtilityNodes;

    private Comparator<LCPeerView> lcPeerViewComparator;
    private Comparator<LEContainer> leContainerComparator;

    // Gradient Sample.
    int convergenceCounter = 0;
    private boolean isConverged;
    private Map<Identifier, LEContainer> addressContainerMap = new HashMap<>();

    // Leader Election.
    private UUID electionRoundId;
    private boolean inElection;

    private UUID awaitLeaseCommitId;
    private UUID leaseTimeoutId;
    private KAddress leaderAddress;

    // Ports.
    Positive<Network> network = requires(Network.class);
    Positive<Timer> timer = requires(Timer.class);
    Positive<GradientPort> gradient = requires(GradientPort.class);
    Negative<LeaderElectionPort> election = provides(LeaderElectionPort.class);
    Negative<TestPort> testPortNegative = provides(TestPort.class);

    private CompTracker compTracker;

    public ElectionFollower(ElectionInit<ElectionFollower> init) {
        electionConfig = new ElectionKCWrapper(config());
        self = init.selfAddress;
        logPrefix = "<nid:" + self.getId() + "> ";
        LOG.info("{}initiating..", logPrefix);
        cohortsRuleSet = init.cohortsRuleSet;

        selfLCView = init.initialView;
        selfContainer = new LEContainer(self, selfLCView);

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

        subscribe(gradientSampleHandler, gradient);
        subscribe(viewUpdateHandler, election);

        subscribe(mockedUpdateHandler, testPortNegative);

        subscribe(promiseHandler, network);
        subscribe(awaitLeaseCommitTimeoutHandler, timer);

        subscribe(commitRequestHandler, network);
        subscribe(leaseTimeoutHandler, timer);
        subscribe(extensionRequestHandler, network);
    }

    //**************************CONTROL*****************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting", logPrefix);
            compTracker.start();
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
    
    private void setEventTracking() {
        compTracker.registerPositivePort(network);
        compTracker.registerPositivePort(timer);
        compTracker.registerNegativePort(gradient);
        compTracker.registerNegativePort(election);
        //TODO Alex - what is all this testing code doing here? - cleanup when time
        compTracker.registerNegativePort(testPortNegative);
        //TODO Alex - shouldn't I have a view update here as well
//        compTracker.registerPositivePort(viewUpdate);
    }
    
    private void setStateTracking() {
        compTracker.registerReducer(new LeaderGroupHistoryReducer());
    }
    //**************************************************************************

    Handler mockedUpdateHandler = new Handler<MockedGradientUpdate>() {
        @Override
        public void handle(MockedGradientUpdate event) {
            LOG.trace("Received mocked update from the gradient");

            // Incorporate the new sample.
            Map<Identifier, LEContainer> oldContainerMap = addressContainerMap;
            addressContainerMap = ElectionHelper.addGradientSample(event.collection);

            // Check how much the sample changed.
            if (ElectionHelper.isRoundConverged(oldContainerMap.keySet(), addressContainerMap.keySet(), electionConfig.convergenceTest)) {
                if (!isConverged) {

                    convergenceCounter++;
                    if (convergenceCounter >= electionConfig.convergenceRounds) {
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
            Pair<SortedSet<LEContainer>, SortedSet<LEContainer>> lowerAndHigherViewPair = ElectionHelper.getHigherAndLowerViews(addressContainerMap.values(), leContainerComparator, selfContainer);
            lowerUtilityNodes = lowerAndHigherViewPair.getValue0();
            higherUtilityNodes = lowerAndHigherViewPair.getValue1();

        }
    };

    Handler gradientSampleHandler = new Handler<GradientSample>() {
        @Override
        public void handle(GradientSample event) {

            LOG.trace("{}Received gradient sample", logPrefix);

            // Incorporate the new sample.
            Map<Identifier, LEContainer> oldContainerMap = addressContainerMap;
            addressContainerMap = ElectionHelper.addGradientSample(event.gradientNeighbours);

            // Check how much the sample changed.
            if (ElectionHelper.isRoundConverged(oldContainerMap.keySet(), addressContainerMap.keySet(), electionConfig.convergenceTest)) {

                if (!isConverged) {

                    convergenceCounter++;
                    if (convergenceCounter >= electionConfig.convergenceRounds) {
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
            Pair<SortedSet<LEContainer>, SortedSet<LEContainer>> lowerAndHigherViewPair = ElectionHelper.getHigherAndLowerViews(addressContainerMap.values(), leContainerComparator, selfContainer);
            lowerUtilityNodes = lowerAndHigherViewPair.getValue0();
            higherUtilityNodes = lowerAndHigherViewPair.getValue1();

        }
    };

    Handler viewUpdateHandler = new Handler<ViewUpdate>() {
        @Override
        public void handle(ViewUpdate viewUpdate) {

            LCPeerView oldView = selfLCView;
            selfLCView = viewUpdate.selfPv;
            selfContainer = new LEContainer(self, selfLCView);

            // Resetting the in election check is kind of tricky so need to be careful about the procedure.
            if (viewUpdate.electionRoundId != null && inElection) {
                if (electionRoundId != null && electionRoundId.equals(viewUpdate.electionRoundId)) {
                    LOG.info("{}Resetting election check for the UUID: {}", logPrefix, electionRoundId);

                    inElection = false;
                    resetElectionMetaData();
                }
            }
        }
    };

    /**
     * Promise request from the node trying to assert itself as leader. The
     * request is sent to all the nodes in the system that the originator of
     * request seems fit and wants them to be a part of the leader group.
     */
    ClassMatchedHandler promiseHandler = new ClassMatchedHandler<Promise.Request, BasicContentMsg<KAddress, BasicHeader<KAddress>, Promise.Request>>() {
        @Override
        public void handle(Promise.Request request, BasicContentMsg<KAddress, BasicHeader<KAddress>, Promise.Request> container) {
            LOG.debug("{}Received promise request from:{}", logPrefix, container.getSource().getId());
            LCPeerView requestLeaderView = container.getContent().leaderView;
            boolean acceptCandidate = true;

            if (selfLCView.isLeaderGroupMember() || inElection) {
                // If part of leader group or already promised by being present in election, I deny promise.
                acceptCandidate = false;
            } else {

                if (request.leaderAddress.getId().equals(self.getId())) {
                    acceptCandidate = true; // Always accept self.
                } else {
                    acceptCandidate = cohortsRuleSet.validate(new LEContainer(container.getContent().leaderAddress, container.getContent().leaderView),
                            new LEContainer(self, selfLCView), addressContainerMap.values());
                }
            }

            // Update the election round msgId only if I decide to accept the candidate.
            if (acceptCandidate) {

                electionRoundId = container.getContent().electionRoundId;
                inElection = true;

                ScheduleTimeout st = new ScheduleTimeout(5000);
                st.setTimeoutEvent(new TimeoutCollection.AwaitLeaseCommitTimeout(st, electionRoundId));

                awaitLeaseCommitId = st.getTimeoutEvent().getTimeoutId();
                trigger(st, timer);
            }

            Promise.Response response = request.answer(acceptCandidate, isConverged, container.getContent().electionRoundId);
            BasicContentMsg promiseResponse = container.answer(response);
            trigger(promiseResponse, network);

        }
    };

    /**
     * Timeout Handler in case the node that had earlier extracted a promise
     * from this node didn't answer with the commit message in time.
     */
    Handler awaitLeaseCommitTimeoutHandler = new Handler<TimeoutCollection.AwaitLeaseCommitTimeout>() {
        @Override
        public void handle(TimeoutCollection.AwaitLeaseCommitTimeout event) {
            LOG.info("{}: The promise is not yet fulfilled with lease commit", logPrefix);

            if (awaitLeaseCommitId != null && awaitLeaseCommitId.equals(event.getTimeoutId())) {

                // Might be triggered even if the response is handled.
                inElection = false;
                electionRoundId = null;
                trigger(new ElectionState.DisableLGMembership(event.electionRoundId), election);

            } else {
                LOG.debug("Timeout triggered even though the cancel was sent.");
            }

        }
    };

    /**
     * Reset the meta data associated with the current election algorithm.
     */
    private void resetElectionMetaData() {
        electionRoundId = null;
    }

    /**
     * Received the lease commit request from the node trying to assert itself
     * as leader. Accept the request in case it is from the same round msgId.
     */
    ClassMatchedHandler commitRequestHandler = new ClassMatchedHandler<LeaseCommitUpdated.Request, BasicContentMsg<KAddress, BasicHeader<KAddress>, LeaseCommitUpdated.Request>>() {
        @Override
        public void handle(LeaseCommitUpdated.Request request, BasicContentMsg<KAddress, BasicHeader<KAddress>, LeaseCommitUpdated.Request> container) {

            LOG.debug("{}Received lease commit request from: {}", logPrefix, container.getSource().getId());
            LeaseCommitUpdated.Response response;

            if (electionRoundId == null || !electionRoundId.equals(request.electionRoundId)) {
                LOG.debug("{}Received an election response for the round id which has expired or timed out.", logPrefix);
                response = request.answer(false);
                BasicContentMsg commitResponse = container.answer(response);
                trigger(commitResponse, network);

            } else {
                response = request.answer(true);
                BasicContentMsg commitResponse = container.answer(response);
                trigger(commitResponse, network);

                // Cancel the existing awaiting for lease commit timeout.
                CancelTimeout timeout = new CancelTimeout(awaitLeaseCommitId);
                trigger(timeout, timer);
                awaitLeaseCommitId = null;

                LOG.info("{}My new leader: {}", logPrefix, request.leaderAddress);
                leaderAddress = request.leaderAddress;
                trigger(new ElectionState.EnableLGMembership(electionRoundId), election);
                trigger(new LeaderUpdate(request.leaderPublicKey, request.leaderAddress), election);
                compTracker.updateState(new LeaderGroupUpdatePacket(request.leaderAddress.getId()));

                ScheduleTimeout st = new ScheduleTimeout(electionConfig.followerLeaseTime);
                st.setTimeoutEvent(new TimeoutCollection.LeaseTimeout(st));

                leaseTimeoutId = st.getTimeoutEvent().getTimeoutId();
                trigger(st, timer);

            }
        }
    };

    /**
     * As soon as the lease expires reset all the parameters related to the node
     * being under lease. CHECK : If we also need to reset the parameters
     * associated with the current leader. (YES I THINK SO)
     */
    Handler leaseTimeoutHandler = new Handler<TimeoutCollection.LeaseTimeout>() {
        @Override
        public void handle(TimeoutCollection.LeaseTimeout event) {

            if (leaseTimeoutId != null && leaseTimeoutId.equals(event.getTimeoutId())) {

                LOG.info("{}Special : Lease timed out.", logPrefix);
                terminateElectionInformation();
            } else {
                LOG.info("{}Application current lease timeout id has changed", logPrefix);
            }

        }
    };

    /**
     * In case the lease times out or the application demands it, reset the
     * election state in the component.
     * <p/>
     * But do not be quick to dismiss the leader information and then send a
     * leader update to the application. In case I am no longer part of the
     * leader group, then I should eventually pull the information about the
     * current leader.
     */
    private void terminateElectionInformation() {

        leaderAddress = null;
        resetElectionMetaData();
        trigger(new ElectionState.DisableLGMembership(null), election); // round is doesnt matter at this moment.
        compTracker.updateState(new LeaderGroupUpdatePacket(null));
    }

    /**
     * Leader Extension request received. Node which is currently the leader is
     * trying to reassert itself as the leader again. The protocol with the
     * extension simply follows is that the leader should be allowed to continue
     * as leader.
     */
    ClassMatchedHandler extensionRequestHandler = new ClassMatchedHandler<ExtensionRequest, BasicContentMsg<KAddress, BasicHeader<KAddress>, ExtensionRequest>>() {
        @Override
        public void handle(ExtensionRequest leaseExtensionRequest, BasicContentMsg<KAddress, BasicHeader<KAddress>, ExtensionRequest> container) {

            LOG.debug("{}Received leader extension request from the node: {}", logPrefix, container.getSource().getId());
            if (selfLCView.isLeaderGroupMember()) {

                if (leaderAddress != null && !leaseExtensionRequest.leaderAddress.getId().equals(leaderAddress.getId())) {
                    LOG.warn("{}There might be a problem with the leader extension or a special case as I received lease extension from the node other than current leader that I already has set. ", logPrefix);
                }

                CancelTimeout cancelTimeout = new CancelTimeout(leaseTimeoutId);
                trigger(cancelTimeout, timer);
            }

            // Prevent the edge case in which the node which is under a lease might take time
            // to get a response back from the application representing the information and therefore in that time something fishy can happen.
            inElection = true;
            electionRoundId = leaseExtensionRequest.electionRoundId;
            trigger(new ElectionState.EnableLGMembership(electionRoundId), election);

            // Inform the component listening about the leader and schedule a new lease.
            trigger(new LeaderUpdate(leaseExtensionRequest.leaderPublicKey, leaseExtensionRequest.leaderAddress), election);

            compTracker.updateState(new LeaderGroupUpdatePacket(leaseExtensionRequest.leaderAddress.getId()));

            ScheduleTimeout st = new ScheduleTimeout(electionConfig.followerLeaseTime);
            st.setTimeoutEvent(new TimeoutCollection.LeaseTimeout(st));
            leaseTimeoutId = st.getTimeoutEvent().getTimeoutId();

            trigger(st, timer);
        }
    };

    /**
     * Analyze the views present locally and return the highest utility node.
     *
     * @return Highest utility Node.
     */
    public LCPeerView getHighestUtilityNode() {

        if (higherUtilityNodes.size() != 0) {
            return higherUtilityNodes.last().getLCPeerView();
        }

        return selfLCView;
    }

}

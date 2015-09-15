package se.sics.p2ptoolbox.election.core;

import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.*;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.election.api.LCPeerView;
import se.sics.p2ptoolbox.election.api.LEContainer;
import se.sics.p2ptoolbox.election.api.msg.ElectionState;
import se.sics.p2ptoolbox.election.api.msg.LeaderUpdate;
import se.sics.p2ptoolbox.election.api.msg.ViewUpdate;
import se.sics.p2ptoolbox.election.api.msg.mock.MockedGradientUpdate;
import se.sics.p2ptoolbox.election.api.ports.LeaderElectionPort;
import se.sics.p2ptoolbox.election.api.ports.TestPort;
import se.sics.p2ptoolbox.election.api.rules.CohortsRuleSet;
import se.sics.p2ptoolbox.election.core.data.ExtensionRequest;
import se.sics.p2ptoolbox.election.core.data.LeaseCommitUpdated;
import se.sics.p2ptoolbox.election.core.data.Promise;
import se.sics.p2ptoolbox.election.core.util.ElectionHelper;
import se.sics.p2ptoolbox.election.core.util.TimeoutCollection;
import se.sics.p2ptoolbox.gradient.GradientPort;
import se.sics.p2ptoolbox.gradient.msg.GradientSample;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;

import java.util.*;
import java.util.UUID;
import se.sics.p2ptoolbox.util.update.SelfAddressUpdate;
import se.sics.p2ptoolbox.util.update.SelfAddressUpdatePort;

/**
 * Election Follower part of the Leader Election Protocol.
 */
public class ElectionFollower extends ComponentDefinition {

    Logger LOG = LoggerFactory.getLogger(ElectionFollower.class);
    private String logPrefix;

    DecoratedAddress self;
    LCPeerView selfLCView;
    LEContainer selfContainer;
    private CohortsRuleSet cohortsRuleSet;
    private ElectionConfig config;
    private SortedSet<LEContainer> higherUtilityNodes;
    private SortedSet<LEContainer> lowerUtilityNodes;

    private Comparator<LCPeerView> lcPeerViewComparator;
    private Comparator<LEContainer> leContainerComparator;

    // Gradient Sample.
    int convergenceCounter = 0;
    private boolean isConverged;
    private Map<Integer, LEContainer> addressContainerMap;

    // Leader Election.
    private UUID electionRoundId;
    private boolean inElection;

    private UUID awaitLeaseCommitId;
    private UUID leaseTimeoutId;
    private DecoratedAddress leaderAddress;

    // Ports.
    Positive<Network> network = requires(Network.class);
    Positive<Timer> timer = requires(Timer.class);
    Positive<SelfAddressUpdatePort> addressUpdate = requires(SelfAddressUpdatePort.class);
    Positive<GradientPort> gradient = requires(GradientPort.class);
    Negative<LeaderElectionPort> election = provides(LeaderElectionPort.class);
    Negative<TestPort> testPortNegative = provides(TestPort.class);

    public ElectionFollower(ElectionInit<ElectionFollower> init) {
        config = init.electionConfig;
        self = init.selfAddress;
        logPrefix = self.getBase() + " ";
        LOG.info("{}initiating..", logPrefix);
        cohortsRuleSet = init.cohortsRuleSet;
        
        addressContainerMap = new HashMap<Integer, LEContainer>();

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

        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleSelfAddressUpdate, addressUpdate);

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
        }
    };

    Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            LOG.info("{}stopping", logPrefix);
        }
    };
    
    Handler handleSelfAddressUpdate = new Handler<SelfAddressUpdate>() {
        @Override
        public void handle(SelfAddressUpdate update) {
            LOG.info("{} update self address:{}", logPrefix, update.self);
            self = update.self;
        }
    };

    //**************************************************************************
    
    Handler<MockedGradientUpdate> mockedUpdateHandler = new Handler<MockedGradientUpdate>() {
        @Override
        public void handle(MockedGradientUpdate event) {
            LOG.trace("Received mocked update from the gradient");

            // Incorporate the new sample.
            Map<Integer, LEContainer> oldContainerMap = addressContainerMap;
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
            Pair<SortedSet<LEContainer>, SortedSet<LEContainer>> lowerAndHigherViewPair = ElectionHelper.getHigherAndLowerViews(addressContainerMap.values(), leContainerComparator, selfContainer);
            lowerUtilityNodes = lowerAndHigherViewPair.getValue0();
            higherUtilityNodes = lowerAndHigherViewPair.getValue1();

        }
    };

    Handler<GradientSample> gradientSampleHandler = new Handler<GradientSample>() {
        @Override
        public void handle(GradientSample event) {

            LOG.trace("{}: Received gradient sample", self.getId());

            // Incorporate the new sample.
            Map<Integer, LEContainer> oldContainerMap = addressContainerMap;
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
            Pair<SortedSet<LEContainer>, SortedSet<LEContainer>> lowerAndHigherViewPair = ElectionHelper.getHigherAndLowerViews(addressContainerMap.values(), leContainerComparator, selfContainer);
            lowerUtilityNodes = lowerAndHigherViewPair.getValue0();
            higherUtilityNodes = lowerAndHigherViewPair.getValue1();

        }
    };

    Handler<ViewUpdate> viewUpdateHandler = new Handler<ViewUpdate>() {
        @Override
        public void handle(ViewUpdate viewUpdate) {

            LCPeerView oldView = selfLCView;
            selfLCView = viewUpdate.selfPv;
            selfContainer = new LEContainer(self, selfLCView);

            // Resetting the in election check is kind of tricky so need to be careful about the procedure.
            if (viewUpdate.electionRoundId != null && inElection) {
                if (electionRoundId != null && electionRoundId.equals(viewUpdate.electionRoundId)) {
                    LOG.warn("{}: Resetting election check for the UUID: {}", self.getId(), electionRoundId);

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
    ClassMatchedHandler<Promise.Request, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, Promise.Request>> promiseHandler = new ClassMatchedHandler<Promise.Request, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, Promise.Request>>() {
        @Override
        public void handle(Promise.Request request, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, Promise.Request> event) {

            LOG.warn("{}: Received promise request from : {}", self.getId(), event.getSource().getId());
            LCPeerView requestLeaderView = event.getContent().leaderView;
            boolean acceptCandidate = true;

            if (selfLCView.isLeaderGroupMember() || inElection) {
                // If part of leader group or already promised by being present in election, I deny promise.
                acceptCandidate = false;
            } else {

                if (event.getContent().leaderAddress.getBase().equals(self.getBase())) {
                    acceptCandidate = true; // Always accept self.

                } else {

                    acceptCandidate = cohortsRuleSet.validate(new LEContainer(event.getContent().leaderAddress, event.getContent().leaderView),
                            new LEContainer(self, selfLCView), addressContainerMap.values());
                }
            }

            // Update the election round id only if I decide to accept the candidate.
            if (acceptCandidate) {

                electionRoundId = event.getContent().electionRoundId;
                inElection = true;

                ScheduleTimeout st = new ScheduleTimeout(5000);
                st.setTimeoutEvent(new TimeoutCollection.AwaitLeaseCommitTimeout(st, electionRoundId));

                awaitLeaseCommitId = st.getTimeoutEvent().getTimeoutId();
                trigger(st, timer);
            }

            Promise.Response response = new Promise.Response(acceptCandidate, isConverged, event.getContent().electionRoundId);
            DecoratedHeader<DecoratedAddress> header = new DecoratedHeader<DecoratedAddress>(self, event.getSource(), Transport.UDP);
            BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, Promise.Response> promiseResponse = new BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, Promise.Response>(header, response);

//             response = new LeaderPromiseMessage.Response(selfAddress, event.getVodSource(), event.id, new Promise.Response(acceptCandidate, isConverged, event.content.electionRoundId));
            trigger(promiseResponse, network);

        }
    };

    /**
     * Timeout Handler in case the node that had earlier extracted a promise
     * from this node didn't answer with the commit message in time.
     */
    Handler<TimeoutCollection.AwaitLeaseCommitTimeout> awaitLeaseCommitTimeoutHandler = new Handler<TimeoutCollection.AwaitLeaseCommitTimeout>() {
        @Override
        public void handle(TimeoutCollection.AwaitLeaseCommitTimeout event) {

            LOG.warn("{}: The promise is not yet fulfilled with lease commit", self.getId());

            if (awaitLeaseCommitId != null && awaitLeaseCommitId.equals(event.getTimeoutId())) {

                // Might be triggered even if the response is handled.
                inElection = false;
                electionRoundId = null;
                trigger(new ElectionState.DisableLGMembership(event.electionRoundId), election);

            } else {
                LOG.warn("Timeout triggered even though the cancel was sent.");
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
     * as leader. Accept the request in case it is from the same round id.
     */
    ClassMatchedHandler<LeaseCommitUpdated.Request, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, LeaseCommitUpdated.Request>> commitRequestHandler = new ClassMatchedHandler<LeaseCommitUpdated.Request, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, LeaseCommitUpdated.Request>>() {
        @Override
        public void handle(LeaseCommitUpdated.Request request, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, LeaseCommitUpdated.Request> event) {

            LOG.warn("{}: Received lease commit request from: {}", self.getId(), event.getSource().getId());
            LeaseCommitUpdated.Response response;

            if (electionRoundId == null || !electionRoundId.equals(request.electionRoundId)) {

                LOG.warn("{}: Received an election response for the round id which has expired or timed out.", self.getId());
                response = new LeaseCommitUpdated.Response(false, request.electionRoundId);
                DecoratedHeader<DecoratedAddress> header = new DecoratedHeader<DecoratedAddress>(self, event.getSource(), Transport.UDP);

                BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, LeaseCommitUpdated.Response> commitResponse = new BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, LeaseCommitUpdated.Response>(header, response);
                trigger(commitResponse, network);

            } else {

                response = new LeaseCommitUpdated.Response(true, request.electionRoundId);
                DecoratedHeader<DecoratedAddress> header = new DecoratedHeader<DecoratedAddress>(self, event.getSource(), Transport.UDP);

                BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, LeaseCommitUpdated.Response> commitResponse = new BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, LeaseCommitUpdated.Response>(header, response);
                trigger(commitResponse, network);

                // Cancel the existing awaiting for lease commit timeout.
                CancelTimeout timeout = new CancelTimeout(awaitLeaseCommitId);
                trigger(timeout, timer);
                awaitLeaseCommitId = null;

                LOG.warn("{}: My new leader: {}", self.getId(), request.leaderAddress);
                leaderAddress = request.leaderAddress;

                trigger(new ElectionState.EnableLGMembership(electionRoundId), election);
                trigger(new LeaderUpdate(request.leaderPublicKey, request.leaderAddress), election);

                ScheduleTimeout st = new ScheduleTimeout(config.getFollowerLeaseTime());
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
    Handler<TimeoutCollection.LeaseTimeout> leaseTimeoutHandler = new Handler<TimeoutCollection.LeaseTimeout>() {
        @Override
        public void handle(TimeoutCollection.LeaseTimeout event) {

            if (leaseTimeoutId != null && leaseTimeoutId.equals(event.getTimeoutId())) {

                LOG.warn("{}: Special : Lease timed out.", self.getId());
                terminateElectionInformation();
            } else {
                LOG.warn("{}: Application current lease timeout id has changed", self.getId());
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
    }

    /**
     * Leader Extension request received. Node which is currently the leader is
     * trying to reassert itself as the leader again. The protocol with the
     * extension simply follows is that the leader should be allowed to continue
     * as leader.
     */
    ClassMatchedHandler<ExtensionRequest, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, ExtensionRequest>> extensionRequestHandler = new ClassMatchedHandler<ExtensionRequest, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, ExtensionRequest>>() {
        @Override
        public void handle(ExtensionRequest leaseExtensionRequest, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, ExtensionRequest> event) {

            LOG.warn("{}: Received leader extension request from the node: {}", self.getId(), event.getSource().getId());
            if (selfLCView.isLeaderGroupMember()) {

                if (leaderAddress != null && !leaseExtensionRequest.leaderAddress.equals(leaderAddress)) {
                    LOG.warn("{}: There might be a problem with the leader extension or a special case as I received lease extension from the node other than current leader that I already has set. ", self.getId());
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

            ScheduleTimeout st = new ScheduleTimeout(config.getFollowerLeaseTime());
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

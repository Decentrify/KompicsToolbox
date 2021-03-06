package se.sics.p2ptoolbox.election.example.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.election.api.LCPeerView;
import se.sics.p2ptoolbox.election.api.ports.LeaderElectionPort;
import se.sics.p2ptoolbox.election.api.ports.TestPort;
import se.sics.p2ptoolbox.election.core.*;
import se.sics.p2ptoolbox.election.example.data.PeersUpdate;
import se.sics.p2ptoolbox.election.example.msg.AddPeers;
import se.sics.p2ptoolbox.election.example.ports.ApplicationPort;
import se.sics.p2ptoolbox.util.config.SystemConfig;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;

import java.util.Comparator;

/**
 * Main component the would encapsulate the other components.
 *
 * Created by babbar on 2015-04-01.
 */
public class HostManagerComp extends ComponentDefinition{

    Positive<Network> networkPositive = requires(Network.class);
    Positive<Timer> timerPositive = requires(Timer.class);
    Negative<ApplicationPort> applicationPort = provides(ApplicationPort.class);

    Logger logger = LoggerFactory.getLogger(HostManagerComp.class);

    private DecoratedAddress selfAddress;
    private LeaderDescriptor selfView;

    Component electionLeader, electionFollower;
    Component gradientMockUp;
    

    public HostManagerComp(HostManagerCompInit init){

        doInit(init);
        ElectionConfig config = init.electionConfig;

        // Create necessary components.
        electionLeader = create(ElectionLeader.class, new ElectionInit<ElectionLeader>(selfAddress, selfView, 100, config, null, null, init.lcpComparator, null, null));
        electionFollower = create(ElectionFollower.class, new ElectionInit<ElectionFollower>(selfAddress, selfView, 100, config, null, null, init.lcpComparator, null, null));
        gradientMockUp = create(GradientMockUp.class, new GradientMockUp.GradientMockUpInit(selfAddress));

        // Make the necessary connections.
        connect(electionLeader.getNegative(Network.class), networkPositive);
        connect(electionLeader.getNegative(Timer.class), timerPositive);

        connect(electionFollower.getNegative(Network.class), networkPositive);
        connect(electionFollower.getNegative(Timer.class), timerPositive);

        // Connections with the mock up component.
        connect(electionLeader.getPositive(LeaderElectionPort.class), gradientMockUp.getNegative(LeaderElectionPort.class));
        connect(electionFollower.getPositive(LeaderElectionPort.class), gradientMockUp.getNegative(LeaderElectionPort.class));

        connect(gradientMockUp.getNegative(Timer.class), timerPositive);

        connect(electionLeader.getPositive(TestPort.class), gradientMockUp.getNegative(TestPort.class));
        connect(electionFollower.getPositive(TestPort.class), gradientMockUp.getNegative(TestPort.class));

        // Handlers.
        subscribe(startHandler, control);
        subscribe(addPeersHandler, networkPositive);
    }

    private void doInit(HostManagerCompInit init) {

        selfAddress = init.systemConfig.self;
        selfView = new LeaderDescriptor(selfAddress.getId());
    }


    Handler<Start> startHandler = new Handler<Start>(){
        @Override
        public void handle(Start start) {
            logger.debug(" {}: Host Manager Component Started .... ", selfAddress.getId());
        }
    };




    ClassMatchedHandler<AddPeers, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, AddPeers>> addPeersHandler = new ClassMatchedHandler<AddPeers, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, AddPeers>>() {
        @Override
        public void handle(AddPeers addPeers, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, AddPeers> decoratedAddressDecoratedHeaderAddPeersBasicContentMsg) {
            logger.trace("Received add peers event from the simulator.... ");
            trigger(new PeersUpdate(addPeers.peers), gradientMockUp.getNegative(ApplicationPort.class));
        }
    };


    /**
     * Init class for the main component.
     */
    public static class HostManagerCompInit extends Init<HostManagerComp>{

        Comparator<LCPeerView> lcpComparator;
        private SystemConfig systemConfig;
        private ElectionConfig electionConfig;

        public HostManagerCompInit(SystemConfig systemConfig, ElectionConfig electionConfig, Comparator<LCPeerView> lcpComparator){
            
            this.systemConfig = systemConfig;
            this.electionConfig = electionConfig;
            this.lcpComparator = lcpComparator;

        }
    }




}

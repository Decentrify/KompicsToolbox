/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.p2ptoolbox.simulator;

import io.netty.channel.MessageSizeEstimator.Handle;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.net.msgs.DirectMsg;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Kompics;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.p2p.experiment.dsl.events.TerminateExperiment;
import se.sics.p2ptoolbox.simulator.cmd.NetworkOpCmd;
import se.sics.p2ptoolbox.simulator.cmd.OperationCmd;
import se.sics.p2ptoolbox.simulator.cmd.SimulationResult;
import se.sics.p2ptoolbox.simulator.cmd.StartNodeCmd;
import se.sics.p2ptoolbox.simulator.util.NodeIdFilter;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimulatorComponent extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(SimulatorComponent.class);

    private final SimulationContextImpl simulationContext;

    private Positive<ExperimentPort> experimentPort = requires(ExperimentPort.class);
    private Positive<VodNetwork> network = requires(VodNetwork.class);
    private Positive<Timer> timer = requires(Timer.class);

    private LinkedList<NetworkOpCmd> queuedOps;
    private NetworkOpCmd activeOp;
    private SimulationResult simResult;
    
    private Set<Component> startedComp;

    public SimulatorComponent(SimulatorInit init) {
        log.info("initiating...");
        this.simulationContext = new SimulationContextImpl(init.rand, init.simAddress);
        this.queuedOps = new LinkedList<NetworkOpCmd>();
        this.activeOp = null;
        this.simResult = null;
        this.startedComp = new HashSet<Component>();
        
        subscribe(handleStartNode, experimentPort);
        subscribe(handleNetworkOp, experimentPort);
        subscribe(handleNetworkOpResp, network);
        subscribe(handleSimulationResult, experimentPort);
        subscribe(handleTerminateExperiment, experimentPort);
    }

    private Handler<StartNodeCmd> handleStartNode = new Handler<StartNodeCmd>() {

        @Override
        public void handle(StartNodeCmd cmd) {
            log.info("received start cmd:{} for node:{}", cmd, cmd.getNodeId());

            Component node = create(cmd.getNodeComponentDefinition(), cmd.getNodeComponentInit());
            connect(node.getNegative(VodNetwork.class), network, new NodeIdFilter(cmd.getNodeId()));
            connect(node.getNegative(Timer.class), timer);

            simulationContext.registerNode(cmd.getNodeId());
            startedComp.add(node);
            
            trigger(Start.event, node.getControl());
        }

    };

    private Handler<NetworkOpCmd> handleNetworkOp = new Handler<NetworkOpCmd>() {

        @Override
        public void handle(NetworkOpCmd op) {
            log.info("received network op:{}", op);

            queuedOps.add(op);
            if(activeOp == null) {
                tryNewOp();
            }
        }
    };

    private void tryNewOp() {
        if (simulationContext.canContinue() && !queuedOps.isEmpty()) {
            activeOp = queuedOps.removeFirst();
            log.info("starting op:{}", activeOp);
            activeOp.beforeCmd(simulationContext);
            trigger(activeOp.getNetworkMsg(simulationContext.getSimulatorAddress()), network);
        }
    }

    private Handler<DirectMsg> handleNetworkOpResp = new Handler<DirectMsg>() {

        @Override
        public void handle(DirectMsg resp) {
            log.info("received network op:{} response", resp);

            if (activeOp == null) {
                log.error("unexpected response:{}", resp);
                throw new RuntimeException("posible logic exception - unexpected response");
            }
            try {
                activeOp.validate(simulationContext, resp);
            } catch (OperationCmd.ValidationException ex) {
                activeOp = null;
                simulationContext.fail(ex);
                return;
            }
            activeOp.afterValidation(simulationContext);
            log.info("op:{} successfull", activeOp);
            activeOp = null;
            tryNewOp();
        }
    };

    private Handler<SimulationResult> handleSimulationResult = new Handler<SimulationResult>() {

        @Override
        public void handle(SimulationResult event) {
            log.info("received simulation result request");
            simResult = event;
        }

    };

    private void deliverResults() {
        if (simResult == null) {
            log.error("no simulation result delivery");
            throw new RuntimeException("no SimulationResult event in scenario");
        }
        simResult.setSimulationResult(simulationContext.getSimulationResult());
    }

    private Handler<TerminateExperiment> handleTerminateExperiment = new Handler<TerminateExperiment>() {

        @Override
        public void handle(TerminateExperiment event) {
            log.info("terminating simulation...");
            if (simulationContext.canContinue() && activeOp == null) {
                log.error("simulation ongoing...");
                throw new RuntimeException("simulation ongoing - tried to terminate experiment experiment too soon");
            }
            deliverResults();
            for(Component comp : startedComp) {
                trigger(Stop.event, comp.control());
            }
            Kompics.shutdown();
        }

    };
    
//    Positive<Experiment> simulator = requires(Experiment.class);
//    Positive<Network> net = requires(Network.class);
//    Positive<Timer> timer = requires(Timer.class);
//
//    private final Random rand;
//    private TreeSet<Integer> portsInUse = new TreeSet<Integer>();
//    private InetAddress localIP;
//    private Configuration baseConfig;
//    private Address receiver;
//    private Address target;
//
//    Positive<ExperimentPort> expExecutor = requires(ExperimentPort.class);
//    public SimulatorComponent(SimulatorComponentInit init) {
//
//        try {
//            localIP = InetAddress.getLocalHost();
//        } catch (UnknownHostException ex) {
//            throw new RuntimeException(ex.getMessage());
//        }
//
//        receiver = new Address(localIP, getFreePort(), null);
//        TimestampIdFactory.init(receiver);
//
//        // Subscriptions
//        //system nodes simulation
//        subscribe(nodeBootHandler, simulator);
//        subscribe(killSystemHandler, simulator);
//        //experiments
//        subscribe(experimentOpHandler, simulator);
//        subscribe(validateHandler, simulator);
//        subscribe(terminateHandler, simulator);
//        subscribe(experimentEndedHandler, expExecutor);
//        subscribe(caracalOpHandler, expExecutor);
//
//        Component experimentExecutor;
//        if (SimulationHelper.type.equals(SimulationHelper.ExpType.NO_RESULT)) {
//            experimentExecutor = create(Experiment1.class, new Experiment1Init());
//        } else if (SimulationHelper.type.equals(SimulationHelper.ExpType.WITH_RESULT)) {
//            experimentExecutor = create(Experiment2.class, new Experiment2Init());
//        } else {
//            LOG.error("unknown experiment");
//            System.exit(1);
//            return;
//        }
//        connect(experimentExecutor.getNegative(Network.class), net, new MessageDestinationFilter(new HostAddress(receiver)));
//        connect(expExecutor.getPair(), experimentExecutor.getPositive(ExperimentPort.class));
//    }
//
//
//    //*****************
//    //system simulation
//    //*****************
//    Handler<BootCmd> nodeBootHandler = new Handler<BootCmd>() {
//        @Override
//        public void handle(BootCmd event) {
//            LOG.info("Booting up {} nodes.", event.nodeCount);
//            if (event.nodeCount < 3) {
//                throw new RuntimeException("FATAL: Need to start at least 3 hosts!");
//            }
//            int n = event.nodeCount - 1;
//
//            int bootstrapPort = getFreePort();
//            baseConfig = Configuration.Factory.modify(baseConfig)
//                    .setBootstrapServer(new Address(localIP, bootstrapPort, null))
//                    .finalise();
//            bootBootstrapNode();
//
//            for (int i = 0; i < n; i++) {
//                int port = getFreePort();
//                bootNode(port);
//            }
//
//        }
//    };
//
//    private void bootBootstrapNode() {
//        Configuration myConf = Configuration.Factory.modifyWithOtherDB(baseConfig, "bootStrap/")
//                .setIp(baseConfig.getBootstrapServer().getIp())
//                .setPort(baseConfig.getBootstrapServer().getPort())
//                .finalise();
//
//        Address netSelf = new Address(myConf.getIp(), myConf.getPort(), null);
//
//        VirtualNetworkChannel vnc = VirtualNetworkChannel.connect(net,
//                new MessageDestinationFilter(new HostAddress(netSelf)));
//        Component manager = create(HostManager.class, new HostManagerInit(myConf, netSelf, vnc));
//
//        connect(manager.getNegative(Timer.class), timer);
//
//        trigger(Start.event, manager.control());
//        target = netSelf;
//    }
//
//    private void bootNode(int port) {
//        Configuration myConf = Configuration.Factory.modifyWithOtherDB(baseConfig, String.valueOf(port) + "/")
//                .setIp(baseConfig.getBootstrapServer().getIp())
//                .setPort(port)
//                .finalise();
//
//        Address netSelf = new Address(myConf.getIp(), myConf.getPort(), null);
//
//        VirtualNetworkChannel vnc = VirtualNetworkChannel.connect(net,
//                new MessageDestinationFilter(new HostAddress(netSelf)));
//        Component manager = create(HostManager.class, new HostManagerInit(myConf, netSelf, vnc));
//
//        connect(manager.getNegative(Timer.class), timer);
//
//        trigger(Start.event, manager.control());
//    }
//
//    Handler<TerminateExperiment> killSystemHandler = new Handler<TerminateExperiment>() {
//        @Override
//        public void handle(TerminateExperiment event) {
//            LOG.info("kill system, terminate experiment.");
//            Kompics.forceShutdown();
//        }
//    };
//
//    Handler<TerminateCmd> terminateHandler = new Handler<TerminateCmd>() {
//        @Override
//        public void handle(TerminateCmd event) {
//            LOG.info("Got termination command.");
//            trigger(new TerminateExperiment(), simulator);
//        }
//    };
//
//    //***************************
//    //experiment forward handlers
//    //***************************
//    Handler<TerminateExperiment> experimentEndedHandler = new Handler<TerminateExperiment>() {
//
//        @Override
//        public void handle(TerminateExperiment event) {
//            LOG.info("experiment ended");
//            trigger(event, simulator);
//        }
//    };
//
//    Handler<OpCmd> experimentOpHandler = new Handler<OpCmd>() {
//
//        @Override
//        public void handle(OpCmd op) {
//            trigger(op, expExecutor);
//        }
//    };
//
//    Handler<CaracalOp> caracalOpHandler = new Handler<CaracalOp>() {
//
//        @Override
//        public void handle(CaracalOp op) {
//            //should get rid of this
//            Key key;
//            if (op instanceof PutRequest) {
//                key = ((PutRequest) op).key;
//            } else if (op instanceof RangeQuery.Request) {
//                key = ((RangeQuery.Request) op).initRange.begin;
//            } else if (op instanceof GetRequest) {
//                key = ((GetRequest) op).key;
//            } else {
//                LOG.error("unexpected op - logic error");
//                System.exit(1);
//                return;
//            }
//
//            CaracalMsg msg = new CaracalMsg(receiver, target, op);
//            ForwardMessage fMsg = new ForwardMessage(receiver, target, key, msg);
//            trigger(fMsg, net);
//        }
//    };
//
//    Handler<ValidateCmd> validateHandler = new Handler<ValidateCmd>() {
//
//        @Override
//        public void handle(ValidateCmd event) {
//            LOG.info("Got validation command.");
//            trigger(event, expExecutor);
//        }
//    };
    public static class SimulatorInit extends Init<SimulatorComponent> {

        public final Random rand;
        public final VodAddress simAddress;

        public SimulatorInit(Random rand, VodAddress simAddress) {
            this.rand = rand;
            this.simAddress = simAddress;
        }
    }

//    private static final Logger log = LoggerFactory.getLogger(SimulatorComponent.class);
//
//    private Positive<VodNetwork> network = requires(VodNetwork.class);
//    private Positive<Timer> timer = requires(Timer.class);
//    private Positive<ExperimentPort> experiment = requires(ExperimentPort.class);
//
//    private final SimulationContextImpl context;
//
//    public SimulatorComponent(SimManagerInit init) {
//        log.info("initiating...");
//        context = new SimulationContextImpl();
//
//        subscribe(handleStartPeer, experiment);
//        subscribe(handleLocalOp, experiment);
//        subscribe(handleNetworkOp, experiment);
//        subscribe(handleTerminate, experiment);
//    }
//
//    Handler<StartPeerCmd> handleStartPeer = new Handler<StartPeerCmd>() {
//
//        @Override
//        public void handle(StartPeerCmd cmd) {
//            log.info("starting peer {}", cmd.nodeId);
//            if(!context.registerNode(cmd.nodeId)) {
//                throw new RuntimeException("could not register node:" + cmd.nodeId + " possible clash of ids");
//            }
//            Component peer = create(cmd.peerClass, cmd.init);
//            connect(peer.getNegative(VodNetwork.class), network, new NodeIdFilter(cmd.nodeId));
//            connect(peer.getNegative(Timer.class), timer);
//
//            Iterator<Map.Entry<Class<? extends PortType>, Boolean>> it = cmd.peerPorts.entrySet().iterator();
//            while(it.hasNext()) {
//                Map.Entry<Class<? extends PortType>, Boolean> portType = it.next();
//                Port port = null;
//                if(portType.getValue()) {
//                    port = peer.getPositive(portType.getKey());
//                } else {
//                    port = peer.getNegative(portType.getKey());
//                }
//                context.registerPort(cmd.nodeId, portType.getKey(), port);
//            }
//            trigger(Start.event, peer.control());
//        }
//    };
//    
//    Handler<NetworkOpCmd> handlerNetworkOp = new Handler<NetworkOpCmd>() {
//
//        @Override
//        public void handle(NetworkOpCmd cmd) {
//            log.info("network cmd:{}", cmd);
//            
//            
//        }
//        
//    };
//            
//    Handler<LocalOpCmd> handleLocalOp = new Handler<LocalOpCmd>() {
//
//        @Override
//        public void handle(LocalOpCmd cmd) {
//            log.error("local commands not yet supported");
//            throw new UnsupportedOperationException();
////            log.info("received local cmd {} for peer {}", cmd.localCmd, cmd.peerId);
////            if (!systemComp.containsKey(cmd.peerId)) {
////                log.error("there is no peer {} - dropping cmd {}", cmd.peerId, cmd.localCmd);
////                return;
////            }
////            Positive peerPort = systemComp.get(cmd.peerId).getValue1();
////            trigger(cmd.localCmd, peerPort);
//        }
//    };
//
//    Handler<TerminateExperiment> handleTerminate = new Handler<TerminateExperiment>() {
//        @Override
//        public void handle(TerminateExperiment event) {
//            log.info("terminate experiment.");
//            Kompics.forceShutdown();
//        }
//    };
//
//    public static class SimManagerInit extends Init<SimulatorComponent> {
//    }
}

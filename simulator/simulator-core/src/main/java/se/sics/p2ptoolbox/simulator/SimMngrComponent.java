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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Kill;
import se.sics.kompics.Kompics;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.simulator.cmd.impl.ReStartNodeCmd;
import se.sics.p2ptoolbox.simulator.cmd.impl.StartAggregatorCmd;
import se.sics.p2ptoolbox.simulator.dsl.events.TerminateExperiment;
import se.sics.p2ptoolbox.simulator.cmd.impl.StartNodeCmd;
import se.sics.p2ptoolbox.simulator.cmd.impl.KillNodeCmd;
import se.sics.p2ptoolbox.simulator.cmd.impl.SetupCmd;
import se.sics.p2ptoolbox.simulator.cmd.util.ConnectSimulatorPort;
import se.sics.p2ptoolbox.util.filters.IntegerIdentifiableFilter;
import se.sics.p2ptoolbox.util.identifiable.IntegerIdentifiable;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimMngrComponent extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(SimMngrComponent.class);

    private final SimulationContextImpl simulationContext;

    private Positive<ExperimentPort> experimentPort = requires(ExperimentPort.class);
    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);

    private Map<Integer, Component> systemNodes;
    private Component aggregator;
    private Component simulationClient;

    public SimMngrComponent(SimMngrInit init) {
        log.info("initiating...");
        this.simulationContext = new SimulationContextImpl(init.rand, init.simAddress);
        this.systemNodes = new HashMap<Integer, Component>();

        subscribe(handleStart, control);
        subscribe(handleStop, control);

        subscribe(handleSetup, experimentPort);
        subscribe(handleStartAggregator, experimentPort);
        subscribe(handleStartNode, experimentPort);
        subscribe(handleStopNode, experimentPort);
        //stop-restart does not work in kompics now
//        subscribe(handleReStartNode, experimentPort); 
        for (final SystemStatusHandler systemStatusHandler : init.systemStatusHandlers) {
            Class<? extends Msg> msgType = systemStatusHandler.getStatusMsgType();
            Handler handler = new Handler(msgType) {

                @Override
                public void handle(KompicsEvent event) {
                    systemStatusHandler.handle(event, simulationContext);
                }
            };
            subscribe(handler, network);
        }
        subscribe(handleTerminateExperiment, experimentPort);
    }

    //**********CONTROL HANDLERS************************************************
    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.info("starting...");

            simulationClient = create(SimClientComponent.class, new SimClientComponent.SimClientInit(simulationContext));
            connect(simulationClient.getNegative(Network.class), network, new IntegerIdentifiableFilter(((IntegerIdentifiable) simulationContext.getSimulatorAddress()).getId()));
            connect(simulationClient.getNegative(Timer.class), timer);
            connect(simulationClient.getNegative(ExperimentPort.class), experimentPort);

            trigger(Start.event, simulationClient.control());
        }

    };

    private Handler<Stop> handleStop = new Handler<Stop>() {

        @Override
        public void handle(Stop event) {
            log.info("stopping...");
        }

    };

    //**************************************************************************
    private Handler handleSetup = new Handler<SetupCmd>() {
        @Override
        public void handle(SetupCmd cmd) {
            log.info("received setup cm:{}", cmd);
            cmd.runSetup();
        }
    };
    
    private Handler<StartAggregatorCmd> handleStartAggregator = new Handler<StartAggregatorCmd>() {

        @Override
        public void handle(StartAggregatorCmd cmd) {
            log.info("received start aggregator cmd:{}", cmd);

            aggregator = create(cmd.getNodeComponentDefinition(), cmd.getNodeComponentInit());
            connect(aggregator.getNegative(Timer.class), timer);
            Address aggregatorAdr = cmd.getAddress();
            if (aggregatorAdr instanceof IntegerIdentifiable) {
                Integer aggregatorId = ((IntegerIdentifiable) aggregatorAdr).getId();
                connect(aggregator.getNegative(Network.class), network, new IntegerIdentifiableFilter(aggregatorId));
            } else {
                log.error("aggregator address is wrong - not identifiable");
                throw new RuntimeException("aggregator address is wrong - not identifiable");
            }
            if(cmd instanceof ConnectSimulatorPort) {
                connect(aggregator.getNegative(ExperimentPort.class), experimentPort);
            }
            simulationContext.registerAggregator(aggregatorAdr);
            trigger(Start.event, aggregator.control());
        }
    };

    private Handler<StartNodeCmd> handleStartNode = new Handler<StartNodeCmd>() {

        @Override
        public void handle(StartNodeCmd cmd) {
            log.info("received start cmd:{} for node:{}", cmd, cmd.getNodeId());

            Component node = create(cmd.getNodeComponentDefinition(), cmd.getNodeComponentInit(simulationContext.getAggregatorAddress(), simulationContext.systemOpenNodesSample(cmd.bootstrapSize(), cmd.getAddress())));
            connect(node.getNegative(Timer.class), timer);

            Component natEmulator = null;
            Negative<Network> nodeNetwork = null;
            if (cmd.getAddress() instanceof DecoratedAddress) {
                DecoratedAddress dAdr = (DecoratedAddress) cmd.getAddress();
                nodeNetwork = node.getNegative(Network.class);
            } else {
                nodeNetwork = node.getNegative(Network.class);
            }
            connect(nodeNetwork, network, new IntegerIdentifiableFilter(cmd.getNodeId()));

            //TODO maybe this should be one method
            simulationContext.registerNode(cmd.getNodeId());
            simulationContext.bootNode(cmd.getNodeId(), cmd.getAddress());
            systemNodes.put(cmd.getNodeId(), node);
            //********************************

            if (cmd.getAddress() instanceof DecoratedAddress) {
                DecoratedAddress dAdr = (DecoratedAddress) cmd.getAddress();
            }
            trigger(Start.event, node.control());
        }
    };

    private Handler<KillNodeCmd> handleStopNode = new Handler<KillNodeCmd>() {

        @Override
        public void handle(KillNodeCmd cmd) {
            log.info("received kill cmd:{} for node:{}", cmd, cmd.getNodeId());

            Component node = systemNodes.remove(cmd.getNodeId());
            if (node == null) {
                throw new RuntimeException("node does not exist");
            }
            simulationContext.killNode(cmd.getNodeId());
            disconnect(node.getNegative(Network.class), network);
            disconnect(node.getNegative(Timer.class), timer);
            trigger(Kill.event, node.control());
            if (node != null) {
                trigger(Kill.event, node.control());
            }
        }
    };

    private Handler<ReStartNodeCmd> handleReStartNode = new Handler<ReStartNodeCmd>() {

        @Override
        public void handle(ReStartNodeCmd cmd) {
            log.info("received re-start cmd:{} for node:{}", cmd, cmd.getNodeId());
            if (!systemNodes.containsKey(cmd.getNodeId())) {
                throw new RuntimeException("node does not exist");
            }
            Component node = systemNodes.get(cmd.getNodeId());
            connect(node.getNegative(Network.class), network, new IntegerIdentifiableFilter(cmd.getNodeId()));
            connect(node.getNegative(Timer.class), timer);
            trigger(Start.event, node.control());
        }
    };

    private Handler<TerminateExperiment> handleTerminateExperiment = new Handler<TerminateExperiment>() {

        @Override
        public void handle(TerminateExperiment event) {
            log.info("terminating simulation...");
            Kompics.forceShutdown();

        }
    };

    public static class SimMngrInit extends Init<SimMngrComponent> {

        public final Random rand;
        public final Address simAddress;
        public final Set<SystemStatusHandler> systemStatusHandlers;

        public SimMngrInit(Random rand, Address simAddress, Set<SystemStatusHandler> systemStatusHandlers) {
            this.rand = rand;
            this.simAddress = simAddress;
            this.systemStatusHandlers = systemStatusHandlers;
        }
    }
}

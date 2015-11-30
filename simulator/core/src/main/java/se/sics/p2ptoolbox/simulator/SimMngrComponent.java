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

import se.sics.ktoolbox.simulator.SimulatorPort;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Kill;
import se.sics.kompics.Kompics;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.simulator.SimulatorComponent;
import se.sics.ktoolbox.simulator.SimulatorControlPort;
import se.sics.ktoolbox.simulator.events.SetupEvent;
import se.sics.ktoolbox.simulator.events.TerminateExperiment;
import se.sics.ktoolbox.simulator.events.system.KillNodeEvent;
import se.sics.ktoolbox.simulator.events.system.StartAggregatorEvent;
import se.sics.ktoolbox.simulator.events.system.StartNodeEvent;
import se.sics.ktoolbox.util.selectors.DestinationHostSelector;
import se.sics.p2ptoolbox.util.identifiable.IntegerIdentifiable;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimMngrComponent extends ComponentDefinition implements SimulatorComponent {

    private static final Logger log = LoggerFactory.getLogger(SimMngrComponent.class);
    private String logPrefix = "";

    private final Positive simPort = requires(SimulatorPort.class);
    private final Positive simControlPort = requires(SimulatorControlPort.class);
    private final Positive network = requires(Network.class);
    private final Positive timer = requires(Timer.class);

    private final SimulationContextImpl simulationContext;

    private Component aggregator;
    private Map<Integer, Component> systemNodes = new HashMap<>();

    public SimMngrComponent(SimMngrInit init) {
        log.info("{}initiating...", logPrefix);
        this.simulationContext = new SimulationContextImpl(init.rand);

        subscribe(handleStart, control);
        subscribe(handleSetup, simPort);
        subscribe(handleStartAggregator, simPort);
        subscribe(handleStartNode, simPort);
        subscribe(handleKillNode, simPort);
        subscribe(handleTerminateExperiment, simControlPort);

        //subscribe custom handlers for specific network events
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
    }

    //**********CONTROL HANDLERS************************************************
    private final Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            log.info("{}starting...", logPrefix);
        }
    };
    //************************TESTING_HANDLERS**********************************
    private final Handler handleNet = new Handler<Msg>() {
        @Override
        public void handle(Msg msg) {
            log.info("{}net msg:{}", logPrefix, msg);
        }
    };
    //**************************************************************************
    private final Handler handleSetup = new Handler<SetupEvent>() {
        @Override
        public void handle(SetupEvent setup) {
            log.info("{}received setup cm:{}", logPrefix, setup);
            setup.runSetup();
        }
    };

    private final Handler handleStartAggregator = new Handler<StartAggregatorEvent>() {

        @Override
        public void handle(StartAggregatorEvent startAggregator) {
            log.info("{}received start aggregator cmd:{}", logPrefix, startAggregator);

            aggregator = create(startAggregator.getComponentDefinition(), startAggregator.getComponentInit());
            connect(aggregator.getNegative(Timer.class), timer, Channel.TWO_WAY);
            Address aggregatorAdr = startAggregator.getAddress();
            if (aggregatorAdr instanceof IntegerIdentifiable) {
                Integer aggregatorId = ((IntegerIdentifiable) aggregatorAdr).getId();
                connect(aggregator.getNegative(Network.class), network, new DestinationHostSelector(aggregatorId, true), Channel.TWO_WAY);
            } else {
                log.error("{}aggregator address is wrong - not identifiable", logPrefix);
                throw new RuntimeException("aggregator address is wrong - not identifiable");
            }
            connect(aggregator.getNegative(SimulatorPort.class), simPort, Channel.TWO_WAY);
            simulationContext.registerAggregator(aggregatorAdr);
            trigger(Start.event, aggregator.control());
        }
    };

    private final Handler handleStartNode = new Handler<StartNodeEvent>() {

        @Override
        public void handle(StartNodeEvent startNode) {
            log.info("{}received start:{} for node:{}", new Object[]{logPrefix, startNode,
                startNode.getNodeId()});

            Component node = create(startNode.getComponentDefinition(), startNode.getComponentInit());
            connect(node.getNegative(Timer.class), timer, Channel.TWO_WAY);
            connect(node.getNegative(Network.class), network, 
                    new DestinationHostSelector(startNode.getNodeId(), true), Channel.TWO_WAY);

            simulationContext.startNode(startNode.getNodeId(), startNode.getAddress());
            systemNodes.put(startNode.getNodeId(), node);

            trigger(Start.event, node.control());
        }
    };

    private Handler handleKillNode = new Handler<KillNodeEvent>() {

        @Override
        public void handle(KillNodeEvent killNode) {
            log.info("{}received kill cmd:{} for node:{}", killNode, killNode.getNodeId());
            Component node = systemNodes.remove(killNode.getNodeId());
            if (node == null) {
                throw new RuntimeException("node does not exist");
            }
            simulationContext.killNode(killNode.getNodeId());
            disconnect(node.getNegative(Network.class), network);
            disconnect(node.getNegative(Timer.class), timer);
            trigger(Kill.event, node.control());
        }
    };

    private final Handler handleTerminateExperiment = new Handler<TerminateExperiment>() {

        @Override
        public void handle(TerminateExperiment event) {
            log.info("{}terminating simulation...", logPrefix);
            Kompics.forceShutdown();
        }
    };

    public static class SimMngrInit extends Init<SimMngrComponent> {

        public final Random rand;
        public final Set<SystemStatusHandler> systemStatusHandlers;

        public SimMngrInit(Random rand, Set<SystemStatusHandler> systemStatusHandlers) {
            this.rand = rand;
            this.systemStatusHandlers = systemStatusHandlers;
        }
    }
}

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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Kompics;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.Stopped;
import se.sics.kompics.p2p.experiment.dsl.events.TerminateExperiment;
import se.sics.p2ptoolbox.simulator.cmd.StartNodeCmd;
import se.sics.p2ptoolbox.simulator.util.NodeIdFilter;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimMngrComponent extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(SimMngrComponent.class);

    private final SimulationContextImpl simulationContext;

    private Positive<ExperimentPort> experimentPort = requires(ExperimentPort.class);
    private Positive<VodNetwork> network = requires(VodNetwork.class);
    private Positive<Timer> timer = requires(Timer.class);

    private Set<Component> startedComp;
    private int stoppedCounter;

    public SimMngrComponent(SimMngrInit init) {
        log.info("initiating...");
        this.simulationContext = new SimulationContextImpl(init.rand, init.simAddress);
        this.startedComp = new HashSet<Component>();

        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleStopped, control);

        subscribe(handleStartNode, experimentPort);
        subscribe(handleTerminateExperiment, experimentPort);
    }

    //**********CONTROL HANDLERS************************************************
    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.info("starting...");

            Component simClient = create(SimClientComponent.class, new SimClientComponent.SimClientInit(simulationContext));
            connect(simClient.getNegative(VodNetwork.class), network, new NodeIdFilter(simulationContext.getSimulatorAddress().getId()));
            connect(simClient.getNegative(Timer.class), timer);
            connect(simClient.getNegative(ExperimentPort.class), experimentPort);
            startedComp.add(simClient);

            subscribe(handleFault, simClient.control());
            trigger(Start.event, simClient.control());
        }

    };

    private Handler<Stop> handleStop = new Handler<Stop>() {

        @Override
        public void handle(Stop event) {
            log.info("stopping...");
        }

    };

    private Handler<Fault> handleFault = new Handler<Fault>() {

        @Override
        public void handle(Fault event) {
            log.info("exception:{} msg:{}", event.getFault(), event.getFault().getMessage());
            if (event.getFault() instanceof RuntimeException) {
                throw (RuntimeException) event.getFault();
            } else {
                throw new RuntimeException(event.getFault());
            }
        }

    };

    private Handler<Stopped> handleStopped = new Handler<Stopped>() {

        @Override
        public void handle(Stopped event) {
            stoppedCounter--;
            log.info("child stopped, remaining unstopped children:{}", stoppedCounter);
            if(stoppedCounter == 0) {
                Kompics.shutdown();
            }
        }

    };

    //**************************************************************************
    private Handler<StartNodeCmd> handleStartNode = new Handler<StartNodeCmd>() {

        @Override
        public void handle(StartNodeCmd cmd) {
            log.info("received start cmd:{} for node:{}", cmd, cmd.getNodeId());

            Component node = create(cmd.getNodeComponentDefinition(), cmd.getNodeComponentInit());
            connect(node.getNegative(VodNetwork.class), network, new NodeIdFilter(cmd.getNodeId()));
            connect(node.getNegative(Timer.class), timer);

            simulationContext.registerNode(cmd.getNodeId());
            startedComp.add(node);

            subscribe(handleFault, node.control());
            trigger(Start.event, node.control());
        }

    };

    private Handler<TerminateExperiment> handleTerminateExperiment = new Handler<TerminateExperiment>() {

        @Override
        public void handle(TerminateExperiment event) {
            log.info("terminating simulation(stopping children)...");
            
            stoppedCounter = startedComp.size();
            for (Component comp : startedComp) {
                trigger(Stop.event, comp.control());
            }
        }
    };

    public static class SimMngrInit extends Init<SimMngrComponent> {

        public final Random rand;
        public final VodAddress simAddress;

        public SimMngrInit(Random rand, VodAddress simAddress) {
            this.rand = rand;
            this.simAddress = simAddress;
        }
    }
}

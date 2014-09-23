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
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.net.VodNetwork;
import se.sics.p2ptoolbox.simulator.cmd.StartPeerCmd;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Kompics;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.p2p.experiment.dsl.events.TerminateExperiment;
import se.sics.p2ptoolbox.simulator.cmd.LocalCmd;
import se.sics.p2ptoolbox.simulator.util.NodeIdFilter;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimManagerComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(SimManagerComp.class);

    private Positive<VodNetwork> network = requires(VodNetwork.class);
    private Positive<Timer> timer = requires(Timer.class);
    private Positive<ExperimentPort> experiment = requires(ExperimentPort.class);

    private HashMap<Integer, Pair<Component, Positive>> systemComp;

    public SimManagerComp(SimManagerInit init) {
        log.debug("init");
        systemComp = new HashMap<Integer, Pair<Component, Positive>>();

        subscribe(handleStartPeer, experiment);
        subscribe(handleLocalCmd, experiment);
        subscribe(handleTerminate, experiment);
    }

    Handler<StartPeerCmd> handleStartPeer = new Handler<StartPeerCmd>() {

        @Override
        public void handle(StartPeerCmd cmd) {
            log.info("starting peer {}", cmd.id);
            Component peer = create(cmd.peerClass, cmd.init);
            connect(peer.getNegative(VodNetwork.class), network, new NodeIdFilter(cmd.id));
            connect(peer.getNegative(Timer.class), timer);

            systemComp.put(cmd.id, Pair.with(peer, peer.getPositive(cmd.peerPort)));
            trigger(Start.event, peer.control());
        }
    };

    Handler<LocalCmd> handleLocalCmd = new Handler<LocalCmd>() {

        @Override
        public void handle(LocalCmd cmd) {
            log.info("received local cmd {} for peer {}", cmd.localCmd, cmd.peerId);
            if (!systemComp.containsKey(cmd.peerId)) {
                log.error("there is no peer {} - dropping cmd {}", cmd.peerId, cmd.localCmd);
                return;
            }
            Positive peerPort = systemComp.get(cmd.peerId).getValue1();
            trigger(cmd.localCmd, peerPort);
        }
    };

    Handler<TerminateExperiment> handleTerminate = new Handler<TerminateExperiment>() {
        @Override
        public void handle(TerminateExperiment event) {
            log.info("terminate experiment.");
            Kompics.forceShutdown();
        }
    };

    public static class SimManagerInit extends Init<SimManagerComp> {
    }
}

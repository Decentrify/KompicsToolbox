/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Croupier is free software; you can redistribute it and/or
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
package se.sics.p2ptoolbox.croupier.example.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.croupier.CroupierComp;
import se.sics.p2ptoolbox.croupier.CroupierConfig;
import se.sics.p2ptoolbox.croupier.CroupierControlPort;
import se.sics.p2ptoolbox.croupier.CroupierPort;
import se.sics.p2ptoolbox.croupier.CroupierSelectionPolicy;
import se.sics.p2ptoolbox.croupier.msg.CroupierDisconnected;
import se.sics.p2ptoolbox.util.filters.IntegerOverlayFilter;
import se.sics.p2ptoolbox.util.network.NatedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class HostComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(HostComp.class);

    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);

    private final NatedAddress selfAddress;
    private final List<NatedAddress> bootstrapNodes;
    private final long seed;

    public HostComp(HostInit init) {
        this.selfAddress = init.selfAddress;
        log.info("{} initiating...", selfAddress);
        this.bootstrapNodes = init.bootstrapNodes;
        log.debug("{} bootstrap sample:{}", selfAddress, bootstrapNodes);
        this.seed = init.seed;

        int shufflePeriodCroupier1 = 1000;
        CroupierConfig croupierConfig1 = new CroupierConfig(10, shufflePeriodCroupier1, 5, CroupierSelectionPolicy.RANDOM, 1);
        Component croupier1 = createNConnectCroupier(croupierConfig1, 10);
        Component compA = createNConnectCompA(croupier1);

        subscribe(handleDisconnected, croupier1.getPositive(CroupierControlPort.class));
        subscribe(handleStart, control);
        subscribe(handleStop, control);
    }
    
    private Component createNConnectCroupier(CroupierConfig config, int overlayId) {
        Component croupier = create(CroupierComp.class, new CroupierComp.CroupierInit(config, overlayId, selfAddress, bootstrapNodes, seed));
        connect(croupier.getNegative(Network.class), network, new IntegerOverlayFilter(overlayId));
        connect(croupier.getNegative(Timer.class), timer);
        return croupier;
    }

    private Component createNConnectCompA(Component croupier) {
        Component compA = create(ExampleComponentA.class, new ExampleComponentA.ExampleInitA(selfAddress, seed));
        connect(croupier.getPositive(CroupierPort.class), compA.getNegative(CroupierPort.class));
        return compA;
    }

    private Handler<Start> handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            log.info("{} starting...", selfAddress);
        }
    };

    private Handler<Stop> handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            log.info("{} stopping...", selfAddress);
        }
    };

    private Handler<CroupierDisconnected> handleDisconnected = new Handler<CroupierDisconnected>() {
        @Override
        public void handle(CroupierDisconnected event) {
            log.info("{} croupier:{} disconnected", selfAddress, event.overlayId);
        }
    };

    public static class HostInit extends Init<HostComp> {
        public final NatedAddress selfAddress;
        public final List<NatedAddress> bootstrapNodes;
        public final long seed;
        public final NatedAddress aggregatorAddress;

        public HostInit(NatedAddress self, Set<NatedAddress> bootstrapNodes, long seed, NatedAddress aggregatorAddress) {
            this.seed = seed;
            this.selfAddress = self;
            this.bootstrapNodes = new ArrayList<NatedAddress>(bootstrapNodes);
            this.aggregatorAddress = aggregatorAddress;
        }
    }
}

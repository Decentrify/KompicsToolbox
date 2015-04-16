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
package se.sics.p2ptoolbox.gradient.idsort.system;

import com.typesafe.config.ConfigFactory;
import java.util.List;
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
import se.sics.p2ptoolbox.croupier.CroupierControlPort;
import se.sics.p2ptoolbox.croupier.CroupierPort;
import se.sics.p2ptoolbox.croupier.CroupierSelectionPolicy;
import se.sics.p2ptoolbox.croupier.msg.CroupierDisconnected;
import se.sics.p2ptoolbox.croupier.CroupierComp;
import se.sics.p2ptoolbox.croupier.CroupierConfig;
import se.sics.p2ptoolbox.gradient.GradientPort;
import se.sics.p2ptoolbox.gradient.GradientComp;
import se.sics.p2ptoolbox.gradient.GradientConfig;
import se.sics.p2ptoolbox.gradient.simulation.NoFilter;
import se.sics.p2ptoolbox.gradient.idsort.IdSortComp;
import se.sics.p2ptoolbox.gradient.idsort.IdViewComparator;
import se.sics.p2ptoolbox.util.filters.IntegerOverlayFilter;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class IdSortHostComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(IdSortHostComp.class);

    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);

    private Positive<CroupierControlPort> croupierControlPort1;

    private DecoratedAddress selfAddress;

    public IdSortHostComp(HostInit init) {
        this.selfAddress = init.selfAddress;
        log.info("{} initiating...", selfAddress);
        log.debug("{} bootstrap sample:{}", selfAddress, init.bootstrapNodes);

        subscribe(handleStart, control);
        subscribe(handleStop, control);

        int viewSize = 10;
        int shuffleSize = 5;
        int croupierOverlayId = 10;
        int gradientOverlayId = 11;

        CroupierConfig croupierConfig = new CroupierConfig(ConfigFactory.load("application.conf"));
        CroupierComp.CroupierInit croupierInit1 = new CroupierComp.CroupierInit(croupierConfig, croupierOverlayId, selfAddress, init.bootstrapNodes, init.seed);
        Component croupier1 = createNConnectCroupier(croupierInit1);

        GradientConfig gradientConfig1 = new GradientConfig(viewSize, init.period, shuffleSize, init.softMaxTemperature);
        GradientComp.GradientInit gradientInit1 = new GradientComp.GradientInit(selfAddress, gradientConfig1, gradientOverlayId, new IdViewComparator(), new NoFilter(), init.seed);
        Component gradient1 = createNConnectGradient(gradientInit1, croupier1);

        IdSortComp.IdSortInit exampleInit1 = new IdSortComp.IdSortInit(selfAddress);
        Component compA = createNConnectIdSort(exampleInit1, croupier1, gradient1);

        subscribe(handleDisconnected, croupier1.getPositive(CroupierControlPort.class));
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

    private Component createNConnectCroupier(CroupierComp.CroupierInit croupierInit) {
        Component croupier = create(CroupierComp.class, croupierInit);
        connect(croupier.getNegative(Network.class), network, new IntegerOverlayFilter(croupierInit.overlayId));
        connect(croupier.getNegative(Timer.class), timer);
        return croupier;
    }

    private Component createNConnectGradient(GradientComp.GradientInit gradientInit, Component croupier) {
        Component gradient = create(GradientComp.class, gradientInit);
        connect(gradient.getNegative(Network.class), network, new IntegerOverlayFilter(gradientInit.overlayId));
        connect(gradient.getNegative(Timer.class), timer);
        connect(gradient.getNegative(CroupierPort.class), croupier.getPositive(CroupierPort.class));
        return gradient;
    }

    private Component createNConnectIdSort(IdSortComp.IdSortInit exampleInit, Component croupier, Component gradient) {
        Component example = create(IdSortComp.class, exampleInit);
        connect(example.getNegative(CroupierPort.class), croupier.getPositive(CroupierPort.class));
        connect(example.getNegative(GradientPort.class), gradient.getPositive(GradientPort.class));
        return example;
    }

    private Handler<CroupierDisconnected> handleDisconnected = new Handler<CroupierDisconnected>() {

        @Override
        public void handle(CroupierDisconnected event) {
            log.info("{} croupier:{} disconnected", selfAddress, event.overlayId);
        }

    };

    public static class HostInit extends Init<IdSortHostComp> {

        public final long seed;
        public final DecoratedAddress selfAddress;
        public final List<DecoratedAddress> bootstrapNodes;
        public final int period;
        public final double softMaxTemperature;

        public HostInit(DecoratedAddress selfAddress, List<DecoratedAddress> bootstrapNodes, long seed, int period, double softMaxTemperature) {
            this.seed = seed;
            this.selfAddress = selfAddress;
            this.bootstrapNodes = bootstrapNodes;
            this.period = period;
            this.softMaxTemperature = softMaxTemperature;
        }
    }
}

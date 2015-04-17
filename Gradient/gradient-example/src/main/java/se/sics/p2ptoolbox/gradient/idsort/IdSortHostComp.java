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
package se.sics.p2ptoolbox.gradient.idsort;

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
import se.sics.p2ptoolbox.croupier.msg.CroupierDisconnected;
import se.sics.p2ptoolbox.croupier.CroupierComp;
import se.sics.p2ptoolbox.croupier.CroupierConfig;
import se.sics.p2ptoolbox.gradient.GradientPort;
import se.sics.p2ptoolbox.gradient.GradientComp;
import se.sics.p2ptoolbox.gradient.GradientConfig;
import se.sics.p2ptoolbox.gradient.simulation.NoFilter;
import se.sics.p2ptoolbox.gradient.idsort.IdSortComp;
import se.sics.p2ptoolbox.gradient.idsort.IdViewComparator;
import se.sics.p2ptoolbox.util.config.SystemConfig;
import se.sics.p2ptoolbox.util.filters.IntegerOverlayFilter;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class IdSortHostComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(IdSortHostComp.class);

    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);

    private final SystemConfig systemConfig;
    private final CroupierConfig croupierConfig;
    private final GradientConfig gradientConfig;
    private final String logPrefix;

    public IdSortHostComp(HostInit init) {
        this.systemConfig = init.systemConfig;
        this.croupierConfig = init.croupierConfig;
        this.gradientConfig = init.gradientConfig;
        this.logPrefix = systemConfig.self.toString();
        log.info("{} initiating with bootstrap:{}", logPrefix, systemConfig.bootstrapNodes);

        subscribe(handleStart, control);
        subscribe(handleStop, control);

        CroupierComp.CroupierInit croupierInit = new CroupierComp.CroupierInit(croupierConfig, 10, systemConfig.self, systemConfig.bootstrapNodes, init.seed);
        Component croupier = createNConnectCroupier(croupierInit);

        GradientComp.GradientInit gradientInit = new GradientComp.GradientInit(systemConfig.self, gradientConfig, 11, new IdViewComparator(), new NoFilter(), init.seed);
        Component gradient = createNConnectGradient(gradientInit, croupier);

        IdSortComp.IdSortInit exampleInit = new IdSortComp.IdSortInit(systemConfig.self);
        Component compA = createNConnectIdSort(exampleInit, croupier, gradient);

        subscribe(handleDisconnected, croupier.getPositive(CroupierControlPort.class));
    }

    private Handler<Start> handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            log.info("{} starting...", logPrefix);
        }
    };

    private Handler<Stop> handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            log.info("{} stopping...", logPrefix);
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
            log.info("{} croupier:{} disconnected", logPrefix, event.overlayId);
        }
    };

    public static class HostInit extends Init<IdSortHostComp> {

        public final long seed;
        public final SystemConfig systemConfig;
        public final CroupierConfig croupierConfig;
        public final GradientConfig gradientConfig;
        
        public HostInit(long seed, SystemConfig systemConfig, CroupierConfig croupierConfig, GradientConfig gradientConfig) {
            this.seed = seed;
            this.systemConfig = systemConfig;
            this.croupierConfig = croupierConfig;
            this.gradientConfig = gradientConfig;
        }
    }
}

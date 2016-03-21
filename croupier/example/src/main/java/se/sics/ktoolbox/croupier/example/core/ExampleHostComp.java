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
package se.sics.ktoolbox.croupier.example.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.croupier.CroupierComp;
import se.sics.p2ptoolbox.croupier.CroupierControlPort;
import se.sics.p2ptoolbox.croupier.CroupierPort;
import se.sics.p2ptoolbox.croupier.msg.CroupierDisconnected;
import se.sics.p2ptoolbox.croupier.msg.CroupierJoin;
import se.sics.p2ptoolbox.util.config.KConfigCache;
import se.sics.p2ptoolbox.util.config.KConfigCore;
import se.sics.p2ptoolbox.util.filters.IntegerOverlayFilter;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.update.SelfViewUpdatePort;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ExampleHostComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(ExampleHostComp.class);
    private final String logPrefix;

    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);

    private final KConfigCache config;
    private final DecoratedAddress self;

    private Component croupier;
    private Component compA;

    public ExampleHostComp(HostInit init) {
        config = init.config;
        self = config.read(ExampleHostKConfig.self).get();
        this.logPrefix = self.getBase().toString() + " ";
        log.info("{}initiating...", logPrefix);

        createNConnectCroupier(10);
        createNConnectCompA();

        subscribe(handleDisconnected, croupier.getPositive(CroupierControlPort.class));
        subscribe(handleStart, control);
    }

    private void createNConnectCroupier(int overlayId) {
        long croupierSeed = config.read(ExampleHostKConfig.seed).get() + overlayId;
        croupier = create(CroupierComp.class, new CroupierComp.CroupierInit(config.configCore, self, overlayId, croupierSeed));
        connect(croupier.getNegative(Network.class), network, new IntegerOverlayFilter(overlayId));
        connect(croupier.getNegative(Timer.class), timer);
    }

    private void createNConnectCompA() {
        compA = create(ExampleComponentA.class, new ExampleComponentA.ExampleInitA(self,
                config.read(ExampleHostKConfig.seed).get(),
                config.read(ExampleHostKConfig.observer).get()));
        connect(croupier.getPositive(CroupierPort.class), compA.getNegative(CroupierPort.class));
        connect(croupier.getNegative(SelfViewUpdatePort.class), compA.getPositive(SelfViewUpdatePort.class));
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            log.info("{} starting...", logPrefix);
            trigger(new CroupierJoin(config.read(ExampleHostKConfig.bootstrap).get()), 
                    croupier.getPositive(CroupierControlPort.class));
        }
    };

    private Handler handleDisconnected = new Handler<CroupierDisconnected>() {
        @Override
        public void handle(CroupierDisconnected event) {
            log.info("{} croupier:{} disconnected", logPrefix, event.overlayId);
        }
    };

    public static class HostInit extends Init<ExampleHostComp> {

        public final KConfigCache config;

        public HostInit(KConfigCore configCore) {
            this.config = new KConfigCache(configCore);
        }
    }
}

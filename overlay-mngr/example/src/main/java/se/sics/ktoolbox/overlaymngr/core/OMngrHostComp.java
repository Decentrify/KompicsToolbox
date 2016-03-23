/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * KompicsToolbox is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.overlaymngr.core;

import java.security.SecureRandom;
import java.util.Random;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.croupier.CroupierPort;
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.overlaymngr.OverlayMngrPort;
import se.sics.ktoolbox.overlaymngr.core.TestCroupierComp.TestCroupierInit;
import se.sics.ktoolbox.overlaymngr.core.TestGradientComp.TestGradientInit;
import se.sics.ktoolbox.overlaymngr.events.OMngrCroupier;
import se.sics.ktoolbox.overlaymngr.events.OMngrTGradient;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.OverlayIdFactory;
import se.sics.ktoolbox.util.idextractor.EventOverlayIdExtractor;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.nat.NatAwareAddress;
import se.sics.ktoolbox.util.network.ports.One2NChannel;
import se.sics.ktoolbox.util.overlays.id.OverlayIdRegistry;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdatePort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OMngrHostComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(OMngrHostComp.class);
    private String logPrefix = " ";

    //***************************CONNECTIONS************************************
    //***********************CONNECT TO EXTERNALY*******************************
    private final Positive<OverlayMngrPort> omngrPort = requires(OverlayMngrPort.class);
    //****************************INTERNAL**************************************
    private One2NChannel<CroupierPort> croupierEnd;
    private One2NChannel<GradientPort> gradientEnd;
    private One2NChannel<OverlayViewUpdatePort> viewUpdateEnd;
    //*****************************CONFIG***************************************
    private final OMngrHostKCWrapper hostConfig;
    private final byte owner = 0x10;
    //******************************EXTENAL_STATE*******************************
    private NatAwareAddress selfAdr;
    private final ExtPort extPorts;
    //*******************************CLEANUP************************************
    private Pair<Component, Channel[]> testCroupier1, testCroupier2;
    private Pair<Component, Channel[]> testGradient1, testGradient2;
    //*********************************AUX**************************************
    private OMngrCroupier.ConnectRequest cReq1, cReq2;
    private OMngrTGradient.ConnectRequest gReq1, gReq2;

    public OMngrHostComp(Init init) {
        hostConfig = new OMngrHostKCWrapper(config());
        SystemKCWrapper systemConfig = new SystemKCWrapper(config());
        logPrefix = "<id:" + systemConfig.id + "> ";
        LOG.info("{}initiating...", logPrefix);

        selfAdr = (NatAwareAddress) init.selfAdr;
        extPorts = init.extPorts;

        croupierEnd = One2NChannel.getChannel("test", extPorts.croupierPort, new EventOverlayIdExtractor());
        gradientEnd = One2NChannel.getChannel("test", extPorts.gradientPort, new EventOverlayIdExtractor());
        viewUpdateEnd = One2NChannel.getChannel("test", extPorts.viewUpdatePort, new EventOverlayIdExtractor());

        subscribe(handleStart, control);
        subscribe(handleCroupierConnected, omngrPort);
        subscribe(handleGradientConnected, omngrPort);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            OverlayIdRegistry.registerPrefix("test", owner);
            connectTestCroupier1Comp();
            connectTestCroupier2Comp();
            connectTestGradient1Comp();
            connectTestGradient2Comp();

            trigger(cReq1, omngrPort);
            trigger(cReq2, omngrPort);
            trigger(gReq1, omngrPort);
            trigger(gReq2, omngrPort);
        }
    };

    Handler handleCroupierConnected = new Handler<OMngrCroupier.ConnectResponse>() {
        @Override
        public void handle(OMngrCroupier.ConnectResponse resp) {
            LOG.info("{}croupier:{} connected", new Object[]{logPrefix, resp.req.croupierId});
            if (resp.getId().equals(cReq1.getId())) {
                trigger(Start.event, testCroupier1.getValue0().control());
            }
            if (resp.getId().equals(cReq2.getId())) {
                trigger(Start.event, testCroupier2.getValue0().control());
            }
        }
    };

    Handler handleGradientConnected = new Handler<OMngrTGradient.ConnectResponse>() {
        @Override
        public void handle(OMngrTGradient.ConnectResponse resp) {
            LOG.info("{}gradient:{} connected", new Object[]{logPrefix, resp.req.tgradientId});
            if (resp.getId().equals(gReq1.getId())) {
                trigger(Start.event, testGradient1.getValue0().control());
            }
            if (resp.getId().equals(gReq2.getId())) {
                trigger(Start.event, testGradient2.getValue0().control());
            }
        }
    };

    private void connectTestCroupier1Comp() {
        Identifier croupierId = OverlayIdFactory.getId(owner, OverlayIdFactory.Type.CROUPIER, new byte[]{0, 0, 1});
        Component tc1Comp = create(TestCroupierComp.class, new TestCroupierInit(croupierId));
        Channel[] tc1Channels = new Channel[0];
        croupierEnd.addChannel(croupierId, tc1Comp.getNegative(CroupierPort.class));
        viewUpdateEnd.addChannel(croupierId, tc1Comp.getPositive(OverlayViewUpdatePort.class));
        testCroupier1 = Pair.with(tc1Comp, tc1Channels);
        cReq1 = new OMngrCroupier.ConnectRequest(croupierId, false);
    }

    private void connectTestCroupier2Comp() {
        Identifier croupierId = OverlayIdFactory.getId(owner, OverlayIdFactory.Type.CROUPIER, new byte[]{0, 0, 2});
        Component tc2Comp = create(TestCroupierComp.class, new TestCroupierInit(croupierId));
        Channel[] tc2Channels = new Channel[0];
        croupierEnd.addChannel(croupierId, tc2Comp.getNegative(CroupierPort.class));
        viewUpdateEnd.addChannel(croupierId, tc2Comp.getPositive(OverlayViewUpdatePort.class));
        testCroupier2 = Pair.with(tc2Comp, tc2Channels);
        cReq2 = new OMngrCroupier.ConnectRequest(croupierId, false);
    }

    private void connectTestGradient1Comp() {
        Identifier tgradientId = OverlayIdFactory.getId(owner, OverlayIdFactory.Type.TGRADIENT, new byte[]{0, 0, 3});
        Identifier croupierId = OverlayIdFactory.changeType(tgradientId, OverlayIdFactory.Type.CROUPIER);

        Random rand = new SecureRandom();
        Component tg1Comp = create(TestGradientComp.class, new TestGradientInit(rand.nextInt(), tgradientId));
        Channel[] tg1Channels = new Channel[0];
        croupierEnd.addChannel(croupierId, tg1Comp.getNegative(CroupierPort.class));
        gradientEnd.addChannel(tgradientId, tg1Comp.getNegative(GradientPort.class));
        viewUpdateEnd.addChannel(tgradientId, tg1Comp.getPositive(OverlayViewUpdatePort.class));
        testGradient1 = Pair.with(tg1Comp, tg1Channels);
        gReq1 = new OMngrTGradient.ConnectRequest(tgradientId, new IdComparator(), new IdGradientFilter());
    }

    private void connectTestGradient2Comp() {
        Identifier tgradientId = OverlayIdFactory.getId(owner, OverlayIdFactory.Type.TGRADIENT, new byte[]{0, 0, 4});
        Identifier croupierId = OverlayIdFactory.changeType(tgradientId, OverlayIdFactory.Type.CROUPIER);

        Random rand = new SecureRandom();
        Component tg2Comp = create(TestGradientComp.class, new TestGradientInit(rand.nextInt(), tgradientId));
        Channel[] tg2Channels = new Channel[0];
        croupierEnd.addChannel(croupierId, tg2Comp.getNegative(CroupierPort.class));
        gradientEnd.addChannel(tgradientId, tg2Comp.getNegative(GradientPort.class));
        viewUpdateEnd.addChannel(tgradientId, tg2Comp.getPositive(OverlayViewUpdatePort.class));
        testGradient2 = Pair.with(tg2Comp, tg2Channels);
        gReq2 = new OMngrTGradient.ConnectRequest(tgradientId, new IdComparator(), new IdGradientFilter());
    }

    public static class Init extends se.sics.kompics.Init<OMngrHostComp> {

        public final KAddress selfAdr;
        public final ExtPort extPorts;

        public Init(KAddress selfAdr, ExtPort extPorts) {
            this.selfAdr = selfAdr;
            this.extPorts = extPorts;
        }
    }

    public static class ExtPort {

        public final Positive<Timer> timerPort;
        public final Positive<Network> networkPort;
        public final Positive<CroupierPort> croupierPort;
        public final Positive<GradientPort> gradientPort;
        public final Negative<OverlayViewUpdatePort> viewUpdatePort;

        public ExtPort(Positive<Timer> timer, Positive<Network> networkPort,
                Positive<CroupierPort> croupierPort, Positive<GradientPort> gradientPort,
                Negative<OverlayViewUpdatePort> viewUpdatePort) {
            this.timerPort = timer;
            this.networkPort = networkPort;
            this.croupierPort = croupierPort;
            this.gradientPort = gradientPort;
            this.viewUpdatePort = viewUpdatePort;
        }
    }
}

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
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.cc.bootstrap.CCBootstrapComp;
import se.sics.ktoolbox.cc.bootstrap.CCOperationPort;
import se.sics.ktoolbox.cc.bootstrap.event.status.CCBootstrapDisconnected;
import se.sics.ktoolbox.cc.bootstrap.event.status.CCBootstrapReady;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatComp;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatPort;
import se.sics.ktoolbox.cc.heartbeat.event.status.CCHeartbeatReady;
import se.sics.ktoolbox.croupier.CroupierPort;
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.overlaymngr.OverlayMngrComp;
import se.sics.ktoolbox.overlaymngr.OverlayMngrPort;
import se.sics.ktoolbox.overlaymngr.core.TestCroupierComp.TestCroupierInit;
import se.sics.ktoolbox.overlaymngr.core.TestGradientComp.TestGradientInit;
import se.sics.ktoolbox.overlaymngr.events.OMngrCroupier;
import se.sics.ktoolbox.overlaymngr.events.OMngrTGradient;
import se.sics.ktoolbox.util.address.AddressUpdate;
import se.sics.ktoolbox.util.address.AddressUpdatePort;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.IntIdentifier;
import se.sics.ktoolbox.util.overlays.id.OverlayIdHelper;
import se.sics.ktoolbox.util.network.nat.NatAwareAddress;
import se.sics.ktoolbox.util.network.nat.NatAwareAddressImpl;
import se.sics.ktoolbox.util.network.ports.One2NChannel;
import se.sics.ktoolbox.util.overlays.EventOverlayIdExtractor;
import se.sics.ktoolbox.util.status.Status;
import se.sics.ktoolbox.util.status.StatusPort;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdatePort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OMngrHostComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(OMngrHostComp.class);
    private String logPrefix = " ";

    //***************************CONNECTIONS************************************
    //provided external connections - CONNECT to
    private final Positive<StatusPort> inStatusPort = requires(StatusPort.class);
    private final Positive<AddressUpdatePort> addressUpdatePort = requires(AddressUpdatePort.class);
    //provided external connection - do NOT connect to
    private final ExtPort extPorts;
    //for internal use - DO NOT connect to
    private final Positive<OverlayMngrPort> omngrPort = requires(OverlayMngrPort.class);
    private One2NChannel<CroupierPort> croupierEnd;
    private One2NChannel<GradientPort> gradientEnd;
    private One2NChannel<OverlayViewUpdatePort> viewUpdateEnd;
    //***********************************CONFIG*********************************
    private final OMngrHostKCWrapper hostConfig;
    private NatAwareAddress selfAdr;
    private final byte owner = 0x10;
    //*******************************CLEANUP************************************
    private Pair<Component, Channel[]> caracalClient;
    private Pair<Component, Channel[]> ccHeartbeat;
    private Pair<Component, Channel[]> overlayMngr;
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

        extPorts = init.extPorts;

        subscribe(handleStart, control);
        subscribe(handleAddressUpdate, addressUpdatePort);
    }

    private void connectCaracalClient() {
        Component ccComp = create(CCBootstrapComp.class, new CCBootstrapComp.CCBootstrapInit(selfAdr));
        Channel[] ccChannels = new Channel[3];
        ccChannels[0] = connect(ccComp.getNegative(Timer.class), extPorts.timerPort, Channel.TWO_WAY);
        ccChannels[1] = connect(ccComp.getNegative(Network.class), extPorts.networkPort, Channel.TWO_WAY);
        ccChannels[2] = connect(ccComp.getPositive(StatusPort.class), inStatusPort.getPair(), Channel.TWO_WAY);
        caracalClient = Pair.with(ccComp, ccChannels);
    }

    private void connectCCHeartbeat() {
        Component ccHComp = create(CCHeartbeatComp.class, new CCHeartbeatComp.CCHeartbeatInit(selfAdr));
        Channel[] ccHChannels = new Channel[4];
        ccHChannels[0] = connect(ccHComp.getNegative(Timer.class), extPorts.timerPort, Channel.TWO_WAY);
        ccHChannels[1] = connect(ccHComp.getNegative(CCOperationPort.class), caracalClient.getValue0().getPositive(CCOperationPort.class), Channel.TWO_WAY);
        ccHChannels[2] = connect(ccHComp.getNegative(StatusPort.class), caracalClient.getValue0().getPositive(StatusPort.class), Channel.TWO_WAY);
        ccHChannels[3] = connect(ccHComp.getPositive(StatusPort.class), inStatusPort.getPair(), Channel.TWO_WAY);
        ccHeartbeat = Pair.with(ccHComp, ccHChannels);
    }

    private void connectOverlayMngr() {
        Component oMngrComp = create(OverlayMngrComp.class, new OverlayMngrComp.Init(
                new OverlayMngrComp.ExtPort(extPorts.timerPort, extPorts.networkPort, extPorts.addressUpdatePort,
                        ccHeartbeat.getValue0().getPositive(CCHeartbeatPort.class))));
        Channel[] oMngrChannels = new Channel[1];
        oMngrChannels[0] = connect(oMngrComp.getPositive(OverlayMngrPort.class), omngrPort.getPair(), Channel.TWO_WAY);
        croupierEnd = One2NChannel.getChannel(oMngrComp.getPositive(CroupierPort.class), new EventOverlayIdExtractor());
        gradientEnd = One2NChannel.getChannel(oMngrComp.getPositive(GradientPort.class), new EventOverlayIdExtractor());
        viewUpdateEnd = One2NChannel.getChannel(oMngrComp.getNegative(OverlayViewUpdatePort.class), new EventOverlayIdExtractor());
        overlayMngr = Pair.with(oMngrComp, oMngrChannels);
    }

    private void connectTestCroupier1Comp() {
        Identifier croupierId = OverlayIdHelper.getIntIdentifier(owner, OverlayIdHelper.Type.CROUPIER, new byte[]{0, 0, 1});
        Component tc1Comp = create(TestCroupierComp.class, new TestCroupierInit(croupierId));
        Channel[] tc1Channels = new Channel[0];
        croupierEnd.addChannel(croupierId, tc1Comp.getNegative(CroupierPort.class));
        viewUpdateEnd.addChannel(croupierId, tc1Comp.getPositive(OverlayViewUpdatePort.class));
        testCroupier1 = Pair.with(tc1Comp, tc1Channels);
        cReq1 = new OMngrCroupier.ConnectRequest(croupierId, false);
        trigger(cReq1, omngrPort);
    }

    private void connectTestCroupier2Comp() {
        Identifier croupierId = OverlayIdHelper.getIntIdentifier(owner, OverlayIdHelper.Type.CROUPIER, new byte[]{0, 0, 2});
        Component tc2Comp = create(TestCroupierComp.class, new TestCroupierInit(croupierId));
        Channel[] tc2Channels = new Channel[0];
        croupierEnd.addChannel(croupierId, tc2Comp.getNegative(CroupierPort.class));
        viewUpdateEnd.addChannel(croupierId, tc2Comp.getPositive(OverlayViewUpdatePort.class));
        testCroupier2 = Pair.with(tc2Comp, tc2Channels);
        cReq2 = new OMngrCroupier.ConnectRequest(croupierId, false);
        trigger(cReq2, omngrPort);
    }

    private void connectTestGradient1Comp() {
        Identifier tgradientId = OverlayIdHelper.getIntIdentifier(owner, OverlayIdHelper.Type.TGRADIENT, new byte[]{0, 0, 3});
        Identifier croupierId = OverlayIdHelper.changeOverlayType((IntIdentifier) tgradientId, OverlayIdHelper.Type.CROUPIER);

        Random rand = new SecureRandom();
        Component tg1Comp = create(TestGradientComp.class, new TestGradientInit(rand.nextInt(), tgradientId));
        Channel[] tg1Channels = new Channel[0];
        croupierEnd.addChannel(croupierId, tg1Comp.getNegative(CroupierPort.class));
        gradientEnd.addChannel(tgradientId, tg1Comp.getNegative(GradientPort.class));
        viewUpdateEnd.addChannel(tgradientId, tg1Comp.getPositive(OverlayViewUpdatePort.class));
        testGradient1 = Pair.with(tg1Comp, tg1Channels);
        gReq1 = new OMngrTGradient.ConnectRequest(tgradientId, new IdComparator(), new IdGradientFilter());
        trigger(gReq1, omngrPort);
    }

    private void connectTestGradient2Comp() {
        Identifier tgradientId = OverlayIdHelper.getIntIdentifier(owner, OverlayIdHelper.Type.TGRADIENT, new byte[]{0, 0, 4});
        Identifier croupierId = OverlayIdHelper.changeOverlayType((IntIdentifier) tgradientId, OverlayIdHelper.Type.CROUPIER);

        Random rand = new SecureRandom();
        Component tg2Comp = create(TestGradientComp.class, new TestGradientInit(rand.nextInt(), tgradientId));
        Channel[] tg2Channels = new Channel[0];
        croupierEnd.addChannel(croupierId, tg2Comp.getNegative(CroupierPort.class));
        gradientEnd.addChannel(tgradientId, tg2Comp.getNegative(GradientPort.class));
        viewUpdateEnd.addChannel(tgradientId, tg2Comp.getPositive(OverlayViewUpdatePort.class));
        testGradient2 = Pair.with(tg2Comp, tg2Channels);
        gReq2 = new OMngrTGradient.ConnectRequest(tgradientId, new IdComparator(), new IdGradientFilter());
        trigger(gReq2, omngrPort);
    }

    //********************************CONTROL***********************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            trigger(new AddressUpdate.Request(), addressUpdatePort);
        }
    };
    //**************************************************************************
    Handler handleAddressUpdate = new Handler<AddressUpdate.Indication>() {
        @Override
        public void handle(AddressUpdate.Indication update) {
            LOG.info("{}update address from:{} to:{}", new Object[]{logPrefix, selfAdr, update.localAddress});
            if (selfAdr == null) {
                selfAdr = (NatAwareAddress) update.localAddress;

                subscribe(handleCaracalReady, inStatusPort);
                subscribe(handleCaracalDisconnect, inStatusPort);
                subscribe(handleHeartbeatReady, inStatusPort);

                connectCaracalClient();
                connectCCHeartbeat();

                trigger(Start.event, caracalClient.getValue0().control());
                trigger(Start.event, ccHeartbeat.getValue0().control());
            } else {
                selfAdr = (NatAwareAddress) update.localAddress;
            }
        }
    };

    ClassMatchedHandler handleCaracalReady
            = new ClassMatchedHandler<CCBootstrapReady, Status.Internal<CCBootstrapReady>>() {

                @Override
                public void handle(CCBootstrapReady content, Status.Internal<CCBootstrapReady> container) {
                    LOG.info("{}caracal ready", logPrefix);
                }
            };

    ClassMatchedHandler handleHeartbeatReady
            = new ClassMatchedHandler<CCHeartbeatReady, Status.Internal<CCHeartbeatReady>>() {
                @Override
                public void handle(CCHeartbeatReady content, Status.Internal<CCHeartbeatReady> container) {
                    LOG.info("{}heartbeating ready", logPrefix);

                    subscribe(handleCroupierConnected, omngrPort);
                    subscribe(handleGradientConnected, omngrPort);
                    connectOverlayMngr();
                    trigger(Start.event, overlayMngr.getValue0().control());

                    connectTestCroupier1Comp();
                    connectTestCroupier2Comp();
                    connectTestGradient1Comp();
                    connectTestGradient2Comp();
                }
            };

    /**
     * Caracal client gets disconnected.
     */
    ClassMatchedHandler handleCaracalDisconnect
            = new ClassMatchedHandler<CCBootstrapDisconnected, Status.Internal<CCBootstrapDisconnected>>() {

                @Override
                public void handle(CCBootstrapDisconnected content, Status.Internal<CCBootstrapDisconnected> container) {
                    LOG.warn("{}caracal client disconnected", logPrefix);
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

    public static class Init extends se.sics.kompics.Init<OMngrHostComp> {

        public final ExtPort extPorts;

        public Init(ExtPort extPorts) {
            this.extPorts = extPorts;
        }
    }

    public static class ExtPort {

        public final Positive<Timer> timerPort;
        public final Positive<Network> networkPort;
        public final Positive<AddressUpdatePort> addressUpdatePort;

        public ExtPort(Positive<Timer> timer, Positive<Network> networkPort,
                Positive<AddressUpdatePort> addressUpdatePort) {
            this.timerPort = timer;
            this.networkPort = networkPort;
            this.addressUpdatePort = addressUpdatePort;
        }
    }
}

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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Channel;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
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
import se.sics.ktoolbox.overlaymngr.OverlayMngrComp.OverlayMngrInit;
import se.sics.ktoolbox.overlaymngr.OverlayMngrPort;
import se.sics.ktoolbox.overlaymngr.core.TestCroupierComp.TestCroupierInit;
import se.sics.ktoolbox.overlaymngr.core.TestGradientComp.TestGradientInit;
import se.sics.ktoolbox.overlaymngr.events.OMngrCroupier;
import se.sics.ktoolbox.overlaymngr.events.OMngrTGradient;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.IntIdentifier;
import se.sics.ktoolbox.util.network.basic.BasicAddress;
import se.sics.ktoolbox.util.network.nat.NatAwareAddress;
import se.sics.ktoolbox.util.network.nat.NatAwareAddressImpl;
import se.sics.ktoolbox.util.status.Status;
import se.sics.ktoolbox.util.status.StatusPort;
import se.sics.ktoolbox.util.update.view.ViewUpdatePort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OMngrHostComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(OMngrHostComp.class);
    private String logPrefix = " ";

    private final Positive timerPort = requires(Timer.class);
    private final Positive networkPort = requires(Network.class);
    private final Positive inStatusPort = requires(StatusPort.class);
    private final Positive omngrPort = requires(OverlayMngrPort.class);

    private final OMngrHostKCWrapper hostConfig;
    private NatAwareAddress self;

    private Component caracalClientComp;
    private Component ccHeartbeatComp;
    private Component overlayMngrComp;
    private Component testCroupierComp;
    private Component testGradientComp;

    public OMngrHostComp(OMngrHostInit init) {
        this.hostConfig = new OMngrHostKCWrapper(config());
        this.self = NatAwareAddressImpl.open(hostConfig.self);
        this.logPrefix = "<id:" + self.getId().toString() + "> ";
        LOG.info("{}initiating...", logPrefix);

        connectCaracalClient();
        connectCCHeartbeat();

        subscribe(handleStart, control);
        subscribe(handleCaracalReady, inStatusPort);
        subscribe(handleCaracalDisconnect, inStatusPort);
        subscribe(handleHeartbeatReady, inStatusPort);
        subscribe(handleCroupierConnected, omngrPort);
        subscribe(handleGradientConnected, omngrPort);
    }

    private void connectCaracalClient() {
        caracalClientComp = create(CCBootstrapComp.class, new CCBootstrapComp.CCBootstrapInit(self));
        connect(caracalClientComp.getNegative(Timer.class), timerPort, Channel.TWO_WAY);
        connect(caracalClientComp.getNegative(Network.class), networkPort, Channel.TWO_WAY);
        connect(caracalClientComp.getPositive(StatusPort.class), inStatusPort.getPair(), Channel.TWO_WAY);
    }

    private void connectCCHeartbeat() {
        ccHeartbeatComp = create(CCHeartbeatComp.class, new CCHeartbeatComp.CCHeartbeatInit(self));
        connect(ccHeartbeatComp.getNegative(Timer.class), timerPort, Channel.TWO_WAY);
        connect(ccHeartbeatComp.getNegative(CCOperationPort.class), caracalClientComp.getPositive(CCOperationPort.class), Channel.TWO_WAY);
        connect(ccHeartbeatComp.getNegative(StatusPort.class), caracalClientComp.getPositive(StatusPort.class), Channel.TWO_WAY);
        connect(ccHeartbeatComp.getPositive(StatusPort.class), inStatusPort.getPair(), Channel.TWO_WAY);
    }

    private void connectOverlayMngr() {
        List<NatAwareAddress> bootstrap = new ArrayList<>();
        for (BasicAddress adr : hostConfig.bootstrap) {
            bootstrap.add(NatAwareAddressImpl.open(adr));
        }
        overlayMngrComp = create(OverlayMngrComp.class, new OverlayMngrInit(self, bootstrap));
        connect(overlayMngrComp.getNegative(Timer.class), timerPort, Channel.TWO_WAY);
        connect(overlayMngrComp.getNegative(Network.class), networkPort, Channel.TWO_WAY);
        connect(overlayMngrComp.getNegative(CCHeartbeatPort.class), ccHeartbeatComp.getPositive(CCHeartbeatPort.class), Channel.TWO_WAY);
        connect(overlayMngrComp.getPositive(OverlayMngrPort.class), omngrPort.getPair(), Channel.TWO_WAY);
    }
    
    private void connectTestCroupierComp() {
        Identifier croupierId = new IntIdentifier(1);
        testCroupierComp = create(TestCroupierComp.class, new TestCroupierInit());
        OMngrCroupier.ConnectRequestBuilder reqBuilder = new OMngrCroupier.ConnectRequestBuilder();
        reqBuilder.setIdentifiers(croupierId);
        reqBuilder.setupCroupier(false);
        reqBuilder.connectTo(testCroupierComp.getNegative(CroupierPort.class), testCroupierComp.getPositive(ViewUpdatePort.class));
        trigger(reqBuilder.build(), omngrPort);
    }
    
    private void connectTestGradientComp() {
        Identifier croupierId = new IntIdentifier(2);
        Identifier gradientId = new IntIdentifier(3);
        Identifier tgradientId = new IntIdentifier(4);
        Random rand = new SecureRandom();
        testGradientComp = create(TestGradientComp.class, new TestGradientInit(rand.nextInt()));
        OMngrTGradient.ConnectRequestBuilder reqBuilder = new OMngrTGradient.ConnectRequestBuilder();
        reqBuilder.setIdentifiers(croupierId, gradientId, tgradientId);
        reqBuilder.setupGradient(new IdComparator(), new IdGradientFilter());
        reqBuilder.connectTo(testGradientComp.getNegative(GradientPort.class), testGradientComp.getPositive(ViewUpdatePort.class));
        trigger(reqBuilder.build(), omngrPort);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
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
                    connectOverlayMngr();
                    trigger(Start.event, overlayMngrComp.control());
                    connectTestCroupierComp();
                    connectTestGradientComp();
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
        public void handle(OMngrCroupier.ConnectResponse event) {
            LOG.info("{}croupier:{} connected", new Object[]{logPrefix, event.req.croupierId});
            trigger(Start.event, testCroupierComp.control());
        }
    };
    
    Handler handleGradientConnected = new Handler<OMngrTGradient.ConnectResponse>() {
        @Override
        public void handle(OMngrTGradient.ConnectResponse event) {
            LOG.info("{}gradient:{} connected", new Object[]{logPrefix, event.req.tgradientId});
            trigger(Start.event, testGradientComp.control());
        }
    };

    public static class OMngrHostInit extends Init<OMngrHostComp> {
    }
}

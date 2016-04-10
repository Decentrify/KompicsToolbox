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
package se.sics.ktoolbox.overlaymngr.bootstrap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatPort;
import se.sics.ktoolbox.cc.heartbeat.event.CCHeartbeat;
import se.sics.ktoolbox.cc.heartbeat.event.CCOverlaySample;
import se.sics.ktoolbox.croupier.CroupierControlPort;
import se.sics.ktoolbox.croupier.event.CroupierDisconnected;
import se.sics.ktoolbox.croupier.event.CroupierJoin;
import se.sics.ktoolbox.overlaymngr.OverlayMngrConfig;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.Identifiable;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CroupierBootstrapComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(CroupierBootstrapComp.class);
    private String logPrefix = " ";

    //*******************************CONNECTIONS********************************
    private final Positive timerPort = requires(Timer.class);
    private final Positive heartbeatPort = requires(CCHeartbeatPort.class);
    private final Positive croupierStatusPort = requires(CroupierControlPort.class);
    private final Negative bootstrapPort = provides(CroupierBootstrapPort.class);
    //******************************INTERNAL_STATE******************************
    //TODO move as config - rebootstraping
    private static long rebootPeriod = 2000;
    private static int rebootMaxMult = 5;
    //*****
    private Random rand;
    private Map<Identifier, Integer> rebootstrap = new HashMap<>();

    public CroupierBootstrapComp(Init init) {
        SystemKCWrapper systemConfig = new SystemKCWrapper(config());
        logPrefix = "<nid:" + systemConfig.id + ">";
        LOG.info("{}initiating...", logPrefix);

        rand = new Random(systemConfig.seed);

        subscribe(handleStart, control);
        subscribe(handleCroupierBootstrap, bootstrapPort);
        subscribe(handleExternalSample, heartbeatPort);
        subscribe(handleDisconnect, croupierStatusPort);
        subscribe(handleRebootstrap, timerPort);
    }

    //******************************CONTROL*************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
        }
    };

    //**************************************************************************
    Handler handleCroupierBootstrap = new Handler<OMCroupierBootstrap>() {
        @Override
        public void handle(OMCroupierBootstrap event) {
            LOG.trace("{}{}", logPrefix, event);
            trigger(new CCHeartbeat.Start(event.overlayId), heartbeatPort);
            trigger(new CCOverlaySample.Request(event.overlayId), heartbeatPort);
            scheduleNextRebootstrap(event.overlayId, 1);
        }
    };

    Handler handleExternalSample = new Handler<CCOverlaySample.Response>() {

        @Override
        public void handle(CCOverlaySample.Response resp) {
            LOG.info("{}overlay:{} external bootstrap:{}", new Object[]{logPrefix, resp.req.overlayId, resp.overlaySample});
            if (OverlayMngrConfig.isGlobalCroupier(resp.req.overlayId)) {
                //TODO Alex
            } else {
                trigger(new CroupierJoin(resp.req.overlayId, resp.overlaySample), croupierStatusPort);
            }
        }
    };

    Handler handleDisconnect = new Handler<CroupierDisconnected>() {
        @Override
        public void handle(CroupierDisconnected event) {
            if (OverlayMngrConfig.isGlobalCroupier(event.overlayId)) {
                LOG.warn("{}global croupier disconnected", logPrefix);
                //TODO Alex
            } else {
                LOG.info("{}croupier:{} disconnected", new Object[]{logPrefix, event.overlayId});
                trigger(new CCOverlaySample.Request(event.overlayId), heartbeatPort);
                rebootstrap.put(event.overlayId, 1); //speed up until rebootstrapped
            }
        }
    };

    Handler handleRebootstrap = new Handler<RebootstrapTimeout>() {
        @Override
        public void handle(RebootstrapTimeout event) {
            LOG.debug("{}rebootstraping...", logPrefix);
            trigger(new CCOverlaySample.Request(event.overlayId), heartbeatPort);
            scheduleNextRebootstrap(event.overlayId, rebootstrap.get(event.overlayId));
        }
    };

    //**********************************TIMEOUTS********************************
    private void cancelTimeout(UUID timeoutId) {
        CancelTimeout cpt = new CancelTimeout(timeoutId);
        trigger(cpt, timerPort);

    }

    private void scheduleNextRebootstrap(Identifier overlayId, int multiplier) {
        if (multiplier < rebootMaxMult) {
            rebootstrap.put(overlayId, multiplier + 1);
        }
        //introduce some randomness - to spread the rebootstrap load
        //delay between rebootPeriod * [multiplier, multiplier^3] 
        long delay = rebootPeriod * multiplier * (rand.nextInt(multiplier) + 1) * (rand.nextInt(multiplier) + 1);
        
        ScheduleTimeout spt = new ScheduleTimeout(delay);
        RebootstrapTimeout sc = new RebootstrapTimeout(spt, overlayId);
        spt.setTimeoutEvent(sc);
        trigger(spt, timerPort);
    }

    private class RebootstrapTimeout extends Timeout implements Identifiable<Identifier> {

        public final Identifier overlayId;

        RebootstrapTimeout(ScheduleTimeout request, Identifier overlayId) {
            super(request);
            this.overlayId = overlayId;
        }

        @Override
        public String toString() {
            return "Rebootstrap<" + overlayId + "><Timeout<" + getId() + ">";
        }

        @Override
        public Identifier getId() {
            return new UUIDIdentifier(getTimeoutId());
        }
    }

    public static class Init extends se.sics.kompics.Init<CroupierBootstrapComp> {
    }
}

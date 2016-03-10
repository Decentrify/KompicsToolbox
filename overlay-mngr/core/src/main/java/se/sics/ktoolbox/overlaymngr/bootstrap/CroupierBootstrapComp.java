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

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatPort;
import se.sics.ktoolbox.cc.heartbeat.event.CCHeartbeat;
import se.sics.ktoolbox.cc.heartbeat.event.CCOverlaySample;
import se.sics.ktoolbox.croupier.CroupierControlPort;
import se.sics.ktoolbox.croupier.event.CroupierControl;
import se.sics.ktoolbox.croupier.event.CroupierJoin;
import se.sics.ktoolbox.overlaymngr.OverlayMngrConfig;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CroupierBootstrapComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(CroupierBootstrapComp.class);
    private String logPrefix = " ";

    private final Positive timerPort = requires(Timer.class);
    private final Positive heartbeatPort = requires(CCHeartbeatPort.class);
    private final Positive croupierStatusPort = requires(CroupierControlPort.class);
    private final Negative bootstrapPort = provides(CroupierBootstrapPort.class);

    public CroupierBootstrapComp(Init init) {
        LOG.info("{}initiating...", logPrefix);
        
        subscribe(handleStart, control);
        subscribe(handleCroupierBootstrap, bootstrapPort);
        subscribe(handleExternalSample, heartbeatPort);
        subscribe(handleDisconnect, croupierStatusPort);
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
            trigger(new CCOverlaySample.Request(event.overlayId), heartbeatPort);
            trigger(new CCHeartbeat.Start(event.overlayId), heartbeatPort);
        }
    };

    Handler handleExternalSample = new Handler<CCOverlaySample.Response>() {

        @Override
        public void handle(CCOverlaySample.Response resp) {
            LOG.info("{}overlay:{} external bootstrap:{}", new Object[]{logPrefix, resp.req.overlayId, resp.overlaySample});
            if (OverlayMngrConfig.isGlobalCroupier(resp.req.overlayId)) {
                //TODO Alex
            } else {
                trigger(new CroupierJoin(resp.req.overlayId, new ArrayList<>(resp.overlaySample)), croupierStatusPort);
            }
        }
    };

    Handler handleDisconnect = new Handler<CroupierControl>() {
        @Override
        public void handle(CroupierControl event) {
            if (OverlayMngrConfig.isGlobalCroupier(event.overlayId)) {
                LOG.warn("{}global croupier disconnected", logPrefix);
                //TODO Alex
            } else {
                LOG.info("{}croupier:{} disconnected", new Object[]{logPrefix, event.overlayId});
                trigger(new CCOverlaySample.Request(event.overlayId), heartbeatPort);
            }
        }
    };

    public static class Init extends se.sics.kompics.Init<CroupierBootstrapComp> {
    }
}

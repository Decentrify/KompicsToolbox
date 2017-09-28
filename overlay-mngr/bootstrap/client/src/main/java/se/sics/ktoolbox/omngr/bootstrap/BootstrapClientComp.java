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
package se.sics.ktoolbox.omngr.bootstrap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.id.Identifier;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatPort;
import se.sics.ktoolbox.cc.heartbeat.event.CCHeartbeat;
import se.sics.ktoolbox.cc.heartbeat.event.CCOverlaySample;
import se.sics.ktoolbox.omngr.bootstrap.msg.Heartbeat;
import se.sics.ktoolbox.omngr.bootstrap.msg.Sample;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BootstrapClientComp extends ComponentDefinition {

    //hack - change to config later
    private final static int maxSampleSize = 10;
    private final static long heartbeatPeriod = 60 * 1000;
    private final static long msgTimeout = 2000;
    //**************************************************************************

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapClientComp.class);
    private String logPrefix = "";

    //******************************CONNECTIONS*********************************
    Negative<CCHeartbeatPort> heartbeatPort = provides(CCHeartbeatPort.class);
    Positive<Network> networkPort = requires(Network.class);
    Positive<Timer> timerPort = requires(Timer.class);
    //*****************************EXTERNAL_STATE*******************************
    private KAddress selfAdr;
    private KAddress bootstrapServer;
    //*****************************INTERNAL_STATE*******************************
    private Random rand;
    private Set<OverlayId> heartbeats = new HashSet<>();
    //********************************AUX_STATE*********************************
    private UUID heartbeatTimeout;
    private Map<Identifier, Pair<CCOverlaySample.Request, UUID>> pendingRequests = new HashMap<>();

    public BootstrapClientComp(Init init) {
        SystemKCWrapper systemConfig = new SystemKCWrapper(config());
        selfAdr = init.selfAdr;
        logPrefix = "<nid:" + selfAdr.getId() + ">";
        LOG.info("{}initiating with seed:{}", logPrefix, systemConfig.seed);

        bootstrapServer = init.bootstrapServer;

        rand = new Random(systemConfig.seed);

        subscribe(handleStart, control);
        subscribe(handleHeartbeat, timerPort);
        subscribe(handleHeartbeatStart, heartbeatPort);
        subscribe(handleSampleRequest, heartbeatPort);
        subscribe(handleSampleRequestTimeout, timerPort);
        subscribe(handleSampleResponse, networkPort);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            scheduleHearbeatTimeout();
        }
    };

    Handler handleHeartbeat = new Handler<HeartbeatTimeout>() {
        @Override
        public void handle(HeartbeatTimeout event) {
            LOG.debug("{}heartbeating", logPrefix);
            for (OverlayId overlayId : heartbeats) {
                sendHeartbeat(overlayId);
            }
        }
    };

    private void sendHeartbeat(OverlayId overlayId) {
        Heartbeat content = new Heartbeat(overlayId, rand.nextInt(maxSampleSize));
        //Heartbeat content = new Heartbeat(overlayId, ((IntIdentifier) selfAdr.getId()).id);
        KContentMsg container = new BasicContentMsg(new BasicHeader(selfAdr, bootstrapServer, Transport.UDP), content);
        LOG.trace("{}sending:{}", logPrefix, container);
        trigger(container, networkPort);
    }

    Handler handleHeartbeatStart = new Handler<CCHeartbeat.Start>() {
        @Override
        public void handle(CCHeartbeat.Start heartbeat) {
            LOG.info("{}heartbeat on:{}", logPrefix, heartbeat.overlayId);
            heartbeats.add(heartbeat.overlayId);
            sendHeartbeat(heartbeat.overlayId);
        }
    };

    Handler handleSampleRequest = new Handler<CCOverlaySample.Request>() {
        @Override
        public void handle(CCOverlaySample.Request req) {
            LOG.debug("{}sample request for:{}", logPrefix, req.overlayId);

            Sample.Request content = new Sample.Request(req.overlayId);
            UUID tId = scheduleSampleReqTimeout(content.getId());
            pendingRequests.put(content.getId(), Pair.with(req, tId));
            KContentMsg container = new BasicContentMsg(new BasicHeader(selfAdr, bootstrapServer, Transport.UDP), content);
            LOG.trace("{}sending:{}", logPrefix, container);
            trigger(container, networkPort);
        }
    };

    ClassMatchedHandler handleSampleResponse
            = new ClassMatchedHandler<Sample.Response, KContentMsg<?, ?, Sample.Response>>() {

                @Override
                public void handle(Sample.Response content, KContentMsg<?, ?, Sample.Response> container) {
                    LOG.trace("{}received:{}", logPrefix, container);
                    Pair<CCOverlaySample.Request, UUID> req = pendingRequests.remove(content.getId());
                    if (req == null) {
                        LOG.trace("{}late:{}", logPrefix, container);
                        return;
                    }
                    cancelTimeout(req.getValue1());
                    LOG.debug("{}sample response for:{}", logPrefix, req.getValue0().overlayId);
                    answer(req.getValue0(), req.getValue0().answer(content.sample));
                }
            };

    Handler handleSampleRequestTimeout = new Handler<SampleRequestTimeout>() {
        @Override
        public void handle(SampleRequestTimeout timeout) {
            LOG.debug("{}timeout on sample request", logPrefix);
            Pair<CCOverlaySample.Request, UUID> req = pendingRequests.remove(timeout.eventId);
            if (req == null) {
                LOG.trace("{}late:{}", logPrefix, timeout);
                return;
            }
            LOG.debug("{}sample response for:{}", logPrefix, req.getValue0().overlayId);
            answer(req.getValue0(), req.getValue0().answer(new ArrayList<KAddress>()));
        }
    };

    //*******************************TIMEOUTS***********************************
    private void cancelTimeout(UUID timeout) {
        CancelTimeout cpt = new CancelTimeout(timeout);
        trigger(cpt, timerPort);
    }

    private void scheduleHearbeatTimeout() {
        if (heartbeatTimeout != null) {
            LOG.warn("{} double starting heartbeat timeout", logPrefix);
        }
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(heartbeatPeriod, heartbeatPeriod);
        HeartbeatTimeout sc = new HeartbeatTimeout(spt);
        spt.setTimeoutEvent(sc);
        heartbeatTimeout = sc.getTimeoutId();
        trigger(spt, timerPort);
    }

    private void cancelHeartbeatTimeout() {
        if (heartbeatTimeout == null) {
            return;
        }
        CancelTimeout cpt = new CancelTimeout(heartbeatTimeout);
        heartbeatTimeout = null;
        trigger(cpt, timerPort);

    }

    private class HeartbeatTimeout extends Timeout {

        HeartbeatTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }

        @Override
        public String toString() {
            return "HeartbeatTimeout<" + getTimeoutId() + ">";
        }
    }

    private UUID scheduleSampleReqTimeout(Identifier eventId) {
        ScheduleTimeout spt = new ScheduleTimeout(msgTimeout);
        SampleRequestTimeout sc = new SampleRequestTimeout(spt, eventId);
        spt.setTimeoutEvent(sc);
        trigger(spt, timerPort);
        return sc.getTimeoutId();
    }

    private class SampleRequestTimeout extends Timeout {

        public final Identifier eventId;

        SampleRequestTimeout(ScheduleTimeout request, Identifier eventId) {
            super(request);
            this.eventId = eventId;
        }

        @Override
        public String toString() {
            return "SampleRequestTimeout<" + getTimeoutId() + ">";
        }
    }

    //**************************************************************************
    public static class Init extends se.sics.kompics.Init<BootstrapClientComp> {

        public final KAddress selfAdr;
        public final KAddress bootstrapServer;

        public Init(KAddress selfAdr, KAddress bootstrapServer) {
            this.selfAdr = selfAdr;
            this.bootstrapServer = bootstrapServer;
        }
    }
}

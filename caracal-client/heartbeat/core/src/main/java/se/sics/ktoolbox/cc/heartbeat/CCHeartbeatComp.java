/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.cc.heartbeat;

import com.google.common.io.BaseEncoding;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.caracaldb.Key;
import se.sics.caracaldb.KeyRange;
import se.sics.caracaldb.operations.PutRequest;
import se.sics.caracaldb.operations.RangeQuery;
import se.sics.caracaldb.store.ActionFactory;
import se.sics.caracaldb.store.Limit;
import se.sics.caracaldb.store.TFFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Direct;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.cc.bootstrap.CCBootstrapPort;
import se.sics.ktoolbox.cc.common.op.CCOpEvent;
import se.sics.ktoolbox.cc.bootstrap.msg.CCReady;
import se.sics.ktoolbox.cc.common.config.CCHeartbeatConfig;
import se.sics.ktoolbox.cc.common.op.CCOpManager;
import se.sics.ktoolbox.cc.common.op.CCOperation;
import se.sics.ktoolbox.cc.common.op.CCSimpleReady;
import se.sics.ktoolbox.cc.heartbeat.msg.CCHeartbeat;
import se.sics.ktoolbox.cc.heartbeat.msg.CCOverlaySample;
import se.sics.ktoolbox.cc.heartbeat.op.CCHeartbeatOp;
import se.sics.ktoolbox.cc.heartbeat.op.CCOverlaySampleOp;
import se.sics.ktoolbox.cc.heartbeat.util.CCKeyFactory;
import se.sics.ktoolbox.cc.heartbeat.util.CCValueFactory;
import se.sics.p2ptoolbox.util.config.SystemConfig;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CCHeartbeatComp extends ComponentDefinition implements CCOpManager {

    private static final Logger LOG = LoggerFactory.getLogger(CCHeartbeatComp.class);

    //There is no need to set timers on caracal requests - it should always answer and it should have a timer of its own
    Positive<CCBootstrapPort> caracal = requires(CCBootstrapPort.class);
    Positive<Timer> timer = requires(Timer.class);
    Negative<CCHeartbeatPort> provided = provides(CCHeartbeatPort.class);

    private final SystemConfig systemConfig;
    private final CCHeartbeatConfig heartbeatConfig;
    private final String logPrefix;
    private final Random rand;

    private byte[] schemaPrefix;
    private Set<ByteBuffer> heartbeats;
    private final Map<UUID, CCOperation> activeOps;
    private final Set<UUID> opsToRemove;
    private UUID sanityCheckTId;
    private UUID heartbeatTId;

    public CCHeartbeatComp(CCHeartbeatInit init) {
        this.systemConfig = init.systemConfig;
        this.heartbeatConfig = init.heartbeatConfig;
        this.logPrefix = systemConfig.self.getBase().toString();
        this.rand = new Random(systemConfig.seed);
        this.heartbeats = new HashSet<ByteBuffer>();
        this.activeOps = new HashMap<UUID, CCOperation>();
        this.opsToRemove = new HashSet<UUID>();

        LOG.info("{} initiating...", logPrefix);

        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleReady, caracal);
        subscribe(handleSanityCheck, timer);
        subscribe(handleHeartbeat, timer);
        subscribe(handleHeartbeatStart, provided);
        subscribe(handleHeartbeatStop, provided);
        subscribe(handleOverlaySampleRequest, provided);
        subscribe(handleCCOpResponse, caracal);
        subscribe(handleCCOpTimeout, caracal);
    }
    //**************************************************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{} waiting for ready", logPrefix);
        }
    };
    Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            LOG.info("{} stopping...", logPrefix);
            cancelHeartbeat();
            cancelSanityCheck();
        }
    };
    Handler handleReady = new Handler<CCReady>() {
        @Override
        public void handle(CCReady event) {
            LOG.info("{} received ready and schema prefix", logPrefix);
            if (schemaPrefix == null) {
                LOG.info("{} starting...", logPrefix);
                scheduleHeartbeat();
                scheduleSanityCheck();
                trigger(new CCSimpleReady(), provided);
            }
            schemaPrefix = event.caracalSchemaData.getId(heartbeatConfig.heartbeatSchemaName());
            if (schemaPrefix == null) {
                LOG.error("{} schema undefined - make sure carcal has the schema:{} setup", logPrefix, heartbeatConfig.heartbeatSchemaName());
                throw new RuntimeException("schema undefined - make sure you ran the SchemaSetup");
            }
        }
    };
    Handler handleSanityCheck = new Handler<SanityCheckTimeout>() {
        @Override
        public void handle(SanityCheckTimeout event) {
            LOG.trace("{} event", logPrefix);
            cleanOps();
            Set<String> stringHeartbeats = new HashSet<String>();
            for(ByteBuffer bb : heartbeats) {
                stringHeartbeats.add(BaseEncoding.base16().encode(bb.array()));
            }
            LOG.info("{} memory usage - activeOps:{}, heartbeats:{}",
                    new Object[]{logPrefix, activeOps.size(), stringHeartbeats});
        }
    };

    private void cleanOps() {
        for (UUID opId : opsToRemove) {
            activeOps.remove(opId);
        }
    }
    //**************************************************************************
    Handler handleHeartbeatStart = new Handler<CCHeartbeat.Start>() {
        @Override
        public void handle(CCHeartbeat.Start start) {
            LOG.info("{} updating heartbeat:{}", 
                    new Object[]{logPrefix, BaseEncoding.base16().encode(start.overlayId)});
            if (start.overlayId.length > 127) {
                LOG.error("{} overlay identifier is too long - try to stick to 127 bytes", logPrefix);
                throw new RuntimeException("overlay identifier is too long - try to stick to 127 bytes");
            }
            ByteBuffer bKey = ByteBuffer.wrap(start.overlayId);
            heartbeats.add(bKey);
        }
    };

    Handler handleHeartbeatStop = new Handler<CCHeartbeat.Stop>() {
        @Override
        public void handle(CCHeartbeat.Stop stop) {
            LOG.info("{} stopping heartbeat:{}", 
                    new Object[]{logPrefix, BaseEncoding.base16().encode(stop.overlayId)});
            ByteBuffer bKey = ByteBuffer.wrap(stop.overlayId);
            heartbeats.remove(bKey);
        }
    };

    Handler handleHeartbeat = new Handler<HeartbeatTimeout>() {
        @Override
        public void handle(HeartbeatTimeout event) {
            LOG.debug("{} heartbeat", logPrefix);
            byte[] value = CCValueFactory.getHeartbeatValue(systemConfig.self);
            for (ByteBuffer overlay : heartbeats) {
                Key heartbeatKey = CCKeyFactory.getHeartbeatKey(schemaPrefix, overlay.array(), rand.nextInt(heartbeatConfig.heartbeatSize()));
                PutRequest put = new PutRequest(UUID.randomUUID(), heartbeatKey, value);
                LOG.debug("{} sending heartbeat for:{}", 
                        new Object[]{logPrefix, BaseEncoding.base16().encode(overlay.array())});
                CCOperation op = new CCHeartbeatOp(UUID.randomUUID(), CCHeartbeatComp.this, put);
                activeOps.put(op.getId(), op);
                op.start();
            }
        }
    };
    //**************************************************************************
    Handler handleOverlaySampleRequest = new Handler<CCOverlaySample.Request>() {
        @Override
        public void handle(CCOverlaySample.Request event) {
            LOG.debug("{} received sample request for:{}", 
                    new Object[]{logPrefix, BaseEncoding.base16().encode(event.overlayId)});
            KeyRange overlaySampleRange = CCKeyFactory.getHeartbeatRange(schemaPrefix, event.overlayId);
            RangeQuery.Request range = new RangeQuery.Request(UUID.randomUUID(), overlaySampleRange, Limit.toItems(heartbeatConfig.heartbeatSize()), TFFactory.noTF(), ActionFactory.noop(), RangeQuery.Type.SEQUENTIAL);
            CCOperation op = new CCOverlaySampleOp(UUID.randomUUID(), systemConfig.self, CCHeartbeatComp.this, event, range);
            activeOps.put(op.getId(), op);
            op.start();
        }
    };

    Handler handleCCOpResponse = new Handler<CCOpEvent.Response>() {
        @Override
        public void handle(CCOpEvent.Response event) {
            LOG.trace("{} received:{}", new Object[]{logPrefix, event.opResp});
            for (CCOperation op : activeOps.values()) {
                if (op.ownResp(event.opResp.id)) {
                    op.handle(event.opResp);
                    return;
                }
            }
            LOG.warn("{} unexpected response:{}", logPrefix, event);
        }
    };

    Handler handleCCOpTimeout = new Handler<CCOpEvent.Timeout>() {
        @Override
        public void handle(CCOpEvent.Timeout event) {
            LOG.debug("{} op timed out:{}", logPrefix, event.opReq);
            for (CCOperation op : activeOps.values()) {
                if (op.ownResp(event.opReq.id)) {
                    op.fail();
                    return;
                }
            }
            LOG.warn("{} unexpected response:{}", logPrefix, event);
        }
    };

    private void scheduleHeartbeat() {
        if (heartbeatTId != null) {
            LOG.warn("{} double starting heartbeat", logPrefix);
            return;
        }
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(heartbeatConfig.heartbeatPeriod(), heartbeatConfig.heartbeatPeriod());
        HeartbeatTimeout sc = new HeartbeatTimeout(spt);
        spt.setTimeoutEvent(sc);
        heartbeatTId = sc.getTimeoutId();
        trigger(spt, timer);
    }

    private void cancelHeartbeat() {
        if (heartbeatTId == null) {
            LOG.warn("{} double stopping sanityCheck", logPrefix);
            return;
        }
        CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(heartbeatTId);
        heartbeatTId = null;
        trigger(cpt, timer);
    }

    private void scheduleSanityCheck() {
        if (sanityCheckTId != null) {
            LOG.warn("{} double starting sanityCheck", logPrefix);
            return;
        }
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(heartbeatConfig.sanityCheckPeriod(), heartbeatConfig.sanityCheckPeriod());
        SanityCheckTimeout sc = new SanityCheckTimeout(spt);
        spt.setTimeoutEvent(sc);
        sanityCheckTId = sc.getTimeoutId();
        trigger(spt, timer);
    }

    private void cancelSanityCheck() {
        if (sanityCheckTId == null) {
            LOG.warn("{} double stopping sanityCheck", logPrefix);
            return;
        }
        CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(sanityCheckTId);
        sanityCheckTId = null;
        trigger(cpt, timer);
    }

    //****************CCOpManager***********************************************
    @Override
    public void completed(UUID opId, KompicsEvent resp) {
        CCOperation op = activeOps.get(opId);
        opsToRemove.add(opId);
        LOG.debug("{} completed:{}", logPrefix, op);
        if (resp == null) {
            //heartbeat completed; do nothing
            return;
        }
        LOG.warn("{} did not expect this resp - operation:{} logic bug", logPrefix, op);
    }

    @Override
    public void completed(UUID opId, Direct.Request req, Direct.Response resp) {
        CCOperation op = activeOps.get(opId);
        opsToRemove.add(opId);
        LOG.debug("{} completed:{}", logPrefix, op);
        answer(req, resp);
    }

    @Override
    public void send(CCOpEvent.Request req) {
        LOG.trace("{} send:{}", logPrefix, req);
        trigger(req, caracal);
    }
    //**************************************************************************

    public static class CCHeartbeatInit extends Init<CCHeartbeatComp> {

        public final SystemConfig systemConfig;
        public final CCHeartbeatConfig heartbeatConfig;

        public CCHeartbeatInit(SystemConfig systemConfig, CCHeartbeatConfig hConfig) {
            this.systemConfig = systemConfig;
            this.heartbeatConfig = hConfig;
        }
    }

    public class HeartbeatTimeout extends Timeout {

        public HeartbeatTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }

        @Override
        public String toString() {
            return "HEARTBEAT_TIMEOUT";
        }
    }

    public class SanityCheckTimeout extends Timeout {

        public SanityCheckTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }

        @Override
        public String toString() {
            return "SANITYCHECK_TIMEOUT";
        }
    }
}

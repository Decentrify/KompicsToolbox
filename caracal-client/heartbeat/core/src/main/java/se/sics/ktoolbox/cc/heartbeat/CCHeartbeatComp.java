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
import se.sics.kompics.ClassMatchedHandler;
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
import se.sics.ktoolbox.cc.bootstrap.CCOperationPort;
import se.sics.ktoolbox.cc.bootstrap.event.status.CCBootstrapDisconnected;
import se.sics.ktoolbox.cc.bootstrap.event.status.CCBootstrapReady;
import se.sics.ktoolbox.cc.heartbeat.event.CCHeartbeat;
import se.sics.ktoolbox.cc.heartbeat.event.CCOverlaySample;
import se.sics.ktoolbox.cc.heartbeat.event.status.CCHeartbeatReady;
import se.sics.ktoolbox.cc.heartbeat.op.CCHeartbeatOp;
import se.sics.ktoolbox.cc.heartbeat.op.CCOverlaySampleOp;
import se.sics.ktoolbox.cc.heartbeat.util.CCKeyFactory;
import se.sics.ktoolbox.cc.heartbeat.util.CCValueFactory;
import se.sics.ktoolbox.cc.op.CCOpManager;
import se.sics.ktoolbox.cc.op.CCOperation;
import se.sics.ktoolbox.cc.operation.event.CCOpRequest;
import se.sics.ktoolbox.cc.operation.event.CCOpResponse;
import se.sics.ktoolbox.cc.operation.event.CCOpTimeout;
import se.sics.p2ptoolbox.util.config.KConfigCore;
import se.sics.p2ptoolbox.util.config.impl.SystemKCWrapper;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.status.Status;
import se.sics.p2ptoolbox.util.status.StatusPort;
import se.sics.p2ptoolbox.util.update.SelfAddressUpdate;
import se.sics.p2ptoolbox.util.update.SelfAddressUpdatePort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CCHeartbeatComp extends ComponentDefinition implements CCOpManager {

    private static final Logger LOG = LoggerFactory.getLogger(CCHeartbeatComp.class);
    private final String logPrefix;

    //There is no need to set timers on caracal requests - it should always answer and it should have a timer of its own
    Positive<Timer> timer = requires(Timer.class);
    Positive<SelfAddressUpdatePort> addressUpdate = requires(SelfAddressUpdatePort.class);
    Positive<CCOperationPort> caracal = requires(CCOperationPort.class);
    Positive<StatusPort> otherStatus = requires(StatusPort.class);
    Negative<StatusPort> myStatus = provides(StatusPort.class);
    Negative<CCHeartbeatPort> provided = provides(CCHeartbeatPort.class);

    private final SystemKCWrapper systemConfig;
    private final CCHeartbeatKCWrapper heartbeatConfig;
    private DecoratedAddress publicAdr;
    private final Random rand;

    private byte[] schemaPrefix;
    private final Set<ByteBuffer> heartbeats = new HashSet<>();
    private final Map<UUID, CCOperation> activeOps = new HashMap<>();
    private final Set<UUID> opsToRemove = new HashSet<>();
    private UUID sanityCheckTId;
    private UUID heartbeatTId;

    public CCHeartbeatComp(CCHeartbeatInit init) {
        this.systemConfig = new SystemKCWrapper(init.configCore);
        this.heartbeatConfig = new CCHeartbeatKCWrapper(init.configCore);
        this.publicAdr = init.publicAdr;
        this.logPrefix = "<nid:" + systemConfig.id + "> ";
        this.rand = new Random(systemConfig.seed);

        LOG.info("{}initiating...", logPrefix);

        subscribe(handleStart, control);
        subscribe(handleCCBootstrapReady, otherStatus);
        subscribe(handleCCBootstrapDisconnected, otherStatus);
        subscribe(handleSanityCheck, timer);
        subscribe(handleHeartbeat, timer);
        subscribe(handleHeartbeatStart, provided);
        subscribe(handleHeartbeatStop, provided);
        subscribe(handleOverlaySampleRequest, provided);
        subscribe(handleCCOpResponse, caracal);
        subscribe(handleCCOpTimeout, caracal);
        subscribe(handleSelfAddressUpdate, addressUpdate);
    }
    //**************************************************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{} waiting for ready", logPrefix);
        }
    };
    ClassMatchedHandler handleCCBootstrapReady
            = new ClassMatchedHandler<CCBootstrapReady, Status.Internal<CCBootstrapReady>>() {

                @Override
                public void handle(CCBootstrapReady status, Status.Internal<CCBootstrapReady> container) {
                    LOG.info("{}bootstrap ready", logPrefix);
                    if (schemaPrefix == null) {
                        LOG.info("{}starting...", logPrefix);
                        scheduleHeartbeat();
                        scheduleSanityCheck();
                        trigger(new Status.Internal(new CCHeartbeatReady()), myStatus);
                    }
                    schemaPrefix = status.caracalSchemaData.getId(heartbeatConfig.heartbeatSchema);
                    if (schemaPrefix == null) {
                        LOG.error("{}schema undefined - make sure carcal has the schema:{} setup", logPrefix, heartbeatConfig.heartbeatSchema);
                        throw new RuntimeException("schema undefined - make sure you ran the SchemaSetup");
                    }
                }
            };
    ClassMatchedHandler handleCCBootstrapDisconnected
            = new ClassMatchedHandler<CCBootstrapDisconnected, Status.Internal<CCBootstrapDisconnected>>() {

                @Override
                public void handle(CCBootstrapDisconnected status, Status.Internal<CCBootstrapDisconnected> container) {
                    LOG.warn("{}bootstrap disconnected from caracal", logPrefix);
                }
            };
    Handler handleSanityCheck = new Handler<PeriodicStateCheck>() {
        @Override
        public void handle(PeriodicStateCheck event) {
            cleanOps();
            Set<String> stringHeartbeats = new HashSet<String>();
            for (ByteBuffer bb : heartbeats) {
                stringHeartbeats.add(BaseEncoding.base16().encode(bb.array()));
            }
            LOG.info("{}activeOps:{}, heartbeats:{}",
                    new Object[]{logPrefix, activeOps.size(), stringHeartbeats});
        }
    };

    private void cleanOps() {
        for (UUID opId : opsToRemove) {
            activeOps.remove(opId);
        }
    }

    Handler handleSelfAddressUpdate = new Handler<SelfAddressUpdate>() {
        @Override
        public void handle(SelfAddressUpdate update) {
            LOG.info("{}update self address:{}", logPrefix, update.self);
            publicAdr = update.self.changeBase(publicAdr.getBase());
        }
    };
    //**************************************************************************
    Handler handleHeartbeatStart = new Handler<CCHeartbeat.Start>() {
        @Override
        public void handle(CCHeartbeat.Start start) {
            LOG.debug("{}updating heartbeat:{}",
                    new Object[]{logPrefix, BaseEncoding.base16().encode(start.overlayId)});
            if (start.overlayId.length > 127) {
                LOG.error("{}overlay identifier is too long - try to stick to 127 bytes", logPrefix);
                throw new RuntimeException("overlay identifier is too long - try to stick to 127 bytes");
            }
            ByteBuffer bKey = ByteBuffer.wrap(start.overlayId);
            heartbeats.add(bKey);
        }
    };

    Handler handleHeartbeatStop = new Handler<CCHeartbeat.Stop>() {
        @Override
        public void handle(CCHeartbeat.Stop stop) {
            LOG.debug("{}stopping heartbeat:{}",
                    new Object[]{logPrefix, BaseEncoding.base16().encode(stop.overlayId)});
            ByteBuffer bKey = ByteBuffer.wrap(stop.overlayId);
            heartbeats.remove(bKey);
        }
    };

    Handler handleHeartbeat = new Handler<PeriodicHeartbeat>() {
        @Override
        public void handle(PeriodicHeartbeat event) {
            LOG.debug("{}periodic heartbeat", logPrefix);
            byte[] value = CCValueFactory.getHeartbeatValue(publicAdr);
            for (ByteBuffer overlay : heartbeats) {
                Key heartbeatKey = CCKeyFactory.getHeartbeatKey(schemaPrefix, overlay.array(), rand.nextInt(heartbeatConfig.heartbeatSpace));
                PutRequest put = new PutRequest(UUID.randomUUID(), heartbeatKey, value);
                LOG.trace("{}sending heartbeat for:{}",
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
            LOG.debug("{}received sample request for:{}",
                    new Object[]{logPrefix, BaseEncoding.base16().encode(event.overlayId)});
            KeyRange overlaySampleRange = CCKeyFactory.getHeartbeatRange(schemaPrefix, event.overlayId);
            RangeQuery.Request range = new RangeQuery.Request(UUID.randomUUID(), overlaySampleRange,
                    Limit.toItems(heartbeatConfig.heartbeatSpace), TFFactory.noTF(), ActionFactory.noop(), RangeQuery.Type.SEQUENTIAL);
            CCOperation op = new CCOverlaySampleOp(UUID.randomUUID(), publicAdr, CCHeartbeatComp.this, event, range);
            activeOps.put(op.getId(), op);
            op.start();
        }
    };

    Handler handleCCOpResponse = new Handler<CCOpResponse>() {
        @Override
        public void handle(CCOpResponse event) {
            LOG.trace("{}received:{}", new Object[]{logPrefix, event.opResp});
            for (CCOperation op : activeOps.values()) {
                if (op.ownResp(event.opResp.id)) {
                    op.handle(event.opResp);
                    return;
                }
            }
            LOG.warn("{}unexpected response:{}", logPrefix, event);
        }
    };

    Handler handleCCOpTimeout = new Handler<CCOpTimeout>() {
        @Override
        public void handle(CCOpTimeout event) {
            LOG.debug("{}op timed out:{}", logPrefix, event.opReq);
            for (CCOperation op : activeOps.values()) {
                if (op.ownResp(event.opReq.opReq.id)) {
                    op.fail();
                    return;
                }
            }
            LOG.warn("{}unexpected response:{}", logPrefix, event);
        }
    };

    //****************CCOpManager***********************************************
    //TODO Alex - when time allows refactor to make it easier to understand
    //heartbeat
    @Override
    public void completed(UUID opId, KompicsEvent resp) {
        CCOperation op = activeOps.get(opId);
        opsToRemove.add(opId);
        LOG.debug("{}completed:{}", logPrefix, op);
        if (resp == null) {
            //heartbeat completed; do nothing
            return;
        }
        LOG.warn("{}did not expect this resp - operation:{} logic bug", logPrefix, op);
    }

    //overlaySample
    @Override
    public void completed(UUID opId, Direct.Request req, Direct.Response resp) {
        CCOperation op = activeOps.get(opId);
        opsToRemove.add(opId);
        LOG.debug("{}completed:{}", logPrefix, op);
        answer(req, resp);
    }

    @Override
    public void send(CCOpRequest req) {
        LOG.trace("{}send:{}", logPrefix, req);
        trigger(req, caracal);
    }

    //**************************************************************************
    public static class CCHeartbeatInit extends Init<CCHeartbeatComp> {

        public final KConfigCore configCore;
        public final DecoratedAddress publicAdr;

        public CCHeartbeatInit(KConfigCore configCore, DecoratedAddress publicAdr) {
            this.configCore = configCore;
            this.publicAdr = publicAdr;
        }
    }

    private void scheduleHeartbeat() {
        if (heartbeatTId != null) {
            LOG.warn("{} double starting heartbeat", logPrefix);
            return;
        }
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(heartbeatConfig.heartbeatPeriod, heartbeatConfig.heartbeatPeriod);
        PeriodicHeartbeat sc = new PeriodicHeartbeat(spt);
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

    public class PeriodicHeartbeat extends Timeout {

        public PeriodicHeartbeat(SchedulePeriodicTimeout request) {
            super(request);
        }

        @Override
        public String toString() {
            return "HEARTBEAT_TIMEOUT";
        }
    }

    private void scheduleSanityCheck() {
        if (sanityCheckTId != null) {
            LOG.warn("{} double starting sanityCheck", logPrefix);
            return;
        }
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(heartbeatConfig.stateCheckPeriod, heartbeatConfig.stateCheckPeriod);
        PeriodicStateCheck sc = new PeriodicStateCheck(spt);
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

    public class PeriodicStateCheck extends Timeout {

        public PeriodicStateCheck(SchedulePeriodicTimeout request) {
            super(request);
        }

        @Override
        public String toString() {
            return "STATE_CHECK_TIMEOUT";
        }
    }
}

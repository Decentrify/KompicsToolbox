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
package se.sics.p2ptoolbox.caracalclient.heartbeat;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.caracaldb.Key.KeyBuilder;
import se.sics.caracaldb.operations.PutRequest;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Address;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.caracalclient.bootstrap.CCBootstrapPort;
import se.sics.p2ptoolbox.caracalclient.bootstrap.msg.CCReady;
import se.sics.p2ptoolbox.caracalclient.bootstrap.msg.CCSchema;
import se.sics.p2ptoolbox.caracalclient.heartbeat.msg.CCHeartbeat;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CCHeartbeatComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(CCHeartbeatComp.class);

    //There is no need to set timers on caracal requests - it should always answer and it should have a timer of its own
    Positive<CCBootstrapPort> caracal = requires(CCBootstrapPort.class);
    Positive<Timer> timer = requires(Timer.class);
    Negative<CCHeartbeatPort> provided = provides(CCHeartbeatPort.class);
    
    private final DecoratedAddress self;
    private final CCHeartbeatConfig ccHeartbeatConfig;
    private final String logPrefix;
    private final Random rand;
    
    private UUID heartbeatTId;
    private byte[] schemaPrefix;
    private Map<ByteBuffer, byte[]> heartbeatPiggyBack;
    
    public CCHeartbeatComp(CCHeartbeatInit init) {
        this.self = init.self;
        this.ccHeartbeatConfig = init.hConfig;
        this.logPrefix = self.toString();
        this.rand = new Random(init.seed);
        this.heartbeatPiggyBack = new HashMap<ByteBuffer, byte[]>();
        
        LOG.info("{} initiating...", logPrefix);
        
        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleReady, caracal);
        subscribe(handleSchemaResponse, caracal);
        subscribe(handleHeartbeatUpdate, provided);
        subscribe(handleHeartbeatStop, provided);
        subscribe(handleHeartbeat, timer);
    }
    //**************************************************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{} received start - waiting for ready", logPrefix);
        }
    };
    Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            LOG.info("{} stopping...", logPrefix);
        }
    };
    Handler handleReady = new Handler<CCReady>() {
        @Override
        public void handle(CCReady event) {
            LOG.info("{} received ready - getting caracal schema prefix", logPrefix);
            CCSchema.Request schemaRequest = new CCSchema.Request(ccHeartbeatConfig.schemaName);
            trigger(schemaRequest, caracal);
        }
    };
    Handler handleSchemaResponse = new Handler<CCSchema.Response>() {
        @Override
        public void handle(CCSchema.Response resp) {
            LOG.info("{} received caracal schema prefix - starting...");
            if(resp.name.equals(ccHeartbeatConfig.schemaName)) {
                throw new RuntimeException("unexpected schema response");
            }
            schemaPrefix = resp.id;
            scheduleHeartbeatCheck();
        }
    };
    //**************************************************************************
    Handler handleHeartbeatUpdate = new Handler<CCHeartbeat.Update>() {
        @Override
        public void handle(CCHeartbeat.Update update) {
            LOG.info("{} updating piggyback information for:{}", logPrefix, BaseEncoding.base16().encode(update.heartbeatId));
            ByteBuffer bb = ByteBuffer.wrap(update.heartbeatId);
            heartbeatPiggyBack.put(bb, update.heartbeatPiggyBack);
        }
    };
    
    Handler handleHeartbeatStop = new Handler<CCHeartbeat.Stop>() {
        @Override
        public void handle(CCHeartbeat.Stop update) {
            LOG.info("{} stopping heartbeat:{}", logPrefix,  BaseEncoding.base16().encode(update.heartbeatId));
            ByteBuffer bb = ByteBuffer.wrap(update.heartbeatId);
            heartbeatPiggyBack.remove(bb);
        }
    };
    
    Handler handleHeartbeat = new Handler<HeartbeatTimeout>() {

        @Override
        public void handle(HeartbeatTimeout event) {
            LOG.info("{} heartbeat", logPrefix);
            for(Map.Entry<ByteBuffer, byte[]> e: heartbeatPiggyBack.entrySet()) {
                KeyBuilder kb = new KeyBuilder(schemaPrefix);
                kb.append(e.getKey().array());
                kb.append(Ints.toByteArray(rand.nextInt(ccHeartbeatConfig.heartbeatSize)));
            }
        }
        
    };
    
    private void scheduleHeartbeatCheck() {
        if (heartbeatTId != null) {
            LOG.warn("{} double starting heartbeat", logPrefix);
            return;
        }
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(ccHeartbeatConfig.heartbeatPeriod, ccHeartbeatConfig.heartbeatPeriod);
        HeartbeatTimeout sc = new HeartbeatTimeout(spt);
        spt.setTimeoutEvent(sc);
        heartbeatTId = sc.getTimeoutId();
        trigger(spt, timer);
    }

    private void cancelSanityCheck() {
        if (heartbeatTId == null) {
            LOG.warn("{} double stopping sanityCheck", logPrefix);
            return;
        }
        CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(heartbeatTId);
        heartbeatTId = null;
        trigger(cpt, timer);
    }

    public static class CCHeartbeatInit extends Init<CCHeartbeatComp> {
        public final DecoratedAddress self;
        public final CCHeartbeatConfig hConfig;
        public final long seed;
        
        public CCHeartbeatInit(DecoratedAddress self, CCHeartbeatConfig hConfig, long seed) {
            this.self = self;
            this.hConfig = hConfig;
            this.seed = seed;
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
    
    
}

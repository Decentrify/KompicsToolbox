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
package se.sics.p2ptoolbox.caracalclient.schemasetup;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.caracaldb.Key;
import se.sics.caracaldb.global.ForwardMessage;
import se.sics.caracaldb.global.Sample;
import se.sics.caracaldb.global.SampleRequest;
import se.sics.caracaldb.global.Schema;
import se.sics.caracaldb.global.SchemaData;
import se.sics.caracaldb.operations.CaracalMsg;
import se.sics.caracaldb.operations.GetRequest;
import se.sics.caracaldb.operations.GetResponse;
import se.sics.caracaldb.operations.ResponseCode;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Kompics;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.caracalclient.bootstrap.CaracalClientConfig;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CCSchemaSetupComp extends ComponentDefinition {

    private final static Logger log = LoggerFactory.getLogger(CCSchemaSetupComp.class);

    Positive<Network> network = requires(Network.class);
    Positive<Timer> timer = requires(Timer.class);

    private final CaracalClientConfig ccConfig;
    private final CCSchemaSetup schemaSetup;
    private final String logPrefix;

    private Map<UUID, UUID> activeTimeouts; //<messageId, timeoutId>
    private SchemaData schemaData;

    private UUID schemaRecheckTimeout;

    public CCSchemaSetupComp(CCSchemaSetupInit init) {
        this.ccConfig = init.ccConfig;
        this.schemaSetup = init.schemaSetup;
        this.logPrefix = "";
        this.activeTimeouts = new HashMap<UUID, UUID>();

        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleSample, network);
        subscribe(handleSchemaRecheck, timer);
        subscribe(handleCaracalTimeout, timer);
        subscribe(handleCaracalResponse, network);
    }

    //**************************************************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            log.info("{} starting...", logPrefix);
            SampleRequest req = new SampleRequest(ccConfig.self, ccConfig.caracalNodes.get(0), 0, true, false, -1);
            log.info("{} sending sample request to:{}", logPrefix, ccConfig.caracalNodes.get(0));
            trigger(req, network);
            schemaData = null;
        }
    };
    Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            log.info("{} stopping...", logPrefix);
        }
    };
    //**************************************************************************
    Handler handleSchemaRecheck = new Handler<CaracalTimeout>() {
        @Override
        public void handle(CaracalTimeout event) {
            log.trace("{} timeout:{}", logPrefix, event);
            if (schemaData == null) {
                throw new RuntimeException("sample request failed - caracal timed out");
            }
            SampleRequest req = new SampleRequest(ccConfig.self, ccConfig.caracalNodes.get(0), 0, true, false, -1);
            log.info("{} sending sample request to:{}", logPrefix, ccConfig.caracalNodes.get(0));
            trigger(req, network);

            schemaData = null;
            schemaRecheckTimeout = null;
        }
    };
    Handler handleSample = new Handler<Sample>() {
        @Override
        public void handle(Sample msg) {
            log.trace("{} received:{}", logPrefix, msg);

            schemaData = SchemaData.deserialise(msg.schemaData);
            for (Map.Entry<String, Map<String, String>> schema : schemaSetup.schemas.entrySet()) {
                if (schemaData.getId(schema.getKey()) == null) {
                    Schema.CreateReq createReq = new Schema.CreateReq(ccConfig.self, ccConfig.caracalNodes.get(0), schema.getKey(), ImmutableMap.copyOf(schema.getValue()));
                    log.debug("{} sending schema create:{}", logPrefix, createReq);
                    trigger(createReq, network);

                    if (schemaRecheckTimeout == null) {
                        scheduleSchemaRecheckTimeout();
                    }
                }
            }

            if (schemaRecheckTimeout != null) {
                checkSchemasReady();
            }
        }
    };

    private void checkSchemasReady() {
        for (String schemaName : schemaSetup.schemas.keySet()) {
            byte[] schemaId = schemaData.getId(schemaName);
            Key target = new Key(schemaId);
            GetRequest get = new GetRequest(UUID.randomUUID(), target);
            CaracalMsg msg = new CaracalMsg(ccConfig.self, ccConfig.caracalNodes.get(0), get);
            ForwardMessage fmsg = new ForwardMessage(ccConfig.self, ccConfig.caracalNodes.get(0), target, msg);
            scheduleCaracalTimeout(fmsg.getId());
            log.debug("{} sending:{}", logPrefix, get);
            trigger(fmsg, network);
        }
    }

    Handler handleCaracalTimeout = new Handler<CaracalTimeout>() {
        @Override
        public void handle(CaracalTimeout event) {
            log.trace("{} timeout:{}", logPrefix, event);
            if (!activeTimeouts.containsKey(event.messageId)) {
                log.debug("{} late timeout:{}", logPrefix, event);
                return;
            }
            throw new RuntimeException("caracal timed out - try again with a different caracal node");
        }
    };

    Handler handleCaracalResponse = new Handler<CaracalMsg>() {
        @Override
        public void handle(CaracalMsg msg) {
            log.trace("{} received:{}", logPrefix, msg);

            if (msg.op instanceof GetResponse) {
                GetResponse getResp = (GetResponse) msg.op;
                if (getResp.code.equals(ResponseCode.SUCCESS)) {
                    cancelCaracalTimeout(msg.getId());
                    log.info("{} schema:{} is ready for use:", schemaData.getName(getResp.key.getArray()));
                    if (activeTimeouts.isEmpty()) {
                        log.info("{} caracal is ready to use - exiting", logPrefix);
                        Kompics.shutdown();
                    }
                }
            } else {
                log.warn("{} weird, unexpected caracal response:{}", logPrefix, msg.op);
            }
        }
    };

    private void scheduleCaracalTimeout(UUID messageId) {
        ScheduleTimeout st = new ScheduleTimeout(2000);
        CaracalTimeout sc = new CaracalTimeout(st, messageId);
        st.setTimeoutEvent(sc);
        trigger(st, timer);
        activeTimeouts.put(messageId, sc.getTimeoutId());
    }

    private void cancelCaracalTimeout(UUID messageId) {
        CancelTimeout cpt = new CancelTimeout(activeTimeouts.remove(messageId));
        trigger(cpt, timer);
    }

    private void scheduleSchemaRecheckTimeout() {
        ScheduleTimeout st = new ScheduleTimeout(5000);
        SchemaRecheckTimeout sc = new SchemaRecheckTimeout(st);
        st.setTimeoutEvent(sc);
        trigger(st, timer);
        schemaRecheckTimeout = sc.getTimeoutId();
    }

    public static class CCSchemaSetupInit extends Init<CCSchemaSetupComp> {

        public final CaracalClientConfig ccConfig;
        public final CCSchemaSetup schemaSetup;

        public CCSchemaSetupInit(CaracalClientConfig ccConfig, CCSchemaSetup schemaSetup) {
            this.ccConfig = ccConfig;
            this.schemaSetup = schemaSetup;
        }
    }

    private class CaracalTimeout extends Timeout {

        public final UUID messageId;

        public CaracalTimeout(ScheduleTimeout request, UUID messageId) {
            super(request);
            this.messageId = messageId;
        }

        @Override
        public String toString() {
            return "CARACAL_TIMEOUT";
        }
    }

    private class SchemaRecheckTimeout extends Timeout {

        public SchemaRecheckTimeout(ScheduleTimeout request) {
            super(request);
        }

        @Override
        public String toString() {
            return "SCHEMA_RECHECK_TIMEOUT";
        }
    }
}

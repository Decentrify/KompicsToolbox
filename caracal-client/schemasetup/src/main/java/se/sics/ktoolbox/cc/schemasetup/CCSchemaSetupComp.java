///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * GVoD is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package se.sics.ktoolbox.cc.schemasetup;
//
//import com.google.common.collect.ImmutableMap;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import se.sics.caracaldb.Address;
//import se.sics.caracaldb.Key;
//import se.sics.caracaldb.global.ForwardMessage;
//import se.sics.caracaldb.global.Sample;
//import se.sics.caracaldb.global.SampleRequest;
//import se.sics.caracaldb.global.Schema;
//import se.sics.caracaldb.global.SchemaData;
//import se.sics.caracaldb.operations.CaracalMsg;
//import se.sics.caracaldb.operations.GetRequest;
//import se.sics.caracaldb.operations.GetResponse;
//import se.sics.caracaldb.operations.ResponseCode;
//import se.sics.kompics.ComponentDefinition;
//import se.sics.kompics.Handler;
//import se.sics.kompics.Init;
//import se.sics.kompics.Kompics;
//import se.sics.kompics.Positive;
//import se.sics.kompics.Start;
//import se.sics.kompics.Stop;
//import se.sics.kompics.network.Network;
//import se.sics.kompics.timer.CancelTimeout;
//import se.sics.kompics.timer.ScheduleTimeout;
//import se.sics.kompics.timer.Timeout;
//import se.sics.kompics.timer.Timer;
//import se.sics.ktoolbox.cc.bootstrap.CCBootstrapPort;
//import se.sics.ktoolbox.cc.bootstrap.msg.CCGetNodes;
//import se.sics.ktoolbox.cc.bootstrap.msg.CCReady;
//import se.sics.ktoolbox.cc.bootstrap.msg.CCUpdate;
//import se.sics.ktoolbox.cc.common.op.CCOpEvent;
//import se.sics.p2ptoolbox.util.config.SystemConfig;
//
///**
// * @author Alex Ormenisan <aaor@kth.se>
// */
//public class CCSchemaSetupComp extends ComponentDefinition {
//
//    private final static Logger log = LoggerFactory.getLogger(CCSchemaSetupComp.class);
//
//    Positive<Network> network = requires(Network.class);
//    Positive<Timer> timer = requires(Timer.class);
//    Positive<CCBootstrapPort> ccBootstrap = requires(CCBootstrapPort.class);
//
//    private final CCSchemaSetup schemaSetup;
//    private final String logPrefix;
//    private final Address ccSelf;
//
//    private SchemaData schemaData;
//    private UUID schemaRecheckTimeout;
//
//    public CCSchemaSetupComp(CCSchemaSetupInit init) {
//        this.ccSelf = new se.sics.caracaldb.Address(init.systemConfig.self.getIp(), init.systemConfig.self.getPort(), null);
//        this.schemaSetup = init.schemaSetup;
//        this.logPrefix = ccSelf.toString();
//
//        subscribe(handleStart, control);
//        subscribe(handleStop, control);
//        subscribe(handleReady, ccBootstrap);
//        subscribe(handleGetNodes, ccBootstrap);
//        subscribe(handleOpTimeout, ccBootstrap);
//        subscribe(handleCaracalResponse, ccBootstrap);
//        subscribe(handleSchemaRecheck, timer);
//    }
//
//    //**************************************************************************
//    Handler handleStart = new Handler<Start>() {
//        @Override
//        public void handle(Start event) {
//            log.info("{} starting...", logPrefix);
//            schemaData = null;
//        }
//    };
//    Handler handleStop = new Handler<Stop>() {
//        @Override
//        public void handle(Stop event) {
//            log.info("{} stopping...", logPrefix);
//        }
//    };
//    //**************************************************************************
//    Handler handleReady = new Handler<CCReady>() {
//        @Override
//        public void handle(CCReady msg) {
//            log.trace("{} received:{}", logPrefix, msg);
//
//            schemaData = msg.caracalSchemaData;
//
//            if (!checkSchemas()) {
//                log.info("not all schemas created");
//                trigger(new CCGetNodes.Req(), ccBootstrap);
//            } else {
//                checkSchemasReady();
//            }
//        }
//    };
//    
//    Handler handleSchemaRecheck = new Handler<SchemaRecheckTimeout>() {
//        @Override
//        public void handle(SchemaRecheckTimeout e) {
//            log.trace("{} schema recheck");
//            trigger(new CCUpdate(), ccBootstrap);
//        }
//    };
//    
//    Handler handleGetNodes = new Handler<CCGetNodes.Resp>() {
//        @Override
//        public void handle(CCGetNodes.Resp resp) {
//            for (Map.Entry<String, Map<String, String>> schema : schemaSetup.schemas.entrySet()) {
//                if (schemaData.getId(schema.getKey()) == null) {
//                    Schema.CreateReq createReq = new Schema.CreateReq(ccSelf, resp.caracalNodes.get(0), schema.getKey(), ImmutableMap.copyOf(schema.getValue()));
//                    log.debug("{} sending schema create:{}", logPrefix, createReq);
//                    trigger(createReq, network);
//                }
//            }
//            scheduleSchemaRecheckTimeout();
//        }
//    };
//
//    Handler handleOpTimeout = new Handler<CCOpEvent.Timeout>() {
//        @Override
//        public void handle(CCOpEvent.Timeout event) {
//            log.trace("{} timeout:{}", logPrefix, event);
//            throw new RuntimeException("caracal timed out - try running schema setup again");
//        }
//    };
//
//    Handler handleCaracalResponse = new Handler<CaracalMsg>() {
//        @Override
//        public void handle(CaracalMsg msg) {
//            log.trace("{} received:{}", logPrefix, msg);
//
//            if (msg.op instanceof GetResponse) {
//                GetResponse getResp = (GetResponse) msg.op;
//                if (getResp.code.equals(ResponseCode.SUCCESS)) {
//                    cancelCaracalTimeout(msg.getId());
//                    log.info("{} schema:{} is ready for use:", schemaData.getName(getResp.key.getArray()));
//                    if (activeTimeouts.isEmpty()) {
//                        log.info("{} caracal is ready to use - exiting", logPrefix);
//                        Kompics.shutdown();
//                    }
//                }
//            } else {
//                log.warn("{} weird, unexpected caracal response:{}", logPrefix, msg.op);
//            }
//        }
//    };
//    
//    private boolean checkSchemas() {
//        for (Map.Entry<String, Map<String, String>> schema : schemaSetup.schemas.entrySet()) {
//            if (schemaData.getId(schema.getKey()) == null) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    private void checkSchemasReady() {
//        for (String schemaName : schemaSetup.schemas.keySet()) {
//            byte[] schemaId = schemaData.getId(schemaName);
//            Key target = new Key(schemaId);
//            GetRequest get = new GetRequest(UUID.randomUUID(), target);
//            log.debug("{} sending:{}", logPrefix, get);
//            trigger(get, ccBootstrap);
//        }
//    }
//
//    private void scheduleSchemaRecheckTimeout() {
//        ScheduleTimeout st = new ScheduleTimeout(5000);
//        SchemaRecheckTimeout sc = new SchemaRecheckTimeout(st);
//        st.setTimeoutEvent(sc);
//        trigger(st, timer);
//        schemaRecheckTimeout = sc.getTimeoutId();
//    }
//
//    public static class CCSchemaSetupInit extends Init<CCSchemaSetupComp> {
//        public final SystemConfig systemConfig;
//        public final CCSchemaSetup schemaSetup;
//
//        public CCSchemaSetupInit(SystemConfig systemConfig, CCSchemaSetup schemaSetup) {
//            this.systemConfig = systemConfig;
//            this.schemaSetup = schemaSetup;
//        }
//    }
//
//    private class SchemaRecheckTimeout extends Timeout {
//
//        public SchemaRecheckTimeout(ScheduleTimeout request) {
//            super(request);
//}
//
//        @Override
//        public String toString() {
//            return "SCHEMA_RECHECK_TIMEOUT";
//        }
//    }
//}

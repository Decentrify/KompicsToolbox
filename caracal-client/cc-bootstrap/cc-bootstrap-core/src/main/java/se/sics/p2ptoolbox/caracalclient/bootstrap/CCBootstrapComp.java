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
package se.sics.p2ptoolbox.caracalclient.bootstrap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.javatuples.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.caracaldb.global.Sample;
import se.sics.caracaldb.global.SampleRequest;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.caracaldb.Address;
import se.sics.caracaldb.global.ForwardMessage;
import se.sics.caracaldb.global.SchemaData;
import se.sics.caracaldb.operations.CaracalMsg;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.caracalclient.bootstrap.msg.CCDisconnected;
import se.sics.p2ptoolbox.caracalclient.bootstrap.msg.CCReady;
import se.sics.p2ptoolbox.caracalclient.common.op.CCOpEvent;
import se.sics.p2ptoolbox.util.config.SystemConfig;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CCBootstrapComp extends ComponentDefinition {

    private final static Logger log = LoggerFactory.getLogger(CCBootstrapComp.class);

    Negative<CCBootstrapPort> provided = provides(CCBootstrapPort.class);
    Positive<Network> network = requires(Network.class);
    Positive<Timer> timer = requires(Timer.class);

    private final Random rand;
    private final SystemConfig systemConfig;
    private final CCBootstrapConfig ccBootstrapConfig;
    private final String logPrefix;
    private final Address ccSelf;

    private SchemaData schemas = null;
    private UUID sanityCheckTId = null;

    private final List<Address> activeNodes;
    private final List<Address> deadNodes;

    private Map<UUID, Triplet<CCOpEvent.Request, Address, UUID>> activeRequests; //<messageId, <request, caracalNode, timeoutId>>

    public CCBootstrapComp(CCBootstrapInit init) {
        this.rand = new Random(init.seed);
        this.systemConfig = init.systemConfig;
        this.ccSelf = new se.sics.caracaldb.Address(systemConfig.self.getIp(), systemConfig.self.getPort(), null);
        this.ccBootstrapConfig = init.ccBootstrapConfig;
        this.logPrefix = systemConfig.self.toString();
        if (init.caracalNodes.isEmpty()) {
            log.error("{} no bootstrap caracal nodes provided - cannot boot", logPrefix);
            throw new RuntimeException("no bootstrap caracal nodes provided - cannot boot");
        }
        this.activeNodes = new ArrayList<Address>();
        this.deadNodes = new ArrayList<Address>(init.caracalNodes);
        this.activeRequests = new HashMap<UUID, Triplet<CCOpEvent.Request, Address, UUID>>();
        log.info("{} initiating with bootstrap nodes:{}", logPrefix, activeNodes);

        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleSanityCheck, timer);
        subscribe(handleSample, network);
        subscribe(handleCCOpRequest, provided);
        subscribe(handleCCOpResponse, network);
        subscribe(handleCCOpTimeout, timer);
    }

    //**************************************************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            log.info("{} starting...", logPrefix);
            scheduleSanityCheck();
            SampleRequest req = new SampleRequest(ccSelf, randomCaracalNode(deadNodes), ccBootstrapConfig.sampleSize, true, false, 0);
            log.trace("{} sending:{}", logPrefix, req);
            trigger(req, network);
        }
    };
    Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            log.info("{} stopping...", logPrefix);
        }
    };
    //**************************************************************************

    Handler handleSanityCheck = new Handler<SanityCheckTimeout>() {
        @Override
        public void handle(SanityCheckTimeout event) {
            log.trace("{} event", logPrefix);

            log.info("{} memory usage - activeNodes:{} deadNodes:{} activeRequests:{}",
                    new Object[]{logPrefix, activeNodes.size(), deadNodes.size(), activeRequests.size()});

            if (activeNodes.isEmpty()) {
                log.warn("{} disconnected from caracal - possible network partition", logPrefix);
                SampleRequest req = new SampleRequest(ccSelf, randomCaracalNode(deadNodes), ccBootstrapConfig.sampleSize, false, false, 0);
                log.trace("{} sending:{}", logPrefix, req);
                trigger(req, network);
                return;
            }
            if (activeNodes.size() < ccBootstrapConfig.nodeListSize) {
                SampleRequest req = new SampleRequest(ccSelf, randomCaracalNode(activeNodes), ccBootstrapConfig.sampleSize, false, false, 0);
                log.trace("{} sending:{}", logPrefix, req);
                trigger(req, network);
            }
        }
    };

    Handler handleSample = new Handler<Sample>() {
        @Override
        public void handle(Sample msg) {
            log.trace("{} received:{}", logPrefix, msg);
            if (msg.schemaData != null) {
                log.trace("{} received schemas", logPrefix);
                schemas = SchemaData.deserialise(msg.schemaData);
            }
            for (Address node : msg.nodes) {
                if (activeNodes.isEmpty()) {
                    log.info("{} connected to caracal", logPrefix);
                    trigger(new CCReady(schemas), provided);
                }
                if (!activeNodes.contains(node)) {
                    activeNodes.add(node);
                }
            }
            log.info("{} caracal nodes:{}", logPrefix, activeNodes);
        }
    };

    //**************************************************************************
    Handler handleCCOpRequest = new Handler<CCOpEvent.Request>() {
        @Override
        public void handle(CCOpEvent.Request request) {
            log.trace("{} received:{}", logPrefix, request);
            if (activeNodes.isEmpty()) {
                log.info("{} disconnected - cannot serve requests");
                answer(request, new CCOpEvent.Timeout(request.opReq));
                return;
            }

            Address caracalNode = randomCaracalNode(activeNodes);
            CaracalMsg msg = new CaracalMsg(ccSelf, caracalNode, request.opReq);
            ForwardMessage fmsg = new ForwardMessage(ccSelf, caracalNode, request.forwardTo, msg);
            log.debug("{} sending:{}", logPrefix, fmsg);
            trigger(fmsg, network);
            UUID timeoutId = scheduleCaracalTimeout(fmsg.getId());
            activeRequests.put(fmsg.getId(), Triplet.with(request, caracalNode, timeoutId));
        }
    };

    Handler handleCCOpResponse = new Handler<CaracalMsg>() {
        @Override
        public void handle(CaracalMsg response) {
            log.trace("{} received:{}", logPrefix, response);
            Triplet<CCOpEvent.Request, Address, UUID> requestInfo = activeRequests.remove(response.getId());
            if (requestInfo == null) {
                log.debug("{} late message:{}", logPrefix, response.getId());
                return;
            }
            cancelCaracalTimeout(requestInfo.getValue2());
            log.debug("{} received response:{}", logPrefix, response.op);
            answer(requestInfo.getValue0(), new CCOpEvent.Response(response.op));
        }
    };

    Handler handleCCOpTimeout = new Handler<CaracalTimeout>() {
        @Override
        public void handle(CaracalTimeout timeout) {
            log.trace("{} timeout:{}", logPrefix, timeout);
            Triplet<CCOpEvent.Request, Address, UUID> requestInfo = activeRequests.remove(timeout.messageId);
            if (requestInfo == null) {
                log.debug("{} late timeout for message:{}", logPrefix, timeout.messageId);
                return;
            }
            activeNodes.remove(requestInfo.getValue1());
            if(activeNodes.isEmpty()) {
                log.warn("{} disconnected from caracal", logPrefix);
                trigger(new CCDisconnected(), provided);
            }
            deadNodes.add(requestInfo.getValue1());
            if(deadNodes.size() > ccBootstrapConfig.nodeListSize) {
                deadNodes.remove(0);
            }
            log.debug("{} timed out message:{} on caracal node:{}", 
                    new Object[]{logPrefix, timeout.messageId, requestInfo.getValue1()});
            answer(requestInfo.getValue0(), new CCOpEvent.Timeout(requestInfo.getValue0().opReq));
        }
    };
    //**************************************************************************

    private Address randomCaracalNode(List<Address> nodes) {
        return nodes.get(rand.nextInt(nodes.size()));
    }

    private void scheduleSanityCheck() {
        if (sanityCheckTId != null) {
            log.warn("{} double starting sanityChec", logPrefix);
            return;
        }
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(ccBootstrapConfig.sanityCheckTimeout, ccBootstrapConfig.sanityCheckTimeout);
        SanityCheckTimeout sc = new SanityCheckTimeout(spt);
        spt.setTimeoutEvent(sc);
        sanityCheckTId = sc.getTimeoutId();
        trigger(spt, timer);
    }

    private void cancelSanityCheck() {
        if (sanityCheckTId == null) {
            log.warn("{} double stopping sanityCheck", logPrefix);
            return;
        }
        CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(sanityCheckTId);
        sanityCheckTId = null;
        trigger(cpt, timer);
    }

    private UUID scheduleCaracalTimeout(UUID messageId) {
        ScheduleTimeout st = new ScheduleTimeout(ccBootstrapConfig.caracalTimeout);
        CaracalTimeout ct = new CaracalTimeout(st, messageId);
        st.setTimeoutEvent(ct);
        trigger(st, timer);
        return ct.getTimeoutId();
    }

    private void cancelCaracalTimeout(UUID timeoutId) {
        CancelTimeout cpt = new CancelTimeout(timeoutId);
        trigger(cpt, timer);
    }

    public static class CCBootstrapInit extends Init<CCBootstrapComp> {

        public final long seed;
        public final SystemConfig systemConfig;
        public final CCBootstrapConfig ccBootstrapConfig;
        public final List<Address> caracalNodes;

        public CCBootstrapInit(long seed, SystemConfig systemConfig, CCBootstrapConfig ccBootstrapConfig, List<Address> caracalNodes) {
            this.seed = seed;
            this.systemConfig = systemConfig;
            this.ccBootstrapConfig = ccBootstrapConfig;
            this.caracalNodes = caracalNodes;
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

    public class CaracalTimeout extends Timeout {

        public final UUID messageId;

        public CaracalTimeout(ScheduleTimeout request, UUID messageId) {
            super(request);
            this.messageId = messageId;
        }

        @Override
        public String toString() {
            return "CARACAL_TIMEOUT<" + messageId + ">";
        }
    }
}

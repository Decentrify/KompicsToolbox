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
package se.sics.ktoolbox.cc.bootstrap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.javatuples.Pair;
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
import se.sics.ktoolbox.cc.bootstrap.event.status.CCBootstrapDisconnected;
import se.sics.ktoolbox.cc.bootstrap.event.status.CCBootstrapReady;
import se.sics.ktoolbox.cc.operation.event.CCOpRequest;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicAddress;
import se.sics.ktoolbox.util.status.Status;
import se.sics.ktoolbox.util.status.StatusPort;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CCBootstrapComp extends ComponentDefinition {

    private final static Logger LOG = LoggerFactory.getLogger(CCBootstrapComp.class);
    private final String logPrefix;

    Negative<CCOperationPort> ccop = provides(CCOperationPort.class);
    Negative<StatusPort> status = provides(StatusPort.class);
    Positive<Network> network = requires(Network.class);
    Positive<Timer> timer = requires(Timer.class);

    private final SystemKCWrapper systemConfig;
    private final CCBootstrapKCWrapper bootstrapConfig;
    private final Address privateAdr;

    private final CaracalTracker caracalTracker;
    private final OperationTracker operationTracker;

    private UUID sanityCheckTId = null;

    public CCBootstrapComp(CCBootstrapInit init) {
        systemConfig = new SystemKCWrapper(config());
        bootstrapConfig = new CCBootstrapKCWrapper(config());
        privateAdr = init.privateAdr;
        caracalTracker = new CaracalTracker();
        operationTracker = new OperationTracker();
        logPrefix = "<nid:" + systemConfig.id + "> ";
        LOG.info("{}initiating...", logPrefix);

        subscribe(handleStart, control);
        subscribe(handlePeriodicStateCheck, timer);
        subscribe(caracalTracker.handleSample, network);
        subscribe(caracalTracker.handleSampleTimeout, timer);
        subscribe(operationTracker.handleCCOpTimeout, timer);
        subscribe(operationTracker.handleCCOpRequestDisconnected, ccop);
    }

    //**************************************************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{} starting...", logPrefix);
            schedulePeriodicStateCheck();
            caracalTracker.start();
        }
    };

    Handler handlePeriodicStateCheck = new Handler<SanityCheckTimeout>() {
        @Override
        public void handle(SanityCheckTimeout event) {
            caracalTracker.stateCheck();
            operationTracker.stateCheck();
        }
    };

    private void ready() {
        subscribe(operationTracker.handleCCOpResponse, network);
        unsubscribe(operationTracker.handleCCOpRequestDisconnected, ccop);
        subscribe(operationTracker.handleCCOpRequest, ccop);

        LOG.info("{}caracal ready", logPrefix);
        trigger(new Status.Internal(UUIDIdentifier.randomId(), new CCBootstrapReady(caracalTracker.schemas)), status);
    }

    private void disconnected() {
        subscribe(operationTracker.handleCCOpRequestDisconnected, ccop);
        unsubscribe(operationTracker.handleCCOpRequest, ccop);
        unsubscribe(operationTracker.handleCCOpResponse, network);

        LOG.info("{}caracal disconnected", logPrefix);
        trigger(new Status.Internal(UUIDIdentifier.randomId(), new CCBootstrapDisconnected()), status);
    }

    //**************************************************************************
    public class CaracalTracker {

        protected SchemaData schemas = null;
        protected boolean caracalConnected = false;

        private final Random rand;
        private final List<Address> activeNodes = new ArrayList<>();
        private final List<Address> deadNodes = new ArrayList<>();

        private UUID sampleTid = null;

        public CaracalTracker() {
            rand = new Random(systemConfig.seed);
            if (bootstrapConfig.caracalBootstrap.isEmpty()) {
                LOG.error("{}no bootstrap caracal nodes provided - cannot boot", logPrefix);
                throw new RuntimeException("no bootstrap caracal nodes provided - cannot boot");
            }
            deadNodes.addAll(bootstrapConfig.caracalBootstrap);
        }

        protected void start() {
            requestSample(true);
        }

        protected void stateCheck() {
            LOG.info("{}caracal connected:{}", logPrefix, caracalTracker.caracalConnected);
            LOG.info("{}activeNodes:{} deadNodes:{}",
                    new Object[]{logPrefix, activeNodes.size(), deadNodes.size()});

            if (sampleTid == null && activeNodes.size() < bootstrapConfig.bootstrapSize) {
                requestSample(schemas == null);
            }
        }

        private void requestSample(boolean schemaRequest) {
            Address caracalNode = null;
            if (!activeNodes.isEmpty()) {
                caracalNode = randomCaracalNode(activeNodes);
            }
            if (caracalNode == null) {
                caracalNode = randomCaracalNode(deadNodes);
            }
            //should always have at least one dead node since I will not start with 0 bootstraps;
            SampleRequest req = new SampleRequest(privateAdr, caracalNode, bootstrapConfig.bootstrapSize, schemaRequest, false, 0);
            LOG.trace("{}sending:{} to:{}", new Object[]{logPrefix, req, req.getHeader().getDestination()});
            trigger(req, network);
            scheduleCaracalSampleTimeout(caracalNode);
        }

        private void registerSample(Sample sample) {
            if (sample.schemaData != null) {
                schemas = SchemaData.deserialise(sample.schemaData);
                LOG.debug("{}received schemas:{}", logPrefix, schemas.schemas());
            }
            for (Address node : sample.nodes) {
                if (!activeNodes.contains(node)) {
                    activeNodes.add(node);
                }
            }
        }

        Handler handleSample = new Handler<Sample>() {
            @Override
            public void handle(Sample sample) {
                LOG.trace("{}received:{} from:{}", logPrefix, sample, sample.getHeader().getSource());
                registerSample(sample);
                if (sampleTid != null) {
                    cancelCaracalSampleTimeout();
                }
                if (!caracalConnected) {
                    if (schemas != null && !activeNodes.isEmpty()) {
                        caracalConnected = true;
                        ready();
                    } else {
                        requestSample(schemas == null);
                    }
                }
            }
        };

        Handler handleSampleTimeout = new Handler<CaracalSampleTimeout>() {
            @Override
            public void handle(CaracalSampleTimeout timeout) {
                LOG.trace("{}caracal sample timeout from:{}", logPrefix, timeout.caracalNode);
                if (sampleTid == null) {
                    //possible late timeout
                    return;
                }
                if (activeNodes.contains(timeout.caracalNode)) {
                    suspectedNode(timeout.caracalNode);
                }
                sampleTid = null;
                requestSample(schemas == null);
            }
        };

        public Address randomActiveNode() {
            return randomCaracalNode(activeNodes);
        }

        private Address randomCaracalNode(List<Address> nodes) {
            return nodes.get(rand.nextInt(nodes.size()));
        }

        public void suspectedNode(Address suspectNode) {
            if (!activeNodes.remove(suspectNode)) {
                return;
            }
            if (activeNodes.isEmpty()) {
                if (caracalConnected) {
                    caracalConnected = false;
                    disconnected();
                }
            }
            if (sampleTid == null) {
                requestSample(schemas == null);
            }
            if (!deadNodes.contains(suspectNode)) {
                deadNodes.add(suspectNode);
                if (deadNodes.size() > bootstrapConfig.bootstrapSize) {
                    Address randomNode = randomCaracalNode(deadNodes);
                    deadNodes.remove(randomNode);
                }
            }
        }

        private void scheduleCaracalSampleTimeout(Address caracalNode) {
            if (sampleTid != null) {
                throw new RuntimeException("CaracalConnect logic error");
            }
            ScheduleTimeout st = new ScheduleTimeout(bootstrapConfig.caracalRtt);
            CaracalSampleTimeout ct = new CaracalSampleTimeout(st, caracalNode);
            st.setTimeoutEvent(ct);
            trigger(st, timer);
            sampleTid = ct.getTimeoutId();
        }

        private void cancelCaracalSampleTimeout() {
            if (sampleTid == null) {
                throw new RuntimeException("CaracalConnect logic error");
            }
            CancelTimeout cpt = new CancelTimeout(sampleTid);
            trigger(cpt, timer);
            sampleTid = null;
        }
    }

    private class OperationTracker {

        //<messageId, <request, timeoutId>>
        private Map<UUID, Pair<CCOpRequest, UUID>> activeRequests = new HashMap<>();
        //<timeoutId>
        private Set<UUID> activeTimeouts = new HashSet<>();

        protected void stateCheck() {
            LOG.info("{}activeRequests:{} activeTimeout:{}",
                    new Object[]{logPrefix, activeRequests.size(), activeTimeouts.size()});
        }

        //handler for caracal disconnected
        Handler handleCCOpRequestDisconnected = new Handler<CCOpRequest>() {
            @Override
            public void handle(CCOpRequest request) {
                LOG.trace("{}received:{} - disconnected - automatic timeout", logPrefix, request);
                answer(request, request.timeout());
            }
        };

        //handlers for caracalConnected
        Handler handleCCOpRequest = new Handler<CCOpRequest>() {
            @Override
            public void handle(CCOpRequest request) {
                LOG.debug("{}received:{}", logPrefix, request);
                Address caracalNode = caracalTracker.randomActiveNode();
                CaracalMsg msg = new CaracalMsg(privateAdr, caracalNode, request.opReq);
                ForwardMessage fmsg = new ForwardMessage(privateAdr, caracalNode, request.forwardTo, msg);
                LOG.trace("{}sending:{} to:{}", new Object[]{logPrefix, msg, caracalNode});
                trigger(fmsg, network);
                activeRequests.put(fmsg.getId(), Pair.with(request, scheduleCaracalOpTimeout(fmsg.getId(), caracalNode)));
            }
        };

        Handler handleCCOpResponse = new Handler<CaracalMsg>() {
            @Override
            public void handle(CaracalMsg response) {
                LOG.trace("{}received:{}", logPrefix, response);
                Pair<CCOpRequest, UUID> requestInfo = activeRequests.remove(response.getId());
                if (requestInfo == null) {
                    LOG.trace("{}late message:{}", logPrefix, response);
                    return;
                }
                LOG.debug("{}received response:{} from:{}", new Object[]{logPrefix, response.op, response.header.getSource()});
                cancelCaracalOpTimeout(requestInfo.getValue1());
                answer(requestInfo.getValue0(), requestInfo.getValue0().success(response.op));
            }
        };
        Handler handleCCOpTimeout = new Handler<CaracalOpTimeout>() {
            @Override
            public void handle(CaracalOpTimeout timeout) {
                LOG.trace("{}timeout on:{} from:{}", new Object[]{logPrefix, timeout.messageId, timeout.caracalNode});
                if (!activeTimeouts.remove(timeout.getTimeoutId())) {
                    LOG.trace("{}late timeout for message:{}", logPrefix, timeout.messageId);
                    return;
                }
                Pair<CCOpRequest, UUID> requestInfo = activeRequests.remove(timeout.messageId);
                caracalTracker.suspectedNode(timeout.caracalNode);
                answer(requestInfo.getValue0(), requestInfo.getValue0().timeout());
            }
        };

        private UUID scheduleCaracalOpTimeout(UUID messageId, Address caracalNode) {
            ScheduleTimeout st = new ScheduleTimeout(bootstrapConfig.caracalRtt);
            CaracalOpTimeout ct = new CaracalOpTimeout(st, messageId, caracalNode);
            st.setTimeoutEvent(ct);
            trigger(st, timer);
            activeTimeouts.add(ct.getTimeoutId());
            return ct.getTimeoutId();
        }

        private void cancelCaracalOpTimeout(UUID timeoutId) {
            CancelTimeout cpt = new CancelTimeout(timeoutId);
            trigger(cpt, timer);
            activeTimeouts.remove(timeoutId);
        }
    }

    //**************************************************************************
    public static class CCBootstrapInit extends Init<CCBootstrapComp> {

        public final Address privateAdr;

        public CCBootstrapInit(KAddress selfAdr) {
            this.privateAdr = new Address(selfAdr.getIp(), selfAdr.getPort(), null);
        }
    }

    private void schedulePeriodicStateCheck() {
        if (sanityCheckTId != null) {
            LOG.warn("{} double starting sanityCheck", logPrefix);
            return;
        }
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(bootstrapConfig.stateCheckPeriod, bootstrapConfig.stateCheckPeriod);
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

    public class SanityCheckTimeout extends Timeout {

        public SanityCheckTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }

        @Override
        public String toString() {
            return "SANITYCHECK_TIMEOUT";
        }
    }

    public class CaracalOpTimeout extends Timeout {

        public final UUID messageId;
        public final Address caracalNode;

        public CaracalOpTimeout(ScheduleTimeout request, UUID messageId, Address caracalNode) {
            super(request);
            this.messageId = messageId;
            this.caracalNode = caracalNode;
        }

        @Override
        public String toString() {
            return "CARACAL_OP_TIMEOUT<" + messageId + ">";
        }
    }

    public class CaracalSampleTimeout extends Timeout {

        public final Address caracalNode;

        public CaracalSampleTimeout(ScheduleTimeout request, Address caracalNode) {
            super(request);
            this.caracalNode = caracalNode;
        }

        @Override
        public String toString() {
            return "CARACAL_SAMPLE_TIMEOUT";
        }
    }
}

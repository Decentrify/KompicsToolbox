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
package se.sics.nutil.network.bestEffort;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.MessageNotify;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.util.identifiable.Identifiable;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.nutil.network.bestEffort.event.BestEffortMsg;
import se.sics.nutil.tracking.load.NetworkQueueLoadProxy;
import se.sics.nutil.tracking.load.QueueLoadConfig;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BestEffortNetworkComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(BestEffortNetworkComp.class);
    private String logPrefix;

    Negative<Network> incomingNetworkPort = provides(Network.class);
    Positive<Network> outgoingNetworkPort = requires(Network.class);
    Positive<Timer> timerPort = requires(Timer.class);

    private final KAddress self;
    private long timeoutsNotLate = 0;

    //turn into a wheel
    //<contentId, targetId>
    private final Map<Pair<Identifier, Identifier>, UUID> pendingMsgs = new HashMap<>();

    private final NetworkQueueLoadProxy loadTracking;

    public BestEffortNetworkComp(Init init) {
        this.self = init.self;
        this.logPrefix = "<nid:" + self.getId() + ">";

        loadTracking = new NetworkQueueLoadProxy(logPrefix+"bestEffort", proxy, new QueueLoadConfig(config()));

        subscribe(handleStart, control);
        subscribe(handleRetry, timerPort);
        //TODO Alex - once shortcircuit channels work replace these handler with proper ClassMatchers + shortcircuit channel
        subscribe(handleOutgoingMsg, incomingNetworkPort);
        subscribe(handleIncomingMsg, outgoingNetworkPort);
        subscribe(handleForwardMessageNotifyReq, incomingNetworkPort);
        subscribe(handleForwardMessageNotifyResp, outgoingNetworkPort);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            loadTracking.start();
        }
    };

    @Override
    public void tearDown() {
        loadTracking.tearDown();
    }

    Handler handleOutgoingMsg = new Handler<Msg>() {
        @Override
        public void handle(Msg msg) {
            if (!(msg instanceof BasicContentMsg)) {
                LOG.trace("{}forwarding outgoing:{}", logPrefix, msg);
                trigger(msg, outgoingNetworkPort);
            }
            BasicContentMsg contentMsg = (BasicContentMsg) msg;
            if (contentMsg.getContent() instanceof BestEffortMsg.Request) {
                handleRequest(contentMsg);
            } else if (contentMsg.getContent() instanceof BestEffortMsg.Cancel) {
                handleCancel(contentMsg);
            } else {
                LOG.trace("{}forwarding outgoing:{}", logPrefix, msg);
                trigger(msg, outgoingNetworkPort);
            }
        }
    };

    Handler handleIncomingMsg = new Handler<Msg>() {
        @Override
        public void handle(Msg msg) {
            if (loadTracking.getFilter().filter(msg)) {
                return;
            }
            if (!(msg instanceof BasicContentMsg)) {
                LOG.trace("{}forwarding incoming:{}", logPrefix, msg);
                trigger(msg, incomingNetworkPort);
                return;
            }
            BasicContentMsg contentMsg = (BasicContentMsg) msg;
            if (contentMsg.getContent() instanceof Identifiable) {
                handleResponse(contentMsg);
            } else {
                LOG.trace("{}forwarding incoming:{}", logPrefix, msg);
                trigger(msg, incomingNetworkPort);
            }
        }
    };

    Handler handleRetry = new Handler<RetryTimeout>() {
        @Override
        public void handle(RetryTimeout timeout) {
            if (pendingMsgs.remove(timeout.msgId) == null) {
                LOG.trace("{}late retry:{}", logPrefix, timeout.msg);
            } else {
                if (timeout.retriesLeft == 0) {
                    timeoutsNotLate++;
                    BasicContentMsg msg = timeout.msg.answer(timeout.req.timeout());
                    LOG.info("{}retry timeout rto:{} of:{}", new Object[]{logPrefix, timeout.req.rto, msg});
                    trigger(msg, incomingNetworkPort);
                } else {
                    LOG.debug("{}sending retry msg:{}", logPrefix, timeout.msg);
                    doRetry(timeout.msgId, timeout.msg, timeout.req, timeout.retriesLeft - 1);
                }
            }
        }
    };

    Handler handleForwardMessageNotifyReq = new Handler<MessageNotify.Req>() {
        @Override
        public void handle(MessageNotify.Req req) {
            trigger(req, outgoingNetworkPort);
        }
    };

    Handler handleForwardMessageNotifyResp = new Handler<MessageNotify.Resp>() {
        @Override
        public void handle(MessageNotify.Resp resp) {
            trigger(resp, incomingNetworkPort);
        }
    };

    private void doRetry(Pair<Identifier, Identifier> msgId, BasicContentMsg msg, BestEffortMsg.Request retryContent, int retriesLeft) {
        ScheduleTimeout st = new ScheduleTimeout(retryContent.rto);
        RetryTimeout rt = new RetryTimeout(st, retryContent, msg, msgId, retriesLeft);
        st.setTimeoutEvent(rt);
        LOG.debug("{}schedule retry in:{}", logPrefix, retryContent.rto);
        trigger(st, timerPort);
        pendingMsgs.put(msgId, rt.getTimeoutId());
        trigger(msg, outgoingNetworkPort);
    }

    private <C extends Identifiable> void handleRequest(BasicContentMsg<KAddress, KHeader<KAddress>, BestEffortMsg.Request<C>> m) {
        BestEffortMsg.Request<C> retryContent = m.getContent();
        C baseContent = retryContent.getWrappedContent();
        KAddress target = m.getHeader().getDestination();
        Pair<Identifier, Identifier> msgId = Pair.with(baseContent.getId(), target.getId());
        BasicContentMsg msg = new BasicContentMsg(m.getHeader(), baseContent);

        LOG.debug("{}sending msg:{}", logPrefix, msg);
        doRetry(msgId, msg, retryContent, retryContent.retries);
    }

    private void handleResponse(BasicContentMsg<KAddress, KHeader<KAddress>, Identifiable> msg) {
        Identifier msgId = msg.getContent().getId();
        Identifier targetId = msg.getHeader().getSource().getId();
        UUID tid = pendingMsgs.remove(Pair.with(msgId, targetId));
        if (tid != null) {
            LOG.debug("{}forwarding response:{}", logPrefix, msg.getContent());
            trigger(new CancelTimeout(tid), timerPort);
        } else {
            LOG.debug("{}forwarding incoming:{}", logPrefix, msg.getContent());
            timeoutsNotLate--;
            LOG.info("{}timeoutsnotlate:{}", logPrefix, timeoutsNotLate);
        }
        trigger(msg, incomingNetworkPort);
    }

    private <C extends KompicsEvent & Identifiable> void handleCancel(BasicContentMsg<KAddress, KHeader<KAddress>, BestEffortMsg.Cancel<C>> m) {
        C baseContent = m.getContent().content;
        KAddress target = m.getHeader().getDestination();

        UUID tid = pendingMsgs.remove(Pair.with(baseContent.getId(), target.getId()));
        if (tid == null) {
            LOG.trace("{}late cancel:{}", logPrefix, m);
        } else {
            LOG.debug("{}cancel:{}", logPrefix, m);
            trigger(new CancelTimeout(tid), timerPort);
        }
    }

    public static class Init extends se.sics.kompics.Init<BestEffortNetworkComp> {

        public final KAddress self;

        public Init(KAddress self) {
            this.self = self;
        }
    }

    public static class RetryTimeout extends Timeout {

        public final BestEffortMsg.Request req;
        public final BasicContentMsg msg;
        public final Pair<Identifier, Identifier> msgId;
        public final int retriesLeft;

        public RetryTimeout(ScheduleTimeout st, BestEffortMsg.Request req, BasicContentMsg msg, Pair<Identifier, Identifier> msgId, int retriesLeft) {
            super(st);
            this.req = req;
            this.msg = msg;
            this.msgId = msgId;
            this.retriesLeft = retriesLeft;
        }
    }
}

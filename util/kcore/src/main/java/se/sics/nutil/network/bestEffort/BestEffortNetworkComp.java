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

import java.util.List;
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
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.util.Identifiable;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.timer.RingTimer;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.nutil.network.bestEffort.event.BestEffortMsg;
import se.sics.nutil.tracking.load.NetworkQueueLoadProxy;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BestEffortNetworkComp extends ComponentDefinition {

  private static final Logger LOG = LoggerFactory.getLogger(BestEffortNetworkComp.class);
  private String logPrefix;
  //******************************************************************************************************************
  Negative<Network> incomingNetworkPort = provides(Network.class);
  Positive<Network> outgoingNetworkPort = requires(Network.class);
  Positive<Timer> timerPort = requires(Timer.class);
  //******************************************************************************************************************
  private final KAddress self;
  //******************************************************************************************************************
  //turn into a wheel
  //<contentId, targetId>
  private final RingTimer timer;
  //******************************************************************************************************************
  private final NetworkQueueLoadProxy loadTracking;

  public BestEffortNetworkComp(Init init) {
    this.self = init.self;
    this.logPrefix = "<" + init.id + ">";

    timer = new RingTimer(50, 25000);
    BestEffortNetworkConfig beConfig = new BestEffortNetworkConfig(config());
    loadTracking = NetworkQueueLoadProxy.instance("load_be_" + logPrefix, proxy, config(), beConfig.reportDir);

    subscribe(handleStart, control);
    subscribe(handleRingTimer, timerPort);
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
      scheduleRingPeriodicTimeout(50);
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

  Handler handleRingTimer = new Handler<RingTimeout>() {
    @Override
    public void handle(RingTimeout timeout) {
      LOG.debug("{}ring size:{}", logPrefix, timer.getSize());
      List<RingContainer> timeouts = (List) timer.windowTick();
      for (RingContainer tc : timeouts) {
        if (tc.retriesLeft == 0) {
          BasicContentMsg msg = tc.msg.answer(tc.req.timeout());
          LOG.debug("{}retry timeout:{}", logPrefix, msg);
          trigger(msg, incomingNetworkPort);
        } else {
          LOG.debug("{}sending retry msg:{}", logPrefix, tc.msg);
          doRetry(tc.msg, tc.req, tc.retriesLeft - 1);
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

  private void doRetry(BasicContentMsg msg, BestEffortMsg.Request retryContent, int retriesLeft) {
    RingContainer rt = new RingContainer(retryContent, msg, retriesLeft);
    LOG.debug("{}schedule retry in:{}", logPrefix, retryContent.rto);
    trigger(msg, outgoingNetworkPort);
    if(!timer.setTimeout(2 * retryContent.rto, rt)) {
      throw new RuntimeException("fix me with long timer");
    }
  }

  private <C extends Identifiable> void handleRequest(
    BasicContentMsg<KAddress, KHeader<KAddress>, BestEffortMsg.Request<C>> m) {
    BestEffortMsg.Request<C> retryContent = m.getContent();
    C baseContent = retryContent.extractValue();
    KAddress target = m.getHeader().getDestination();
    BasicContentMsg msg = new BasicContentMsg(m.getHeader(), baseContent);

    LOG.debug("{}sending msg:{}", logPrefix, msg);
    doRetry(msg, retryContent, retryContent.retries);
  }

  private void handleResponse(BasicContentMsg<KAddress, KHeader<KAddress>, Identifiable> msg) {
    Identifier msgId = msg.getContent().getId();
    timer.cancelTimeout(msgId);
    trigger(msg, incomingNetworkPort);
  }

  private <C extends KompicsEvent & Identifiable> void handleCancel(
    BasicContentMsg<KAddress, KHeader<KAddress>, BestEffortMsg.Cancel<C>> m) {
    C baseContent = m.getContent().content;
    KAddress target = m.getHeader().getDestination();
    //TODO Alex URGENT - check if this id matches message
    timer.cancelTimeout(baseContent.getId());
  }

  public static class Init extends se.sics.kompics.Init<BestEffortNetworkComp> {

    public final KAddress self;
    public final Identifier id;

    public Init(KAddress self, Identifier id) {
      this.self = self;
      this.id = id;
    }
  }

  public static class RingContainer implements RingTimer.Container {

    public final BestEffortMsg.Request req;
    public final BasicContentMsg<KAddress, KHeader<KAddress>, Identifiable> msg;
    public final int retriesLeft;

    public RingContainer(BestEffortMsg.Request req, BasicContentMsg<KAddress, KHeader<KAddress>, Identifiable> msg,
      int retriesLeft) {
      this.req = req;
      this.msg = msg;
      this.retriesLeft = retriesLeft;
    }

    @Override
    public Identifier getId() {
      return msg.getContent().getId();
    }
  }

  private void scheduleRingPeriodicTimeout(long period) {
    SchedulePeriodicTimeout st = new SchedulePeriodicTimeout(period, period);
    RingTimeout rt = new RingTimeout(st);
    st.setTimeoutEvent(rt);
    LOG.debug("{}schedule periodic ring timer", logPrefix);
    trigger(st, timerPort);
  }
  
  public static class RingTimeout extends Timeout {

    public RingTimeout(SchedulePeriodicTimeout st) {
      super(st);
    }
  }
}

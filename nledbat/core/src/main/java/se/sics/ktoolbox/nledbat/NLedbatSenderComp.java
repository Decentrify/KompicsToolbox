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
package se.sics.ktoolbox.nledbat;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.util.Identifiable;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nledbat.event.external.NLedbatEvents;
import se.sics.ktoolbox.nledbat.event.internal.NLedbatMsg;
import se.sics.ktoolbox.nledbat.util.Cwnd;
import se.sics.ktoolbox.nledbat.util.LedbatConfig;
import se.sics.ktoolbox.nledbat.util.RTTEstimator;
import se.sics.ktoolbox.nutil.timer.RingTimer;
import se.sics.ktoolbox.nutil.timer.RingTimer.Container;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NLedbatSenderComp extends ComponentDefinition {

  private static final Logger LOG = LoggerFactory.getLogger(NLedbatSenderComp.class);
  private String logPrefix = "";

  Positive<Timer> timerPort = requires(Timer.class);
  Negative<Network> incomingNetworkPort = provides(Network.class);
  Positive<Network> outgoingNetworkPort = requires(Network.class);
  Negative<NLedbatSenderCtrl> ctrlPort = provides(NLedbatSenderCtrl.class);

  private final Identifier dataId;
  private final Identifier senderId;
  private final Identifier receiverId;
  private final Cwnd cwnd;
  private final RTTEstimator rttEstimator;
  private final RingTimer ringTimer;
  private final LedbatConfig ledbatConfig;
  private UUID ringTimeout;
  private UUID statusTimeout;
  private LinkedList<BasicContentMsg> pendingData = new LinkedList<>();

  public NLedbatSenderComp(Init init) {
    dataId = init.dataId;
    senderId = init.senderId;
    receiverId = init.receiverId;
    logPrefix = "<" + dataId + "," + senderId + "," + receiverId + ">";

    ledbatConfig = new LedbatConfig();
    cwnd = new Cwnd(ledbatConfig);
    rttEstimator = new RTTEstimator(ledbatConfig);
    ringTimer = new RingTimer(HardCodedConfig.windowSize, HardCodedConfig.maxTimeout);

    subscribe(handleStart, control);
    subscribe(handleIncomingAck, outgoingNetworkPort);
    subscribe(handleOutgoingMsg, incomingNetworkPort);
    subscribe(handleRingTimeout, timerPort);
    subscribe(handleStatusTimeout, timerPort);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      LOG.info("{}starting...", logPrefix);
      scheduleRingTimeout(HardCodedConfig.windowSize);
      scheduleStatusTimeout(HardCodedConfig.statusPeriod);
    }
  };

  @Override
  public void tearDown() {
    cancelRingTimeout();
    cancelStatusTimeout();
  }

  ClassMatchedHandler handleIncomingAck
    = new ClassMatchedHandler<NLedbatMsg.Ack, BasicContentMsg<?, ?, NLedbatMsg.Ack>>() {

      @Override
      public void handle(NLedbatMsg.Ack payload, BasicContentMsg<?, ?, NLedbatMsg.Ack> msg) {
        LOG.trace("{}received:{}", logPrefix, msg);
        Optional<Container> containerAux = ringTimer.cancelTimeout(payload.eventId);
        if (!containerAux.isPresent()) {
          LOG.trace("{}late:{}", logPrefix, payload.getId());
          return;
        }
        long now = System.currentTimeMillis();
        long rtt = payload.ackDelay.receive - payload.dataDelay.send;
        long dataDelay = payload.dataDelay.receive - payload.dataDelay.send;
        cwnd.ack(now, dataDelay, ledbatConfig.MSS);
        rttEstimator.update(rtt);
        ringTimer.cancelTimeout(payload.eventId);
        trySend2();
      }
    };

  Handler handleOutgoingMsg = new Handler<BasicContentMsg>() {
    @Override
    public void handle(BasicContentMsg msg) {
      LOG.trace("{}received:{}", logPrefix, msg);
      pendingData.add((BasicContentMsg) msg);
      trySend2();
      return;
    }
  };

  Handler handleRingTimeout = new Handler<RingTimeout>() {

    @Override
    public void handle(RingTimeout timeout) {
      LOG.trace("{}ring timeout", logPrefix);
      List<Container> timeouts = ringTimer.windowTick();
      long now = System.currentTimeMillis();
      for (Container c : timeouts) {
        RingContainer rc = (RingContainer) c;
        LOG.debug("{}msg:{} timed out", logPrefix, rc.msg);
        cwnd.loss(now, rttEstimator.rto(), ledbatConfig.MSS);
        pendingData.addFirst(rc.msg);
        trySend2();
      }
    }
  };

  Handler handleStatusTimeout = new Handler<StatusTimeout>() {

    @Override
    public void handle(StatusTimeout event) {
      trigger(new NLedbatEvents.SenderStatus(cwnd.size(), rttEstimator.rto(), pendingData.size()), ctrlPort);
    }
  };

  private void trySend2() {
    trySend();
    trySend();
  }

  private void trySend() {
    if (!pendingData.isEmpty() && cwnd.canSend(ledbatConfig.MSS)) {
      BasicContentMsg<?, ?, Identifiable> msg = pendingData.removeFirst();
      LOG.trace("{}sending:{}", logPrefix, msg);
      NLedbatMsg.Data wrappedData = new NLedbatMsg.Data(dataId, msg.extractValue());
      BasicContentMsg ledbatMsg = new BasicContentMsg(msg.getHeader(), wrappedData);
      trigger(ledbatMsg, outgoingNetworkPort);
      cwnd.send(ledbatConfig.MSS);
      ringTimer.setTimeout(rttEstimator.rto(), new RingContainer(msg));
    }
  }

  private void scheduleRingTimeout(long period) {
    SchedulePeriodicTimeout st = new SchedulePeriodicTimeout(period, period);
    RingTimeout rt = new RingTimeout(st, NLedbat.senderTransferId(dataId, senderId, receiverId));
    st.setTimeoutEvent(rt);
    LOG.debug("{}schedule periodic ring timer", logPrefix);
    trigger(st, timerPort);
    ringTimeout = rt.getTimeoutId();
  }

  private void cancelRingTimeout() {
    CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(ringTimeout);
    LOG.debug("{}cancel periodic ring timer", logPrefix);
    trigger(cpt, timerPort);
  }

  private void scheduleStatusTimeout(long period) {
    SchedulePeriodicTimeout st = new SchedulePeriodicTimeout(period, period);
    StatusTimeout rt = new StatusTimeout(st, NLedbat.senderTransferId(dataId, senderId, receiverId));
    st.setTimeoutEvent(rt);
    LOG.debug("{}schedule periodic status timer", logPrefix);
    trigger(st, timerPort);
    statusTimeout = rt.getTimeoutId();
  }

  private void cancelStatusTimeout() {
    CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(statusTimeout);
    LOG.debug("{}cancel periodic status timer", logPrefix);
    trigger(cpt, timerPort);
  }

  public static class Init extends se.sics.kompics.Init<NLedbatSenderComp> {

    public final Identifier dataId;
    public final Identifier senderId;
    public final Identifier receiverId;

    public Init(Identifier dataId, Identifier senderId, Identifier receiverId) {
      this.dataId = dataId;
      this.senderId = senderId;
      this.receiverId = receiverId;
    }
  }

  public static class RingContainer implements RingTimer.Container {

    public final BasicContentMsg<?, ?, Identifiable> msg;

    public RingContainer(BasicContentMsg msg) {
      this.msg = msg;
    }

    @Override
    public Identifier getId() {
      return msg.extractValue().getId();
    }
  }

  public static abstract class PeriodicTimeout extends Timeout {
    public final Identifier transferId;
    public PeriodicTimeout(SchedulePeriodicTimeout spt, Identifier transferId) {
      super(spt);
      this.transferId = transferId;
    }
    
    public Identifier transferId() {
      return transferId;
    }
  }
  
  public static class RingTimeout extends PeriodicTimeout {

    public RingTimeout(SchedulePeriodicTimeout spt, Identifier transferId) {
      super(spt, transferId);
    }
  }

  public static class StatusTimeout extends PeriodicTimeout {

    public StatusTimeout(SchedulePeriodicTimeout spt, Identifier transferId) {
      super(spt, transferId);
    }
  }

  public static class HardCodedConfig {

    public static int statusPeriod = 100;
    public static int windowSize = 50;
    public static int maxTimeout = 25000;
  }

}

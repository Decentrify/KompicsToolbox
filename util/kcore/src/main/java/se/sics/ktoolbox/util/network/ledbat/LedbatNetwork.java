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
package se.sics.ktoolbox.util.network.ledbat;

import com.google.common.base.Optional;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Future;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.MDC;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.ConnectionStatus;
import se.sics.kompics.network.MessageNotify;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.NetworkException;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.netty.AckRequestMsg;
import se.sics.kompics.network.netty.NettyAddress;
import se.sics.kompics.network.netty.NotifyAck;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.util.Identifiable;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.timer.RingTimer;
import se.sics.ktoolbox.nutil.timer.TimerProxy;
import se.sics.ktoolbox.nutil.timer.TimerProxyImpl;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import static se.sics.ktoolbox.util.network.ledbat.LedbatNetwork.ledbatCwnd;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LedbatNetwork extends ComponentDefinition {

  static final long FLOW_CONTROL_GRANULARITY = 5;

  static final int RECV_BUFFER_SIZE = 65536;
  static final int SEND_BUFFER_SIZE = 65536;
  static final int INITIAL_BUFFER_SIZE = 512;
  private static final int CONNECT_TIMEOUT_MS = 5000;

  Negative<Network> net = provides(Network.class);
  Negative<LedbatStatus.Port> status = provides(LedbatStatus.Port.class);
  Positive<Timer> timerPort = requires(Timer.class);
  TimerProxy timer;
  UUID flowControlTId;
  UUID reportTid;

  private Bootstrap bootstrapUDP;
  private DatagramChannel udpChannel;
  final NettyAddress self;
  private final int boundPort;
  private InetAddress alternativeBindIf = null;

  final MessageQueueManager messages = new MessageQueueManager(this);
  LedbatFlowControl ledbatFlowControl;
  LedbatConfigW ledbatConfig;
  final KAddress selfAdr;

  public static final String MDC_KEY_PORT = "knet-port";
  public static final String MDC_KEY_IF = "knet-if";
  public static final String MDC_KEY_ALT_IF = "knet-alt-if";
  final Logger extLog = this.logger;

  private final Map<String, String> customLogCtx = new HashMap<>();

  void setCustomMDC() {
    MDC.setContextMap(customLogCtx);
  }

  private void initLoggingCtx() {
    for (Map.Entry<String, String> e : customLogCtx.entrySet()) {
      loggingCtxPutAlways(e.getKey(), e.getValue());
    }
  }

  public LedbatNetwork(Init init) {
    // probably useless to set here as it won't be re-read in most JVMs after start
    System.setProperty("java.net.preferIPv4Stack", "true");

    self = new NettyAddress(init.self);
    selfAdr = init.self;
    customLogCtx.put(MDC_KEY_PORT, Integer.toString(self.getPort()));
    customLogCtx.put(MDC_KEY_IF, self.getIp().getHostAddress());

    boundPort = self.getPort();

    // CONFIG
    Optional<InetAddress> abiO = config().readValue("netty.bindInterface", InetAddress.class);
    if (abiO.isPresent()) {
      alternativeBindIf = abiO.get();
      customLogCtx.put(MDC_KEY_ALT_IF, self.getIp().getHostAddress());
    }

    initLoggingCtx();

    Identifier noTimerProxyId = null;
    timer = new TimerProxyImpl(noTimerProxyId).setup(proxy, logger);

    logger.info("Alternative Bind Interface set to {}", alternativeBindIf);

    subscribe(handleStart, control);
    subscribe(handleStop, control);
    subscribe(handleMsg, net);
  }

  Handler<Start> handleStart = new Handler<Start>() {

    @Override
    public void handle(Start event) {
      try {
        ledbatConfig = LedbatConfigW.instance(config()).checkedGet();
      } catch (Throwable ex) {
        throw new RuntimeException(ex);
      }

      ledbatFlowControl = new LedbatFlowControl(ledbatConfig);

      // Prepare listening sockets
      InetAddress bindIp = self.getIp();
      if (alternativeBindIf != null) {
        bindIp = alternativeBindIf;
      }
      bindUdpPort(bindIp, self.getPort());
      flowControlTId = timer.schedulePeriodicTimer(FLOW_CONTROL_GRANULARITY, FLOW_CONTROL_GRANULARITY, flowControl());
      if (ledbatConfig.reportPeriod.isPresent()) {
        reportTid = timer.schedulePeriodicTimer(ledbatConfig.reportPeriod.get(), ledbatConfig.reportPeriod.get(),
          flowControlReport());
      }
    }
  };

  Handler<Stop> handleStop = new Handler<Stop>() {

    @Override
    public void handle(Stop event) {
      clearConnections();
    }
  };

  Handler handleMsg = new Handler<Msg>() {
    @Override
    public void handle(Msg msg) {
      if (msg.getHeader().getDestination().sameHostAs(self)) {
        logger.trace("Delivering message {} locally.", msg);
        trigger(msg, net);
        return;
      }
      if (msg instanceof KContentMsg) {
        KContentMsg contentMsg = (KContentMsg) msg;
        if (contentMsg.getContent() instanceof LedbatMsg.Datum) {
          ledbatFlowControl.buffer((KContentMsg<?, ?, LedbatMsg.Datum>) msg);
        } else {
          messages.send(msg);
        }
      } else {
        messages.send(msg);
      }
    }
  };

  private boolean bindUdpPort(final InetAddress addr, final int port) {

    EventLoopGroup group = new NioEventLoopGroup();
    bootstrapUDP = new Bootstrap();
    bootstrapUDP.group(group).channel(NioDatagramChannel.class)
      .handler(new DatagramHandler(this, Transport.UDP));

    bootstrapUDP.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(1500, 1500, RECV_BUFFER_SIZE));
    bootstrapUDP.option(ChannelOption.SO_RCVBUF, RECV_BUFFER_SIZE);
    bootstrapUDP.option(ChannelOption.SO_SNDBUF, SEND_BUFFER_SIZE);
    // bootstrap.setOption("trafficClass", trafficClass);
    // bootstrap.setOption("soTimeout", soTimeout);
    // bootstrap.setOption("broadcast", broadcast);
    bootstrapUDP.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS);
    bootstrapUDP.option(ChannelOption.SO_REUSEADDR, true);

    try {
      InetSocketAddress iAddr = new InetSocketAddress(addr, port);
      udpChannel = (DatagramChannel) bootstrapUDP.bind(iAddr).sync().channel();

      //addLocalSocket(iAddr, c);
      logger.info("Successfully bound to ip:port {}:{}", addr, port);
    } catch (InterruptedException e) {
      logger.error("Problem when trying to bind to {}:{}", addr.getHostAddress(), port);
      return false;
    }

    return true;
  }

  protected void networkException(NetworkException networkException) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  protected void networkStatus(ConnectionStatus status) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  protected void deliverMessage(Msg msg, Channel c) {
    if (msg instanceof NotifyAck) {
      NotifyAck ack = (NotifyAck) msg;
      logger.trace("Got NotifyAck for {}", ack.id);
      messages.ack(ack);
      return;
    }
    logger.debug("Delivering message {} from {} to {} protocol {}",
      new Object[]{msg.toString(), msg.getSource(), msg.getDestination(), msg.getProtocol()});

    if (msg instanceof KContentMsg) {
      KContentMsg contentMsg = (KContentMsg) msg;
      if (contentMsg.getContent() instanceof LedbatMsg.Datum) {
        ledbatFlowControl.deliver(contentMsg);
      } else if (contentMsg.getContent() instanceof LedbatMsg.MultiAck) {
        ledbatFlowControl.ack(contentMsg);
      } else {
        trigger(msg, net);
      }
    } else {
      trigger(msg, net);
    }
  }

  ChannelFuture sendUdpMessage(MessageWrapper msgw) {
    ByteBuf buf = udpChannel.alloc().ioBuffer(INITIAL_BUFFER_SIZE, SEND_BUFFER_SIZE);
    try {
      if (msgw.notify.isPresent() && msgw.notify.get().notifyOfDelivery) {
        MessageNotify.Req msgr = msgw.notify.get();
        AckRequestMsg arm = new AckRequestMsg(msgw.msg, msgr.getMsgId());
        Serializers.toBinary(arm, buf);
      } else {
        Serializers.toBinary(msgw.msg, buf);
      }
      msgw.injectSize(buf.readableBytes(), System.nanoTime());
      DatagramPacket pack = new DatagramPacket(buf, msgw.msg.getDestination().asSocket());
      logger.debug("Sending Datagram message {} ({}bytes)", msgw.msg, buf.readableBytes());
      return udpChannel.writeAndFlush(pack);
    } catch (Exception e) { // serialization might fail horribly with size bounded buff
      logger.warn("Could not send Datagram message {}, error was: {}", msgw, e);
      return null;
    }
  }

  private void clearConnections() {

    long tstart = System.currentTimeMillis();

    try {
      udpChannel.close().syncUninterruptibly();
    } catch (Exception ex) {
      logger.warn("Error during Ledbat shutdown. Messages might have been lost! \n {}", ex);
    }

    long tend = System.currentTimeMillis();

    logger.info("Closed all connections in {}ms", tend - tstart);
  }

  @Override
  public void tearDown() {
    long tstart = System.currentTimeMillis();

    clearConnections();

    logger.info("Shutting down handler groups...");
    Future endUdp = bootstrapUDP.group().shutdownGracefully(1, 5, TimeUnit.MILLISECONDS);
    endUdp.syncUninterruptibly();
    bootstrapUDP = null;

    flowControlTearDown();
    long tend = System.currentTimeMillis();

    logger.info("Ledbat shutdown complete. It took {}ms", tend - tstart);
  }

  void trigger(KompicsEvent event) {
    if (event instanceof Msg) {
      throw new RuntimeException("Not support anymore!");
      //trigger(event, net.getPair());
    } else {
      trigger(event, onSelf);
    }
  }

  void notify(MessageNotify.Req notify) {
    answer(notify);
  }

  void notify(MessageNotify.Req notify, MessageNotify.Resp response) {
    answer(notify, response);
  }

  public static class Init extends se.sics.kompics.Init<LedbatNetwork> {

    public final KAddress self;

    public Init(KAddress self) {
      this.self = self;
    }
  }

  private void flowControlTearDown() {
    if (flowControlTId != null) {
      timer.cancelPeriodicTimer(flowControlTId);
      flowControlTId = null;
    }
    if (reportTid != null) {
      timer.cancelPeriodicTimer(reportTid);
      reportTid = null;
    }
  }

  private Consumer<Boolean> flowControlReport() {
    return (_input) -> {
      ledbatFlowControl.sender.details(logger);
    };
  }

  private Consumer<Boolean> flowControl() {
    return (_ignore) -> {
      ledbatFlowControl.sender.flowTick(logger, (msg) -> messages.send(msg));
      ledbatFlowControl.receiver.sendBatches();
    };
  }

  static Cwnd ledbatCwnd(LedbatConfig ledbatConfig) {
    return new Cwnd(ledbatConfig.INIT_CWND, ledbatConfig.MSS, ledbatConfig.DTL_BETA, ledbatConfig.GAIN,
      ledbatConfig.ALLOWED_INCREASE, ledbatConfig.MIN_CWND);
  }

  static RTTEstimator ledbatRTTEstimator(LedbatConfig ledbatConfig) {
    return new RTTEstimator(ledbatConfig.ALPHA, ledbatConfig.BETA, ledbatConfig.K, ledbatConfig.G);
  }

  static DelayHistory ledbatDelayHistory(LedbatConfig ledbatConfig) {
    return new DelayHistory(ledbatConfig.CURRENT_FILTER, ledbatConfig.BASE_HISTORY);
  }

  class LedbatFlowControl {

    private final LedbatSender sender;
    private final LedbatReceiver receiver;

    public LedbatFlowControl(LedbatConfigW ledbatConfig) {
      sender = new LedbatSender(ledbatConfig, (event) -> trigger(event, status));
      receiver = new LedbatReceiver(selfAdr, (msg) -> messages.send(msg));
    }

    public void buffer(KContentMsg<?, ?, LedbatMsg.Datum> msg) {
      sender.sendingBuffer.add(msg);
    }

    public void deliver(KContentMsg<?, ?, LedbatMsg.Datum> msg) {
      receiver.deliver(msg);
      trigger(msg, net);
    }

    public void ack(KContentMsg<?, ?, LedbatMsg.MultiAck> msg) {
      sender.multiAck(logger, (LedbatMsg.MultiAck) msg.getContent());
    }

  }

  static class LedbatSender {

    private final LedbatConfigW ledbatConfig;
    private DiscretizedSendingBuffer<KContentMsg<?, ?, LedbatMsg.Datum>> sendingBuffer;
    private RingTimer<WheelContainer> wheelTimer;
    private final LossCtrl lossCtrl;
    private Cwnd cwnd;
    private RTTEstimator rttEstimator;
    private DelayHistory delayHistory;
    private final IdentifierFactory eventIds;
    private final Consumer<KompicsEvent> statusSender;

    public LedbatSender(LedbatConfigW ledbatConfig, Consumer<KompicsEvent> statusSender) {
      this.ledbatConfig = ledbatConfig;
      this.sendingBuffer = new DiscretizedSendingBuffer<>(1);
      this.wheelTimer = new RingTimer(FLOW_CONTROL_GRANULARITY, 3 * ledbatConfig.base.MAX_RTO);
      this.lossCtrl = new LossCtrl();
      this.cwnd = ledbatCwnd(ledbatConfig.base);
      this.rttEstimator = ledbatRTTEstimator(ledbatConfig.base);
      this.delayHistory = ledbatDelayHistory(ledbatConfig.base);
      this.eventIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.MSG, java.util.Optional.of(1234l));
      this.statusSender = statusSender;
    }

    public void tearDown() {
    }

    public void flowTick(Logger logger, Consumer<KContentMsg<?, ?, LedbatMsg.Datum>> sender) {
      timeouts(logger);
      long rto = rto();
      int sendingPanes = (int) (rto / FLOW_CONTROL_GRANULARITY);
      long maxSendingSize = (cwnd.size() / ledbatConfig.base.MSS) / sendingPanes;
      int windowSize = 0;
      for (int i = 0; i < maxSendingSize && i < sendingBuffer.buffer.size(); i++) {
        if (cwnd.canSend(logger, ledbatConfig.base.MSS)) {
          windowSize += ledbatConfig.base.MSS;
          cwnd.send(logger, ledbatConfig.base.MSS);
        }
      }
      sendingBuffer.setWindowSize(windowSize);
      sendingBuffer.send((KContentMsg<?, ?, LedbatMsg.Datum> msg) -> {
        wheelTimer.setTimeout(rto, new WheelContainer(msg));
        sender.accept(msg);
      });
    }

    private void timeouts(Logger logger) {
      List<WheelContainer> timedOut = wheelTimer.windowTick();
      long now = System.currentTimeMillis();
      List<KContentMsg<?, ?, LedbatMsg.Datum>> reportT = new LinkedList<>();
      for (WheelContainer c : timedOut) {
        //mean 0.7 micros
        WheelContainer rc = (WheelContainer) c;
//        logger.debug("msg data:{} timed out", rc.req.datum.getId());
        cwnd.loss(logger, ledbatConfig.base.MSS, lossCtrl.loss(logger, now, rto()));
        //
        reportT.add(rc.datumMsg);
      }
      statusSender.accept(new LedbatStatus.Timeout(eventIds.randomId(), reportT, maxAppMsgs()));
    }

    public void multiAck(Logger logger, LedbatMsg.MultiAck acks) {
      long now = System.currentTimeMillis();
      long bytesAcked = 0;
      List<KContentMsg<?, ?, LedbatMsg.Datum>> acked = new LinkedList<>();
      for (LedbatMsg.AckVal ack : acks.acks.acks) {
        java.util.Optional<WheelContainer> ringContainer = wheelTimer.cancelTimeout(ack.msgId);
        long dataDelay = updateRTT(logger, now, acks.acks, ack);
        delayHistory.update(now, dataDelay);
        if (!ringContainer.isPresent()) {
          lossCtrl.acked(logger, now, rto());
        } else {
          acked.add(ringContainer.get().datumMsg);
          bytesAcked += ledbatConfig.base.MSS;
        }
      }
      statusSender.accept(new LedbatStatus.Ack(eventIds.randomId(), acked, maxAppMsgs()));
      cwnd.ack(logger, delayHistory.offTarget(ledbatConfig.base.TARGET), bytesAcked, !sendingBuffer.buffer.isEmpty());
    }

    private long updateRTT(Logger logger, long now, LedbatMsg.BatchAckVal batch, LedbatMsg.AckVal ack) {
      long batchT = batch.rt3 - ack.rt2;

      long rtt = now - ack.st1 - batchT;
      long dataDelay = ack.rt1 - ack.st1;
      logger.debug("rtt:{} bt:{} dd:{}", new Object[]{rtt, batchT, dataDelay});
      rttEstimator.update(rtt);
      return dataDelay;
    }

    private int maxAppMsgs() {
      int kompicsEventBatching = 20;
      int ledbatEventBatching = 2;
      int ledbatMaxIncreasePerAck = 1;
      int cwndAsMsgs = (int) (cwnd.size() / ledbatConfig.base.MSS);
      int requestReadyEvents = 2 * cwndAsMsgs + kompicsEventBatching * ledbatEventBatching * ledbatMaxIncreasePerAck;
      return Math.min(ledbatConfig.bufferSize / ledbatConfig.base.MSS, requestReadyEvents);
    }

    private long rto() {
      return rttEstimator.rto(ledbatConfig.base.INIT_RTO, ledbatConfig.base.MIN_RTO, ledbatConfig.base.MAX_RTO);
    }

    public void details(Logger logger) {
      logger.info("rto:{} cwnd:{}",
        new Object[]{rto(), cwnd.size()});
    }
  }

  static class LedbatReceiver {

    private final Map<Identifier, Batch> batches = new HashMap<>();
    private final IdentifierFactory msgIds;
    private final KAddress selfAdr;
    private final Consumer<KContentMsg> networkSend;

    public LedbatReceiver(KAddress selfAdr, Consumer<KContentMsg> networkSend) {
      this.msgIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.MSG, java.util.Optional.of(1234l));
      this.selfAdr = selfAdr;
      this.networkSend = networkSend;
    }

    public void deliver(KContentMsg<?, ?, LedbatMsg.Datum> msg) {
      Identifier batchId = msg.getHeader().getSource().getId();
      Batch batch = batches.get(batchId);
      if (batch == null) {
        batch = new Batch(msg.getHeader().getSource());
        batches.put(batchId, batch);
      }
      long now = System.currentTimeMillis();
      batch.ack(now, msg.getContent());
      if (batch.acks.size() == 10) {
        sendBatch(now, batch);
        batches.remove(batchId);
      }
    }

    private void sendBatch(long now, Batch batch) {
      LedbatMsg.MultiAck content = new LedbatMsg.MultiAck(msgIds.randomId(), batch.batch());
      KHeader header = new BasicHeader<>(selfAdr, batch.sender, Transport.UDP);
      KContentMsg msg = new BasicContentMsg(header, content);
      networkSend.accept(msg);
    }

    public void sendBatches() {
      Iterator<Batch> it = batches.values().iterator();
      long now = System.currentTimeMillis();
      while (it.hasNext()) {
        Batch batch = it.next();
        sendBatch(now, batch);
        it.remove();
      }
    }
  }

  public static class WheelContainer implements RingTimer.Container {

    public final KContentMsg<?, ?, LedbatMsg.Datum> datumMsg;

    public WheelContainer(KContentMsg<?, ?, LedbatMsg.Datum> datumMsg) {
      this.datumMsg = datumMsg;
    }

    @Override
    public Identifier getId() {
      return datumMsg.getContent().getId();
    }
  }

  static class LossCtrl {

    public boolean loss(Logger logger, long now, long rto) {
      return true;
    }

    public void acked(Logger logger, long now, long rto) {

    }
  }

  static class Cwnd {

    private final double dtl_beta;
    private final double gain;
    private final double allowedIncrease;
    private final int mss;
    private final int minCwnd;

    private long cwnd;
    private long flightSize;

    public Cwnd(int initCwnd, int mss, double dtl_beta, double gain, double allowedIncrease, int minCwnd) {
      this.dtl_beta = dtl_beta;
      this.gain = gain;
      this.allowedIncrease = allowedIncrease;
      this.mss = mss;
      this.minCwnd = minCwnd;

      cwnd = initCwnd * mss;
    }

    public void ack(Logger logger, double offTarget, long bytesNewlyAcked, boolean pendingInFlight) {
      adjustCwnd(logger, offTarget, bytesNewlyAcked, pendingInFlight);
      flightSize -= bytesNewlyAcked;
    }

    public long size() {
      return cwnd;
    }

    public void loss(Logger logger, long bytesNotToBeRetransmitted, boolean adjustLoss) {
      if (adjustLoss) {
        cwnd = Math.max(cwnd / 2, minCwnd * mss);
      }
      flightSize -= bytesNotToBeRetransmitted;
    }

    public boolean canSend(Logger logger, long bytesToSend) {
      return (flightSize + bytesToSend) <= cwnd;
    }

    public void send(Logger logger, long bytesToSend) {
      flightSize += bytesToSend;
    }

    private void adjustCwnd(Logger logger, double offTarget, long bytesNewlyAcked, boolean pendingInFlight) {
      long aux = cwnd;
      if (offTarget < 0) {
        cwnd = (long) (cwnd * dtl_beta);
      } else {
        cwnd = cwnd + (long) ((gain * offTarget * bytesNewlyAcked * mss) / cwnd);
      }
      long maxAllowedCwnd;
      if (pendingInFlight) {
        maxAllowedCwnd = aux + (long) (allowedIncrease * mss);
      } else {
        maxAllowedCwnd = flightSize + (long) (allowedIncrease * mss);
      }
      cwnd = Math.min(cwnd, maxAllowedCwnd);
      cwnd = Math.max(cwnd, minCwnd * mss);
      logger.info("cwnd pre:{} post:{} ot:{}", new Object[]{aux, cwnd, offTarget});
    }

  }

  static class RTTEstimator {

    public final int k;
    public final double alpha;
    public final double beta;
    public final int g;

    private long rto = -1;
    private long srtt;
    private long rttvar;

    public RTTEstimator(double alpha, double beta, int k, int g) {
      this.alpha = alpha;
      this.beta = beta;
      this.k = k;
      this.g = g;
    }

    public RTTEstimator() {
      this(0.125, 0.25, 4, Integer.MIN_VALUE);
    }

    public void update(long r) {
      if (rto == -1) {
        updateFirst(r);
      } else {
        updateNext(r);
      }
      rto = srtt + Math.max(g, k * rttvar);
    }

    private void updateFirst(long r) {
      srtt = r;
      rttvar = r / 2;
    }

    private void updateNext(long r) {
      rttvar = (long) ((1 - beta) * rttvar + (beta * Math.abs(srtt - r)));
      srtt = (long) ((1 - alpha) * srtt + alpha * r);
    }

    public long rto(long initRTO) {
      if (rto == -1) {
        return initRTO;
      } else {
        return rto;
      }
    }

    public long rto(long initRTO, long minRTO, long maxRTO) {
      return Math.min(maxRTO, Math.max(minRTO, rto(initRTO)));
    }

    public void details(Logger logger) {
      logger.info("rto:{}, rrtvar:{}", new Object[]{rto, rttvar});
    }
  }

  static class DelayHistory {

    private final int currentDelaysSize;
    private final int baseDelaysSize;
    private final long[] currentDelays;
    private int currentDelaysPointer = -1;
    private final long[] baseDelays;
    private int baseDelaysPointer = -1;
    private long lastRolloverMinute;

    public DelayHistory(int currentDelaysSize, int baseDelaysSize) {
      this.currentDelaysSize = currentDelaysSize;
      this.baseDelaysSize = baseDelaysSize;
      this.currentDelays = new long[currentDelaysSize];
      this.baseDelays = new long[baseDelaysSize];
      resetDelays();
    }

    public DelayHistory resetDelays() {
      lastRolloverMinute = Long.MIN_VALUE;
      for (int i = 0; i < currentDelaysSize; i++) {
        currentDelays[i] = Long.MAX_VALUE;
      }
      for (int i = 0; i < currentDelaysSize; i++) {
        baseDelays[i] = Long.MAX_VALUE;
      }
      return this;
    }

    public void update(long now, long oneWayDelay) {
      updateBaseDelay(now, oneWayDelay);
      updateCurrentDelay(oneWayDelay);
    }

    public double offTarget(int target) {
      long queuingDelay = queuingDelay();
      double offTarget = offTarget(target, queuingDelay);
      return offTarget;
    }

    public double offTarget(int target, long queuingDelay) {
      return (target - queuingDelay) / target;
    }

    public long queuingDelay() {
      return Arrays.stream(currentDelays).min().getAsLong()
        - Arrays.stream(baseDelays).min().getAsLong();
    }

    private void updateCurrentDelay(long oneWayDelay) {
      currentDelaysPointer += 1;
      if (currentDelaysPointer >= currentDelaysSize) {
        currentDelaysPointer = 0;
      }
      currentDelays[currentDelaysPointer] = oneWayDelay;
    }

    private void updateBaseDelay(long now, long oneWayDelay) {
      long nowMinute = roundToMinute(now);
      if (nowMinute > lastRolloverMinute) {
        baseDelaysPointer += 1;
        if (baseDelaysPointer >= baseDelaysSize) {
          baseDelaysPointer = 0;
        }
        baseDelays[baseDelaysPointer] = oneWayDelay;
        lastRolloverMinute = roundToMinute(lastRolloverMinute);
      } else {
        baseDelays[baseDelaysPointer] = Math.min(baseDelays[baseDelaysPointer], oneWayDelay);
      }
    }

    private long roundToMinute(long timeInMillis) {
      return timeInMillis / 60000;
    }

    public void details(Logger logger) {
      long queuingDelay = queuingDelay();
      logger.info("qd:{}", new Object[]{queuingDelay});
    }
  }

  static class DiscretizedSendingBuffer<C extends Object> {

    private final LinkedList<C> buffer = new LinkedList<>();
    private int windowSize;

    public DiscretizedSendingBuffer(int windowSize) {
      this.windowSize = windowSize;
    }

    public void setWindowSize(int windowSize) {
      this.windowSize = windowSize;
    }

    public void add(C content) {
      buffer.add(content);
    }

    public void send(Consumer<C> sender) {
      List<C> result = new LinkedList<>();
      Iterator<C> it = buffer.iterator();
      int sendingBatch = windowSize;
      while (it.hasNext() && sendingBatch > 0) {
        C next = it.next();
        sender.accept(next);
        it.remove();
      }
    }
  }

  static class Batch {

    public List<LedbatMsg.AckVal> acks = new LinkedList<>();
    public final KAddress sender;

    public Batch(KAddress sender) {
      this.sender = sender;
    }

    public void ack(long now, LedbatMsg.Datum<Identifiable<Identifier>> datum) {
      LedbatMsg.AckVal ackVal = new LedbatMsg.AckVal(datum.getId());
      ackVal.setRt2(now);
      acks.add(ackVal);
    }

    public LedbatMsg.BatchAckVal batch() {
      LedbatMsg.BatchAckVal batch = new LedbatMsg.BatchAckVal(new LinkedList<>(acks));
      acks.clear();
      return batch;
    }
  }
}

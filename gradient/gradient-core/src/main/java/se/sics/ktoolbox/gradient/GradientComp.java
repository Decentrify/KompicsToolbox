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
package se.sics.ktoolbox.gradient;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.croupier.CroupierPort;
import se.sics.ktoolbox.croupier.event.CroupierSample;
import se.sics.ktoolbox.gradient.aggregation.GradientViewPacket;
import se.sics.ktoolbox.gradient.aggregation.GradientViewReducer;
import se.sics.ktoolbox.gradient.event.GradientSample;
import se.sics.ktoolbox.gradient.msg.GradientShuffle;
import se.sics.ktoolbox.gradient.temp.RankUpdate;
import se.sics.ktoolbox.gradient.temp.RankUpdatePort;
import se.sics.ktoolbox.gradient.util.GradientContainer;
import se.sics.ktoolbox.gradient.util.GradientLocalView;
import se.sics.ktoolbox.gradient.util.GradientView;
import se.sics.ktoolbox.util.aggregation.CompTracker;
import se.sics.ktoolbox.util.aggregation.CompTrackerImpl;
import se.sics.ktoolbox.util.compare.WrapperComparator;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.network.basic.DecoratedHeader;
import se.sics.ktoolbox.util.other.AgingAdrContainer;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdate;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdatePort;
import se.sics.ktoolbox.util.update.View;

/**
 * Main Gradient class responsible for shuffling peer views with neighbors. It
 * is responsible for maintaining the gradient and returning periodically
 * gradient sample, to the application.
 * <p>
 */
public class GradientComp extends ComponentDefinition {

  private static final Logger LOG = LoggerFactory.getLogger(GradientComp.class);
  private String logPrefix = " ";

  //****************************CONNECTIONS***********************************
  Negative gradient = provides(GradientPort.class);
  Positive network = requires(Network.class);
  Positive timer = requires(Timer.class);
  Positive croupier = requires(CroupierPort.class);
  Negative croupierViewUpdate = provides(OverlayViewUpdatePort.class);
  Negative rankUpdate = provides(RankUpdatePort.class);
  Positive viewUpdate = requires(OverlayViewUpdatePort.class);
  //******************************CONFIG**************************************
  private final GradientKCWrapper gradientConfig;
  private final OverlayId overlayId;
  private GradientFilter filter;
  private final Comparator<GradientContainer> utilityComp;
  //******************************SELF****************************************
  private GradientContainer selfView;
  //******************************STATE***************************************
  private final GradientView gradientNeighbours;
  //*******************************AUX****************************************
  private UUID shuffleCycleId;
  private UUID shuffleTimeoutId;
  //*****************************TRACKING*************************************
  private CompTracker compTracker;

  private final IdentifierFactory eventIds;
  private final IdentifierFactory msgIds;

  public GradientComp(Init init) {
    SystemKCWrapper systemConfig = new SystemKCWrapper(config());
    gradientConfig = new GradientKCWrapper(config());
    overlayId = init.overlayId;
    logPrefix = "<nid:" + systemConfig.id + ",oid:" + overlayId + "> ";
    LOG.info("{}initializing...", logPrefix);

    selfView = new GradientContainer(init.selfAdr, null, 0, Integer.MAX_VALUE);
    utilityComp = new WrapperComparator<>(init.utilityComparator);
    filter = init.gradientFilter;

    gradientNeighbours = new GradientView(new SystemKCWrapper(config()), gradientConfig,
      overlayId, logPrefix, init.utilityComparator, init.gradientFilter);

    this.eventIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.EVENT, Optional.of(systemConfig.seed));
    this.msgIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.MSG, Optional.of(systemConfig.seed));
    setCompTracker();

    subscribe(handleStart, control);
    subscribe(handleViewUpdate, viewUpdate);

    subscribe(handleCroupierSample, croupier);
    subscribe(handleShuffleRequest, network);
    subscribe(handleShuffleResponse, network);
    subscribe(handleShuffleCycle, timer);
    subscribe(handleShuffleTimeout, timer);
  }

  private boolean connected() {
    if (gradientNeighbours.isEmpty()) {
      LOG.warn("{}no partners - not shuffling", new Object[]{logPrefix});
      return false;
    }
    return true;
  }

  private boolean ready() {
    if (selfView.getContent() == null) {
      LOG.warn("{}no self view", logPrefix);
      return false;
    }
    return true;
  }

  private boolean canShuffle() {
    if (!ready() || !connected()) {
      return false;
    }
    return true;
  }
  //*************************Control******************************************
  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      LOG.info("{} starting...", logPrefix);
      compTracker.start();
      schedulePeriodicShuffle();
    }
  };

  Handler handleViewUpdate = new Handler<OverlayViewUpdate.Indication<View>>() {
    @Override
    public void handle(OverlayViewUpdate.Indication<View> viewUpdate) {
      LOG.debug("{} updating self view:{}", new Object[]{logPrefix, viewUpdate.view});
      if (ready() && filter.cleanOldView(selfView.getContent(), viewUpdate.view)) {
        gradientNeighbours.clean(viewUpdate.view);
      }
      selfView = selfView.changeView(viewUpdate.view);
      OverlayId croupierId = overlayId.changeType(OverlayId.BasicTypes.CROUPIER);
      trigger(new OverlayViewUpdate.Indication(eventIds.randomId(), croupierId, false, new GradientLocalView(viewUpdate.view, selfView.rank)),
        croupierViewUpdate);
    }
  };

  //***************************STATE TRACKING*********************************
  private void setCompTracker() {

    switch (gradientConfig.gradientAggLevel) {
      case NONE:
        compTracker = new CompTrackerImpl(proxy, Pair.with(LOG, logPrefix), gradientConfig.gradientAggPeriod);
        break;
      case BASIC:
        compTracker = new CompTrackerImpl(proxy, Pair.with(LOG, logPrefix), gradientConfig.gradientAggPeriod);
        setEventTracking();
        break;
      case FULL:
        compTracker = new CompTrackerImpl(proxy, Pair.with(LOG, logPrefix), gradientConfig.gradientAggPeriod);
        setEventTracking();
        setStateTracking();
        break;
      default:
        throw new RuntimeException("Undefined:" + gradientConfig.gradientAggLevel);
    }
  }

  private void setEventTracking() {
    compTracker.registerPositivePort(network);
    compTracker.registerPositivePort(timer);
    compTracker.registerNegativePort(gradient);
    compTracker.registerNegativePort(rankUpdate);
    compTracker.registerPositivePort(croupier);
    compTracker.registerPositivePort(viewUpdate);
  }

  private void setStateTracking() {
    compTracker.registerReducer(new GradientViewReducer());
  }
  //**************************************************************************
  /**
   * Samples from Croupier used for bootstrapping gradient as well as faster
   * convergence(random samples)
   */
  Handler handleCroupierSample = new Handler<CroupierSample<GradientLocalView>>() {
    @Override
    public void handle(CroupierSample<GradientLocalView> sample) {
      LOG.trace("{}{}", logPrefix, sample);
      LOG.debug("{} \nCroupier public sample:{} \nCroupier private sample:{}",
        new Object[]{logPrefix, sample.publicSample, sample.privateSample});
      if (!ready()) {
        return;
      }
      Set<GradientContainer> gradientCopy = new HashSet<GradientContainer>();
      for (AgingAdrContainer<KAddress, GradientLocalView> container : sample.publicSample.values()) {
        int age = container.getAge();
        gradientCopy.add(new GradientContainer(container.getSource(), container.getContent().appView, age, container.
          getContent().rank));
      }
      for (AgingAdrContainer<KAddress, GradientLocalView> container : sample.privateSample.values()) {
        int age = container.getAge();
        gradientCopy.add(new GradientContainer(container.getSource(), container.getContent().appView, age, container.
          getContent().rank));
      }
      gradientNeighbours.merge(gradientCopy, selfView);
    }
  };

  private void sendShuffleRequest(KAddress to, List<GradientContainer> exchangeGC) {
    DecoratedHeader requestHeader
      = new DecoratedHeader(new BasicHeader(selfView.getSource(), to, Transport.UDP), overlayId);
    GradientShuffle.Request requestContent
      = new GradientShuffle.Request(msgIds.randomId(), overlayId, selfView, exchangeGC);
    BasicContentMsg request = new BasicContentMsg(requestHeader, requestContent);
    LOG.trace("{}sending:{} to:{}", new Object[]{logPrefix, requestContent, request.getDestination()});
    trigger(request, network);
  }

  private void sendShuffleResponse(BasicContentMsg<?, ?, GradientShuffle.Request> req,
    List<GradientContainer> exchangeGC) {
    GradientShuffle.Response responseContent = req.getContent().answer(selfView, exchangeGC);
    BasicContentMsg response = req.answer(responseContent);
    LOG.trace("{}sending:{} to:{}", new Object[]{logPrefix, responseContent, response.getDestination()});
    trigger(response, network);
  }

  /**
   * Periodic Gradient Shuffle
   */
  Handler<ShuffleCycle> handleShuffleCycle = new Handler<ShuffleCycle>() {
    @Override
    public void handle(ShuffleCycle event) {
      LOG.trace("{}{}", logPrefix, event);
      if (!canShuffle()) {
        return;
      }

      updateRank();
      publishSample();

      gradientNeighbours.incrementAges();
      GradientContainer partner = gradientNeighbours.getShuffleNode(selfView);
      List<GradientContainer> exchangeGC = gradientNeighbours.getExchangeCopy(partner, gradientConfig.shuffleSize);
      sendShuffleRequest(partner.getSource(), exchangeGC);
      scheduleShuffleTimeout(partner.getSource());
    }
  };

  private void updateRank() {
    int rank = gradientNeighbours.rank(selfView);
    if (rank != selfView.rank) {
      LOG.trace("{}updated rank from:{} to:{}", new Object[]{logPrefix, selfView.rank, rank});
      selfView = selfView.changeRank(rank);

      OverlayId croupierId = overlayId.changeType(OverlayId.BasicTypes.CROUPIER);
      trigger(new OverlayViewUpdate.Indication(eventIds.randomId(), croupierId, false,
        new GradientLocalView(selfView.getContent(), selfView.rank)),
        croupierViewUpdate);
      trigger(new RankUpdate(eventIds.randomId(), overlayId, selfView.rank), rankUpdate);
    }
  }

  private void publishSample() {
    GradientSample publishedSample = new GradientSample(eventIds.randomId(), overlayId, selfView.getContent(),
      gradientNeighbours.getAllCopy());
    if (publishedSample.gradientNeighbours.isEmpty()) {
      LOG.warn("{}no neighbours", logPrefix);
    }
    LOG.info("{}view:{}", logPrefix, publishedSample.gradientNeighbours);
    trigger(publishedSample, gradient);
    compTracker.updateState(new GradientViewPacket(publishedSample));
  }

  Handler<ShuffleTimeout> handleShuffleTimeout = new Handler<ShuffleTimeout>() {

    @Override
    public void handle(ShuffleTimeout event) {
      LOG.trace("{} {}", logPrefix, event);
      if (shuffleTimeoutId == null) {
        LOG.debug("{} late timeout {}", logPrefix, event);
        return;
      } else {
        LOG.debug("{} node:{} timed out", logPrefix, event.dest);
        shuffleTimeoutId = null;
        gradientNeighbours.clean(event.dest);
      }
    }
  };

  ClassMatchedHandler handleShuffleRequest
    = new ClassMatchedHandler<GradientShuffle.Request, BasicContentMsg<KAddress, DecoratedHeader<KAddress>, GradientShuffle.Request>>() {

    @Override
    public void handle(GradientShuffle.Request content,
      BasicContentMsg<KAddress, DecoratedHeader<KAddress>, GradientShuffle.Request> container) {
      DecoratedHeader<KAddress> header = container.getHeader();
      KAddress reqSrc = container.getHeader().getSource();
      LOG.trace("{}received:{} from:{}", new Object[]{logPrefix, container, reqSrc});

      if (!ready()) {
        return;
      }
      if (selfView.getSource().getId().equals(reqSrc.getId())) {
        LOG.error("{} Tried to shuffle with myself", logPrefix);
        throw new RuntimeException("tried to shuffle with myself");
      }

      gradientNeighbours.incrementAges();
      List<GradientContainer> exchangeGC = gradientNeighbours.
        getExchangeCopy(content.selfGC, gradientConfig.shuffleSize);
      sendShuffleResponse(container, exchangeGC);

      gradientNeighbours.merge(content.exchangeGC, selfView);
      gradientNeighbours.merge(content.selfGC, selfView);
    }
  };

  ClassMatchedHandler handleShuffleResponse
    = new ClassMatchedHandler<GradientShuffle.Response, BasicContentMsg<KAddress, DecoratedHeader<KAddress>, GradientShuffle.Response>>() {

    @Override
    public void handle(GradientShuffle.Response content,
      BasicContentMsg<KAddress, DecoratedHeader<KAddress>, GradientShuffle.Response> container) {
      DecoratedHeader<KAddress> header = container.getHeader();
      KAddress shufflePartner = container.getHeader().getSource();

      LOG.debug("{}received:{} from:{}", new Object[]{logPrefix, container, shufflePartner});
      if (selfView.getSource().getId().equals(shufflePartner.getId())) {
        LOG.error("{} Tried to shuffle with myself", logPrefix);
        throw new RuntimeException("tried to shuffle with myself");
      }

      if (shuffleTimeoutId == null) {
        LOG.debug("{} req:{} already timed out", new Object[]{logPrefix, content.msgId, shufflePartner});
        return;
      }
      cancelShuffleTimeout();

      gradientNeighbours.merge(content.exchangeGC, selfView);
      gradientNeighbours.merge(content.selfGC, selfView);
    }
  };

  private void schedulePeriodicShuffle() {
    if (shuffleCycleId != null) {
      LOG.warn("{} double starting periodic shuffle", logPrefix);
      return;
    }
    SchedulePeriodicTimeout spt
      = new SchedulePeriodicTimeout(gradientConfig.shufflePeriod, gradientConfig.shufflePeriod);
    ShuffleCycle sc = new ShuffleCycle(spt, overlayId);
    spt.setTimeoutEvent(sc);
    shuffleCycleId = sc.getTimeoutId();
    trigger(spt, timer);
  }

  private void cancelPeriodicShuffle() {
    if (shuffleCycleId == null) {
      LOG.warn("{} double stopping periodic shuffle", logPrefix);
      return;
    }
    CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(shuffleCycleId);
    shuffleCycleId = null;
    trigger(cpt, timer);
  }

  private void scheduleShuffleTimeout(KAddress dest) {
    if (shuffleTimeoutId != null) {
      LOG.warn("{} double starting shuffle timeout", logPrefix);
      return;
    }
    ScheduleTimeout spt = new ScheduleTimeout(gradientConfig.shufflePeriod / 2);
    ShuffleTimeout sc = new ShuffleTimeout(spt, dest, overlayId);
    spt.setTimeoutEvent(sc);
    shuffleTimeoutId = sc.getTimeoutId();
    trigger(spt, timer);
  }

  private void cancelShuffleTimeout() {
    if (shuffleTimeoutId == null) {
      LOG.warn("{} double stopping shuffle timeout", logPrefix);
    }
    CancelTimeout cpt = new CancelTimeout(shuffleTimeoutId);
    shuffleTimeoutId = null;
    trigger(cpt, timer);
  }

  public static class ShuffleCycle extends Timeout {

    public final OverlayId overlayId;

    public ShuffleCycle(SchedulePeriodicTimeout request, OverlayId overlayId) {
      super(request);
      this.overlayId = overlayId;
    }

    @Override
    public String toString() {
      return "Gradient<" + overlayId + ">ShuffleCycle<" + getTimeoutId() + ">";
    }
  }

  public static class ShuffleTimeout extends Timeout {

    public final KAddress dest;
    public final OverlayId overlayId;

    public ShuffleTimeout(ScheduleTimeout request, KAddress dest, OverlayId overlayId) {
      super(request);
      this.dest = dest;
      this.overlayId = overlayId;
    }

    @Override
    public String toString() {
      return "Gradient<" + overlayId + ">ShuffleTimeout<" + getTimeoutId() + ">";
    }
  }

  public static class Init extends se.sics.kompics.Init<GradientComp> {

    public final KAddress selfAdr;
    public final OverlayId overlayId;
    public final Comparator utilityComparator;
    public final GradientFilter gradientFilter;

    public Init(KAddress selfAdr, OverlayId overlayId, Comparator utilityComparator, GradientFilter gradientFilter) {
      this.selfAdr = selfAdr;
      this.overlayId = overlayId;
      this.utilityComparator = utilityComparator;
      this.gradientFilter = gradientFilter;
    }
  }
}

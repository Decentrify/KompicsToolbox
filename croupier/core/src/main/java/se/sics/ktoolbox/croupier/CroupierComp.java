/**
 * This file is part of the Kompics P2P Framework.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.ktoolbox.croupier;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.croupier.aggregation.CroupierViewPacket;
import se.sics.ktoolbox.croupier.aggregation.CroupierViewReducer;
import se.sics.ktoolbox.croupier.aggregation.SelfViewPacket;
import se.sics.ktoolbox.croupier.aggregation.SelfViewReducer;
import se.sics.ktoolbox.croupier.behaviour.CroupierBehaviour;
import se.sics.ktoolbox.croupier.behaviour.CroupierObserver;
import se.sics.ktoolbox.croupier.event.CroupierDisconnected;
import se.sics.ktoolbox.croupier.event.CroupierJoin;
import se.sics.ktoolbox.croupier.event.CroupierSample;
import se.sics.ktoolbox.croupier.msg.CroupierShuffle;
import se.sics.ktoolbox.croupier.view.LocalView;
import se.sics.ktoolbox.nutil.timer.TimerProxy;
import se.sics.ktoolbox.nutil.timer.TimerProxyImpl;
import se.sics.ktoolbox.util.aggregation.CompTracker;
import se.sics.ktoolbox.util.aggregation.CompTrackerImpl;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.network.basic.DecoratedHeader;
import se.sics.ktoolbox.util.network.nat.NatAwareAddress;
import se.sics.ktoolbox.util.network.nat.NatType;
import se.sics.ktoolbox.util.other.AdrContainer;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdate;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdatePort;
import se.sics.ktoolbox.util.overlays.view.archive.BootstrapView;
import se.sics.ktoolbox.util.update.View;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierComp extends ComponentDefinition {

  private static final int maxRebootstrap = 10;

  // INFO - status, memory statistics 
  // DEBUG - bootstraping, samples 
  // TRACE - protocol messages
  //****************************CONNECTIONS***********************************
  Negative<CroupierControlPort> croupierControlPort = negative(CroupierControlPort.class);
  Negative<CroupierPort> croupierPort = negative(CroupierPort.class);
  Positive<CroupierPort> bootstrapPort = positive(CroupierPort.class);
  Positive<Network> network = requires(Network.class);
  Positive<Timer> timer = requires(Timer.class);
  Positive<OverlayViewUpdatePort> viewUpdate = requires(OverlayViewUpdatePort.class);
  private final TimerProxy timerProxy;
  //******************************CONFIG**************************************
  private final SystemKCWrapper systemConfig;
  private final CroupierKCWrapper croupierConfig;
  private final OverlayId overlayId;
  private final NatAwareAddress selfAdr;
  //******************************SELF****************************************
  private CroupierBehaviour behaviour;
  //******************************STATE***************************************
  private final Random rand;
  private final List<NatAwareAddress> bootstrapNodes = new ArrayList<>();
  private final LocalView publicView;
  private final LocalView privateView;
  //*******************************AUX****************************************
  private UUID shuffleCycleTid = null;
  private UUID shuffleTid = null;
  //*****************************TRACKING*************************************
  private CompTracker compTracker;
  private final IdentifierFactory eventIds;
  private final IdentifierFactory msgIds;

  public CroupierComp(Init init) {
    systemConfig = new SystemKCWrapper(config());
    croupierConfig = new CroupierKCWrapper(config());
    overlayId = init.overlayId;
    loggingCtxPutAlways("nId", init.selfAdr.getId().toString());
    loggingCtxPutAlways("oId", overlayId.toString());
    eventIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.EVENT,
      java.util.Optional.of(systemConfig.seed));
    msgIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.MSG,
      java.util.Optional.of(systemConfig.seed));
    timerProxy = new TimerProxyImpl();
    selfAdr = init.selfAdr;
    behaviour = new CroupierObserver();
    rand = new Random(systemConfig.seed + overlayId.partition(Integer.MAX_VALUE));
    publicView = new LocalView(croupierConfig, rand);
    privateView = new LocalView(croupierConfig, rand);

    setCompTracker();

    subscribe(handleStart, control);
    subscribe(handleLegacyBootstrap, croupierControlPort);
    subscribe(handleReBootstrap, bootstrapPort);
    subscribe(handleViewUpdate, viewUpdate);
    subscribe(handleShuffleRequest, network);
    subscribe(handleShuffleResponse, network);
  }

  private boolean connected() {
    if (bootstrapNodes.isEmpty() && publicView.isEmpty() && privateView.isEmpty()) {
      logger.warn("no partners - not shuffling");
      //TODO Alex Disconnected -  legacy code
      trigger(new CroupierDisconnected(eventIds.randomId(), overlayId), croupierControlPort);
      return false;
    }
    return true;
  }

  private boolean ready() {
    if (!behaviour.getView().isPresent()) {
      logger.warn("no self view");
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

  //***************************STATE TRACKING*********************************
  private void setCompTracker() {
    switch (croupierConfig.croupierAggLevel) {
      case NONE:
        compTracker = new CompTrackerImpl(proxy, Pair.with(logger, ""), croupierConfig.croupierAggPeriod);
        break;
      case BASIC:
        compTracker = new CompTrackerImpl(proxy, Pair.with(logger, ""), croupierConfig.croupierAggPeriod);
        setEventTracking();
        break;
      case FULL:
        compTracker = new CompTrackerImpl(proxy, Pair.with(logger, ""), croupierConfig.croupierAggPeriod);
        setEventTracking();
        setStateTracking();
        break;
      default:
        throw new RuntimeException("Undefined:" + croupierConfig.croupierAggLevel);
    }
  }

  private void setEventTracking() {
    compTracker.registerPositivePort(network);
    compTracker.registerPositivePort(timer);
    compTracker.registerNegativePort(croupierPort);
    compTracker.registerNegativePort(croupierControlPort);
    compTracker.registerPositivePort(bootstrapPort);
    compTracker.registerPositivePort(viewUpdate);
  }

  private void setStateTracking() {
    compTracker.registerReducer(new CroupierViewReducer());
    compTracker.registerReducer(new SelfViewReducer());
  }
  //****************************CONTROL***************************************
  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      timerProxy.setup(proxy);
      long shuffleCycleDelay = croupierConfig.shufflePeriod;
      long shuffleCyclePeriod = croupierConfig.shufflePeriod;
      shuffleCycleTid = timerProxy.schedulePeriodicTimer(shuffleCycleDelay, shuffleCyclePeriod, shuffleCycleTimer());
      compTracker.start();
    }
  };

  @Override
  public void tearDown() {
    timerProxy.cancel();
  }

  private Consumer<Boolean> shuffleCycleTimer() {
    return (_ignore) -> {
      shuffleCycle();
    };
  }

  private void shuffleCycle() {
    logger.debug("shuffle cycle");
    if (!canShuffle()) {
      return;
    }
    publishSample();
    incrementAges();
    NatAwareAddress shufflePartner = selectPeerToShuffleWith();
    Map publicSample = publicView.initiatorSample(shufflePartner);
    Map privateSample = privateView.initiatorSample(shufflePartner);
    sendShuffleRequest(shufflePartner, publicSample, privateSample);

    long shuffleDelay = croupierConfig.shufflePeriod / 2;
    shuffleTid = timerProxy.scheduleTimer(shuffleDelay, shuffleTimer(shufflePartner));
  }

  private Consumer<Boolean> shuffleTimer(NatAwareAddress dest) {
    return (_ignore) -> {
      logger.debug("node:{} timed out", dest);

      //TODO Alex - maybe put in a dead box and try again later
      if (NatType.isOpen(dest)) {
        publicView.timedOut(dest);
      } else {
        privateView.timedOut(dest);
      }
      bootstrapNodes.remove(dest);
      if (!connected()) {
        trigger(new CroupierDisconnected(eventIds.randomId(), overlayId), croupierControlPort);
      }
    };
  }

  Handler handleViewUpdate = new Handler<OverlayViewUpdate.Indication<View>>() {
    @Override
    public void handle(OverlayViewUpdate.Indication<View> viewUpdate) {
      logger.debug("update observer:{} view:{}", new Object[]{viewUpdate.observer, viewUpdate.view});
      behaviour = behaviour.processView(viewUpdate);
      compTracker.updateState(new SelfViewPacket(viewUpdate.view));
    }
  };

  Handler handleLegacyBootstrap = new Handler<CroupierJoin>() {
    @Override
    public void handle(CroupierJoin join) {
      if (bootstrapNodes.size() > maxRebootstrap) {
        logger.debug("still have bootstrap nodes");
        return;
      }
      logger.info("bootstraping with:{}", new Object[]{join.bootstrap});
      for (NatAwareAddress bootstrap : join.bootstrap) {
        if (!checkIfSelf(bootstrap)) {
          bootstrapNodes.add(bootstrap);
        }
      }
    }
  };

  Handler handleReBootstrap = new Handler<CroupierSample<BootstrapView>>() {
    @Override
    public void handle(CroupierSample<BootstrapView> sample) {
      while (bootstrapNodes.size() < croupierConfig.viewSize) {
        for (AdrContainer<KAddress, BootstrapView> container : sample.publicSample.values()) {
          if (container.getContent().memberOf(overlayId)
            && !checkIfSelf(container.getSource())) {
            logger.debug("rebootstrap node:{}", container.getSource());
            bootstrapNodes.add((NatAwareAddress) container.getSource());
          }
        }
      }
    }
  };

  private boolean checkIfSelf(KAddress adr) {
    if (adr.getId().equals(selfAdr.getId())) {
      return true;
    }
    if (adr.getIp().equals(selfAdr.getIp()) && adr.getPort() == selfAdr.getPort()) {
      return true;
    }
    return false;
  }

  //**************************************************************************
  private void sendShuffleRequest(NatAwareAddress to, Map publicSample, Map privateSample) {
    DecoratedHeader requestHeader
      = new DecoratedHeader(new BasicHeader(selfAdr, to, Transport.UDP), overlayId);
    CroupierShuffle.Request requestContent = new CroupierShuffle.Request(msgIds.randomId(), overlayId,
      behaviour.getView(), publicSample, privateSample);
    BasicContentMsg request = new BasicContentMsg(requestHeader, requestContent);
    logger.trace("sending:{} to:{}", new Object[]{requestContent, request.getDestination()});
    trigger(request, network);
  }

  private void sendShuffleResponse(BasicContentMsg<?, ?, CroupierShuffle.Request> req, Map publicSample,
    Map privateSample) {
    CroupierShuffle.Response responseContent = req.getContent().answer(behaviour.getView(), publicSample, privateSample);
    BasicContentMsg response = req.answer(responseContent);
    logger.trace("sending:{} to:{}", new Object[]{responseContent, response.getDestination()});
    trigger(response, network);
  }

  ClassMatchedHandler handleShuffleRequest
    = new ClassMatchedHandler<CroupierShuffle.Request, BasicContentMsg<NatAwareAddress, DecoratedHeader<NatAwareAddress>, CroupierShuffle.Request>>() {

    @Override
    public void handle(CroupierShuffle.Request content,
      BasicContentMsg<NatAwareAddress, DecoratedHeader<NatAwareAddress>, CroupierShuffle.Request> container) {
      NatAwareAddress shufflePartner = container.getSource();
      logger.trace("received:{} from:{}", new Object[]{content, shufflePartner});
      if (!ready()) {
        return;
      }
      if (selfAdr.getId().equals(shufflePartner.getId())) {
        logger.warn("tried to shuffle with myself");
        //TODO Alex - what if this is a different shuffle timer
        timerProxy.cancelTimer(shuffleTid);
        bootstrapNodes.remove(container.getDestination());
        return;
      }
      Map publicSample = publicView.receiverSample(shufflePartner);
      Map privateSample = privateView.receiverSample(shufflePartner);
      sendShuffleResponse(container, publicSample, privateSample);
      retainSamples(shufflePartner, content.selfView, content.publicNodes, content.privateNodes);
    }
  };

  ClassMatchedHandler handleShuffleResponse
    = new ClassMatchedHandler<CroupierShuffle.Response, BasicContentMsg<NatAwareAddress, DecoratedHeader<NatAwareAddress>, CroupierShuffle.Response>>() {

    @Override
    public void handle(CroupierShuffle.Response content,
      BasicContentMsg<NatAwareAddress, DecoratedHeader<NatAwareAddress>, CroupierShuffle.Response> container) {
      NatAwareAddress shufflePartner = container.getSource();
      if (shuffleTid == null) {
        logger.debug("shuffle:{} from:{} already timed out", new Object[]{content, shufflePartner});
        return;
      }
      logger.trace("received:{} from:{}", new Object[]{content, shufflePartner});
      //TODO Alex - what if this is a different shuffle timer
      timerProxy.cancelTimer(shuffleTid);
      retainSamples(shufflePartner, content.selfView, content.publicNodes, content.privateNodes);
    }
  };

  private void retainSamples(NatAwareAddress partnerAdr, Optional<View> partnerView,
    Map publicSample, Map privateSample) {
    Triplet<NatAwareAddress, View, Boolean> retainPublic;
    Triplet<NatAwareAddress, View, Boolean> retainPrivate;
    if (!partnerView.isPresent()) {
      retainPublic = Triplet.with(partnerAdr, null, false);
      retainPrivate = Triplet.with(partnerAdr, null, false);
    } else {
      if (NatType.isOpen(partnerAdr)) {
        retainPublic = Triplet.with(partnerAdr, partnerView.get(), true);
        retainPrivate = Triplet.with(partnerAdr, null, false);
      } else {
        retainPublic = Triplet.with(partnerAdr, null, false);
        retainPrivate = Triplet.with(partnerAdr, partnerView.get(), true);
      }
    }
    publicView.selectToKeep(selfAdr, retainPublic, publicSample);
    privateView.selectToKeep(selfAdr, retainPrivate, privateSample);
  }

  //**************************************************************************
  private List<NatAwareAddress> copyCleanSelf(List<NatAwareAddress> addresses) {
    List<NatAwareAddress> result = new ArrayList<>();
    Iterator<NatAwareAddress> it = addresses.iterator();
    while (it.hasNext()) {
      NatAwareAddress node = it.next();
      if (!node.getId().equals(selfAdr.getId())) {
        result.add(node);
      }
    }
    return result;
  }

  private NatAwareAddress selectPeerToShuffleWith() {
    //we want to rebootstrap from time to time even when we have a full view...so we have a chance between [0.1, 1] (actually 1.1..but ok)
    double rebootstrapChance = 0.1 + (double) (croupierConfig.viewSize - publicView.size()) / croupierConfig.viewSize;
    if (rand.nextDouble() < rebootstrapChance) {
      if (!bootstrapNodes.isEmpty()) {
        return bootstrapNodes.remove(0);
      }
    }
    NatAwareAddress node = null;
    if (!publicView.isEmpty()) {
      node = publicView.selectPeerToShuffleWith();
    } else if (!privateView.isEmpty()) {
      node = privateView.selectPeerToShuffleWith();
    }
    return node;
  }

  private void incrementAges() {
    publicView.incrementAges();
    privateView.incrementAges();
  }

  private void publishSample() {
    CroupierSample publishedSample = new CroupierSample(eventIds.randomId(), overlayId,
      publicView.publish(), privateView.publish());
    if (publishedSample.publicSample.isEmpty()) {
      if (publishedSample.privateSample.isEmpty()) {
        logger.warn("no neighbours");
      } else {
        logger.warn("no public neighbours");
      }
    }
    logger.info("publishing public nodes:{}", new Object[]{publishedSample.publicSample});
    logger.info("publishing private nodes:{}", new Object[]{publishedSample.privateSample});
    trigger(publishedSample, croupierPort);
    compTracker.updateState(new CroupierViewPacket(publishedSample));

  }

  public static class Init extends se.sics.kompics.Init<CroupierComp> {

    public final NatAwareAddress selfAdr;
    public final OverlayId overlayId;

    public Init(NatAwareAddress selfAdr, OverlayId overlayId) {
      this.selfAdr = selfAdr;
      this.overlayId = overlayId;
    }
  }
}

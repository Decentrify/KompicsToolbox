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
import org.javatuples.Pair;
import org.javatuples.Triplet;
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
    private final static Logger LOG = LoggerFactory.getLogger(CroupierComp.class);
    private String logPrefix = "";

    //****************************CONNECTIONS***********************************
    Negative<CroupierControlPort> croupierControlPort = negative(CroupierControlPort.class);
    Negative<CroupierPort> croupierPort = negative(CroupierPort.class);
    Positive<CroupierPort> bootstrapPort = positive(CroupierPort.class);
    Positive<Network> network = requires(Network.class);
    Positive<Timer> timer = requires(Timer.class);
    Positive<OverlayViewUpdatePort> viewUpdate = requires(OverlayViewUpdatePort.class);
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
    private Pair<UUID, Long> shuffleCycle = Pair.with(null, 0l);
    private UUID shuffleTid;
    //*****************************TRACKING*************************************
    private CompTracker compTracker;
    private final IdentifierFactory eventIds;
    private final IdentifierFactory msgIds;

    public CroupierComp(Init init) {
        systemConfig = new SystemKCWrapper(config());
        croupierConfig = new CroupierKCWrapper(config());
        overlayId = init.overlayId;
        logPrefix = "<nid:" + systemConfig.id + ",oid:" + overlayId + "> ";
        LOG.info("{}initiating...", logPrefix);
        eventIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.EVENT, 
          java.util.Optional.of(systemConfig.seed));
        msgIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.MSG, 
          java.util.Optional.of(systemConfig.seed));

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
        subscribe(handleShuffleCycle, timer);
        subscribe(handleShuffleTimeout, timer);
    }

    private boolean connected() {
        if (bootstrapNodes.isEmpty() && publicView.isEmpty() && privateView.isEmpty()) {
            LOG.warn("{}no partners - not shuffling", new Object[]{logPrefix});
            //TODO Alex Disconnected -  legacy code
            trigger(new CroupierDisconnected(eventIds.randomId(), overlayId), croupierControlPort);
            return false;
        }
        return true;
    }

    private boolean ready() {
        if (!behaviour.getView().isPresent()) {
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

    //***************************STATE TRACKING*********************************
    private void setCompTracker() {
        switch (croupierConfig.croupierAggLevel) {
            case NONE:
                compTracker = new CompTrackerImpl(proxy, Pair.with(LOG, logPrefix), croupierConfig.croupierAggPeriod);
                break;
            case BASIC:
                compTracker = new CompTrackerImpl(proxy, Pair.with(LOG, logPrefix), croupierConfig.croupierAggPeriod);
                setEventTracking();
                break;
            case FULL:
                compTracker = new CompTrackerImpl(proxy, Pair.with(LOG, logPrefix), croupierConfig.croupierAggPeriod);
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
            LOG.info("{}starting...", logPrefix);
            schedulePeriodicShuffle(croupierConfig.shufflePeriod);
            compTracker.start();
        }
    };

    Handler handleViewUpdate = new Handler<OverlayViewUpdate.Indication<View>>() {
        @Override
        public void handle(OverlayViewUpdate.Indication<View> viewUpdate) {
            LOG.debug("{}update observer:{} view:{}", new Object[]{logPrefix, viewUpdate.observer, viewUpdate.view});
            behaviour = behaviour.processView(viewUpdate);
            compTracker.updateState(new SelfViewPacket(viewUpdate.view));
        }
    };

    Handler handleLegacyBootstrap = new Handler<CroupierJoin>() {
        @Override
        public void handle(CroupierJoin join) {
            if (bootstrapNodes.size() > maxRebootstrap) {
                LOG.debug("{}still have bootstrap nodes");
                return;
            }
            LOG.info("{}bootstraping with:{}", new Object[]{logPrefix, join.bootstrap});
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
                        LOG.debug("{}rebootstrap node:{}", logPrefix, container.getSource());
                        bootstrapNodes.add((NatAwareAddress) container.getSource());
                    }
                }
            }
        }
    };
    
    private boolean checkIfSelf(KAddress adr) {
        if(adr.getId().equals(selfAdr.getId())) {
            return true;
        }
        if(adr.getIp().equals(selfAdr.getIp()) && adr.getPort() == selfAdr.getPort()) {
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
        LOG.trace("{}sending:{} to:{}", new Object[]{logPrefix, requestContent, request.getDestination()});
        trigger(request, network);
    }

    private void sendShuffleResponse(BasicContentMsg<?, ?, CroupierShuffle.Request> req, Map publicSample, Map privateSample) {
        CroupierShuffle.Response responseContent = req.getContent().answer(behaviour.getView(), publicSample, privateSample);
        BasicContentMsg response = req.answer(responseContent);
        LOG.trace("{}sending:{} to:{}", new Object[]{logPrefix, responseContent, response.getDestination()});
        trigger(response, network);
    }
    Handler<ShuffleCycle> handleShuffleCycle = new Handler<ShuffleCycle>() {
        @Override
        public void handle(ShuffleCycle event) {
            LOG.trace("{}{}", logPrefix, event);
            if (!canShuffle()) {
                return;
            }
            publishSample();
            incrementAges();
            NatAwareAddress shufflePartner = selectPeerToShuffleWith();
            Map publicSample = publicView.initiatorSample(shufflePartner);
            Map privateSample = privateView.initiatorSample(shufflePartner);
            sendShuffleRequest(shufflePartner, publicSample, privateSample);
            scheduleShuffleTimeout(shufflePartner);
        }
    };

    ClassMatchedHandler handleShuffleRequest
            = new ClassMatchedHandler<CroupierShuffle.Request, BasicContentMsg<NatAwareAddress, DecoratedHeader<NatAwareAddress>, CroupierShuffle.Request>>() {

                @Override
                public void handle(CroupierShuffle.Request content, BasicContentMsg<NatAwareAddress, DecoratedHeader<NatAwareAddress>, CroupierShuffle.Request> container) {
                    NatAwareAddress shufflePartner = container.getSource();
                    LOG.trace("{}received:{} from:{}", new Object[]{logPrefix, content, shufflePartner});
                    if (!ready()) {
                        return;
                    }
                    if (selfAdr.getId().equals(shufflePartner.getId())) {
                        LOG.warn("{} Tried to shuffle with myself", logPrefix);
                        cancelShuffleTimeout();
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
                public void handle(CroupierShuffle.Response content, BasicContentMsg<NatAwareAddress, DecoratedHeader<NatAwareAddress>, CroupierShuffle.Response> container) {
                    NatAwareAddress shufflePartner = container.getSource();
                    if (shuffleTid == null) {
                        LOG.debug("{}shuffle:{} from:{} already timed out", new Object[]{logPrefix, content, shufflePartner});
                        return;
                    }
                    LOG.trace("{}received:{} from:{}", new Object[]{logPrefix, content, shufflePartner});
                    cancelShuffleTimeout();
                    retainSamples(shufflePartner, content.selfView, content.publicNodes, content.privateNodes);
                }
            };

    Handler<ShuffleTimeout> handleShuffleTimeout = new Handler<ShuffleTimeout>() {
        @Override
        public void handle(ShuffleTimeout timeout) {
            if (shuffleTid == null || !shuffleTid.equals(timeout.getTimeoutId())) {
                //late timeout
                return;
            }
            LOG.debug("{}node:{} timed out", logPrefix, timeout.dest);
            shuffleTid = null;

            //TODO Alex - maybe put in a dead box and try again later
            if (NatType.isOpen(timeout.dest)) {
                publicView.timedOut(timeout.dest);
            } else {
                privateView.timedOut(timeout.dest);
            }
            bootstrapNodes.remove(timeout.dest);
            if (!connected()) {
                trigger(new CroupierDisconnected(eventIds.randomId(), overlayId), croupierControlPort);
            }
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
        double rebootstrapChance = 0.1 + (double)(croupierConfig.viewSize - publicView.size())/croupierConfig.viewSize;
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
                LOG.warn("{}no neighbours", logPrefix);
            } else {
                LOG.warn("{}no public neighbours", logPrefix);
            }
        }
        LOG.info("{}publishing public nodes:{}", new Object[]{logPrefix, publishedSample.publicSample});
        LOG.info("{}publishing private nodes:{}", new Object[]{logPrefix, publishedSample.privateSample});
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

    //********************CROUPIER_TIMEOUTS*************************************
    private void schedulePeriodicShuffle(long period) {
        if (shuffleCycle.getValue0() != null) {
            LOG.warn("{} double starting periodic shuffle", logPrefix);
            return;
        }
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(period, period);
        ShuffleCycle sc = new ShuffleCycle(spt, overlayId);
        spt.setTimeoutEvent(sc);
        trigger(spt, timer);

        shuffleCycle = shuffleCycle.setAt0(sc.getTimeoutId());
    }

    private void cancelPeriodicShuffle() {
        if (shuffleCycle.getValue0() == null) {
            LOG.warn("{} double stopping periodic shuffle", logPrefix);
            return;
        }
        CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(shuffleCycle.getValue0());
        trigger(cpt, timer);

        shuffleCycle.setAt0(null);
    }

    private static class ShuffleCycle extends Timeout {

        public final long period;
        public final OverlayId overlayId;
        
        ShuffleCycle(SchedulePeriodicTimeout request, OverlayId overlayId) {
            super(request);
            period = request.getPeriod();
            this.overlayId = overlayId;
        }

        @Override
        public String toString() {
            return "Croupier<" + overlayId + ">ShuffleCycle<" + getTimeoutId() + ">";
        }
    }

    private void scheduleShuffleTimeout(NatAwareAddress dest) {
        if (shuffleTid != null) {
            LOG.warn("{} double starting shuffle timeout", logPrefix);
        }
        ScheduleTimeout spt = new ScheduleTimeout(croupierConfig.shufflePeriod / 2);
        ShuffleTimeout sc = new ShuffleTimeout(spt, dest, overlayId);
        spt.setTimeoutEvent(sc);
        shuffleTid = sc.getTimeoutId();
        trigger(spt, timer);
    }

    private void cancelShuffleTimeout() {
        if (shuffleTid == null) {
            return;
        }
        CancelTimeout cpt = new CancelTimeout(shuffleTid);
        shuffleTid = null;
        trigger(cpt, timer);

    }

    private static class ShuffleTimeout extends Timeout {

        public final NatAwareAddress dest;
        public final OverlayId overlayId;

        ShuffleTimeout(ScheduleTimeout request, NatAwareAddress dest, OverlayId overlayId) {
            super(request);
            this.dest = dest;
            this.overlayId = overlayId;
        }

        @Override
        public String toString() {
            return "Croupier<" + overlayId + ">ShuffleTimeout<" + getTimeoutId() + ">";
        }
    }
}

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
package se.sics.ktoolbox.tgradient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import se.sics.kompics.util.Identifier;
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
import se.sics.ktoolbox.croupier.util.CroupierContainer;
import se.sics.ktoolbox.gradient.GradientFilter;
import se.sics.ktoolbox.gradient.GradientKCWrapper;
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.gradient.event.GradientSample;
import se.sics.ktoolbox.gradient.event.TGradientSample;
import se.sics.ktoolbox.gradient.msg.GradientShuffle;
import se.sics.ktoolbox.gradient.temp.RankUpdate;
import se.sics.ktoolbox.gradient.temp.RankUpdatePort;
import se.sics.ktoolbox.gradient.util.GradientContainer;
import se.sics.ktoolbox.gradient.util.GradientLocalView;
import se.sics.ktoolbox.gradient.util.ViewConfig;
import se.sics.ktoolbox.tgradient.util.TGParentView;
import se.sics.ktoolbox.util.aggregation.CompTracker;
import se.sics.ktoolbox.util.aggregation.CompTrackerImpl;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.network.basic.DecoratedHeader;
import se.sics.ktoolbox.util.network.nat.NatAwareAddress;
import se.sics.ktoolbox.util.other.AgingAdrContainer;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdate;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdatePort;
import se.sics.ktoolbox.util.update.View;

/**
 *
 * Main Gradient class responsible for shuffling peer views with neighbors. It
 * is responsible for maintaining the gradient and returning periodically
 * gradient sample, to the application.
 *
 */
public class TreeGradientComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(TreeGradientComp.class);
    private String logPrefix = " ";

    //****************************CONNECTIONS***********************************
    Negative tGradient = provides(GradientPort.class);
    Negative outCroupier = provides(CroupierPort.class);
    Negative gradientViewUpdate = provides(OverlayViewUpdatePort.class);
    Positive network = requires(Network.class);
    Positive timer = requires(Timer.class);
    Positive viewUpdate = requires(OverlayViewUpdatePort.class);
    Positive croupier = requires(CroupierPort.class);
    Positive gradient = requires(GradientPort.class);
    Positive rankUpdate = requires(RankUpdatePort.class);
    //******************************CONFIG**************************************
    private final GradientKCWrapper gradientConfig;
    private final TGradientKCWrapper tgradientConfig;
    private final OverlayId overlayId;
    private GradientFilter filter;
    //******************************SELF****************************************
    private GradientContainer selfView;
    //******************************STATE***************************************
    private final TGParentView gradientFingers;
    private List<GradientContainer> gradientNeighbours = new ArrayList<>();
    //*******************************AUX****************************************
    private UUID shuffleCycleId;
    private UUID shuffleTimeoutId;
    //*****************************TRACKING*************************************
    private CompTracker compTracker;

    public TreeGradientComp(Init init) {
        SystemKCWrapper systemConfig = new SystemKCWrapper(config());
        gradientConfig = new GradientKCWrapper(config());
        tgradientConfig = new TGradientKCWrapper(config());
        overlayId = init.overlayId;
        logPrefix = "<nid:" + systemConfig.id + ",oid:" + overlayId + "> ";
        filter = init.gradientFilter;
        LOG.info("{}initializing...", logPrefix);

        selfView = new GradientContainer(init.selfAdr, null, 0, Integer.MAX_VALUE);
        ViewConfig viewConfig = new ViewConfig(gradientConfig.viewSize, gradientConfig.softMaxTemp, gradientConfig.oldThreshold);
        gradientFingers = new TGParentView(overlayId, new SystemKCWrapper(config()), gradientConfig, tgradientConfig, logPrefix,
                init.gradientFilter);

        setCompTracker();

        subscribe(handleStart, control);
        subscribe(handleViewUpdate, viewUpdate);
        subscribe(handleRankUpdate, rankUpdate);

        subscribe(handleCroupierSample, croupier);
        subscribe(handleGradientSample, gradient);
        subscribe(handleShuffleRequest, network);
        subscribe(handleShuffleResponse, network);
        subscribe(handleShuffleCycle, timer);
        subscribe(handleShuffleTimeout, timer);
    }

    private boolean connected() {
        if (gradientFingers.isEmpty() && gradientNeighbours.isEmpty()) {
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
        if (selfView.rank == Integer.MAX_VALUE) {
            LOG.warn("{}no rank yet", logPrefix);
            return false;
        }
        return true;
    }
    //*********Control**********************************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            compTracker.start();
            schedulePeriodicShuffle();
        }
    };

    Handler handleViewUpdate = new Handler<OverlayViewUpdate.Indication<View>>() {
        @Override
        public void handle(OverlayViewUpdate.Indication<View> update) {
            LOG.info("{}update self view:{}", logPrefix, update.view);
            if (selfView.getContent() != null && filter.cleanOldView(update.view, selfView.getContent())) {
                gradientNeighbours.clear();
                gradientFingers.clean(update.view);
            }
            selfView = selfView.changeView(update.view);

            OverlayId gradientId = overlayId.changeType(OverlayId.BasicTypes.GRADIENT);
            trigger(update.changeOverlay(gradientId), gradientViewUpdate);
        }
    };

    Handler handleRankUpdate = new Handler<RankUpdate>() {
        @Override
        public void handle(RankUpdate update) {
            LOG.trace("{}{}", logPrefix, update);
            selfView = selfView.changeRank(update.rank);
        }
    };

    //***************************STATE TRACKING*********************************
    private void setCompTracker() {
        switch (tgradientConfig.tgradientAggLevel) {
            case NONE:
                compTracker = new CompTrackerImpl(proxy, Pair.with(LOG, logPrefix), tgradientConfig.tgradientAggPeriod);
                break;
            case BASIC:
                compTracker = new CompTrackerImpl(proxy, Pair.with(LOG, logPrefix), tgradientConfig.tgradientAggPeriod);
                setEventTracking();
                break;
            case FULL:
                compTracker = new CompTrackerImpl(proxy, Pair.with(LOG, logPrefix), tgradientConfig.tgradientAggPeriod);
                setEventTracking();
                setStateTracking();
                break;
            default:
                throw new RuntimeException("Undefined:" + tgradientConfig.tgradientAggLevel);
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
    }
    //**************************************************************************
    /**
     * Samples from Croupier used for bootstrapping gradient as well as faster
     * convergence(random samples)
     */
    Handler handleCroupierSample = new Handler<CroupierSample<GradientLocalView>>() {
        @Override
        public void handle(CroupierSample<GradientLocalView> sample) {
            LOG.debug("{}{}", logPrefix, sample);
            LOG.debug("{}\nCroupier public sample:{}\nCroupier private sample:{}",
                    new Object[]{logPrefix, sample.publicSample, sample.privateSample});
            if (!ready()) {
                return;
            }
            Set<GradientContainer> croupierCopy = new HashSet<>();
            for (AgingAdrContainer<KAddress, GradientLocalView> container : sample.publicSample.values()) {
                int age = container.getAge();
                croupierCopy.add(new GradientContainer(container.getSource(), container.getContent().appView, age, container.getContent().rank));
            }
            for (AgingAdrContainer<KAddress, GradientLocalView> container : sample.privateSample.values()) {
                int age = container.getAge();
                croupierCopy.add(new GradientContainer(container.getSource(), container.getContent().appView, age, container.getContent().rank));
            }
            gradientFingers.merge(croupierCopy, selfView);
            
            //publish simplified sample
            Map publicSample = unwrappedSample(sample.publicSample);
            Map privateSample = unwrappedSample(sample.privateSample);
            
            CroupierSample publishSample = new CroupierSample(sample.overlayId, publicSample, privateSample);
            trigger(publishSample, outCroupier);
        }
    };
    
    private Map unwrappedSample(Map wrapped) {
        Map unwrapped = new HashMap();
        for(Map.Entry<Identifier, AgingAdrContainer> e : ((Map<Identifier, AgingAdrContainer>)wrapped).entrySet()) {
            NatAwareAddress src = (NatAwareAddress)e.getValue().getSource();
            View appView = (View)e.getValue().getContent();
            int age = e.getValue().getAge();
            unwrapped.put(e.getKey(), new CroupierContainer(src, appView, age));
        }
        return unwrapped;
    }

    /**
     * Samples from linear gradient used for bootstrapping gradient as well as
     * faster convergence(random samples)
     */
    Handler handleGradientSample = new Handler<GradientSample<GradientLocalView>>() {
        @Override
        public void handle(GradientSample<GradientLocalView> sample) {
            if (!ready()) {
                return;
            }
            LOG.trace("{} {}", logPrefix, sample);
            LOG.debug("{} \n gradient sample:{}", new Object[]{logPrefix, sample.gradientNeighbours});

            gradientNeighbours = (List) sample.gradientNeighbours; //again java stupid generics
            gradientFingers.merge(gradientNeighbours, selfView);
        }
    };

    private void sendShuffleRequest(KAddress to, List<GradientContainer> exchangeGC) {
        DecoratedHeader requestHeader
                = new DecoratedHeader(new BasicHeader(selfView.getSource(), to, Transport.UDP), overlayId);
        GradientShuffle.Request requestContent
                = new GradientShuffle.Request(overlayId, selfView, exchangeGC);
        BasicContentMsg request = new BasicContentMsg(requestHeader, requestContent);
        LOG.trace("{}sending:{} to:{}", new Object[]{logPrefix, requestContent, request.getDestination()});
        trigger(request, network);
    }

    private void sendShuffleResponse(BasicContentMsg<?, ?, GradientShuffle.Request> req, List<GradientContainer> exchangeGC) {
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

            publishSample();

            //TODO Alex send copies and less than view
            GradientContainer partner = gradientFingers.getShuffleNode(selfView);
            gradientFingers.incrementAges();
            sendShuffleRequest(partner.getSource(), gradientNeighbours);
            scheduleShuffleTimeout(partner.getSource());
        }
    };

    private void publishSample() {
        TGradientSample sample = new TGradientSample(overlayId, selfView.getContent(), gradientNeighbours,
                gradientFingers.getAllCopy());
        if(sample.gradientNeighbours.isEmpty()) {
            LOG.warn("{}no neighbours", logPrefix);
        }
        if(sample.gradientFingers.isEmpty()) {
            LOG.warn("{}no fingers", logPrefix);
        }
        LOG.info("{}fingers:{} neighbours:{}", new Object[]{logPrefix, sample.gradientFingers, sample.gradientNeighbours});
        trigger(sample, tGradient);
    }
    
    ClassMatchedHandler handleShuffleRequest
            = new ClassMatchedHandler<GradientShuffle.Request, BasicContentMsg<KAddress, DecoratedHeader<KAddress>, GradientShuffle.Request>>() {

                @Override
                public void handle(GradientShuffle.Request content, BasicContentMsg<KAddress, DecoratedHeader<KAddress>, GradientShuffle.Request> container) {
                    KAddress reqSrc = container.getHeader().getSource();
                    if (selfView.getSource().getId().equals(reqSrc.getId())) {
                        LOG.error("{} Tried to shuffle with myself", logPrefix);
                        throw new RuntimeException("tried to shuffle with myself");
                    }
                    LOG.debug("{} received:{}", new Object[]{logPrefix, container});
                    if (!ready()) {
                        return;
                    }

                    gradientFingers.incrementAges();
                    sendShuffleResponse(container, gradientNeighbours);

                    gradientFingers.merge(content.exchangeGC, selfView);
                    gradientFingers.merge(content.selfGC, selfView);
                }
            };

    ClassMatchedHandler handleShuffleResponse
            = new ClassMatchedHandler<GradientShuffle.Response, BasicContentMsg<KAddress, DecoratedHeader<KAddress>, GradientShuffle.Response>>() {

                @Override
                public void handle(GradientShuffle.Response content, BasicContentMsg<KAddress, DecoratedHeader<KAddress>, GradientShuffle.Response> container) {
                    KAddress respSrc = container.getHeader().getSource();
                    if (selfView.getSource().getId().equals(respSrc.getId())) {
                        LOG.error("{} Tried to shuffle with myself", logPrefix);
                        throw new RuntimeException("tried to shuffle with myself");
                    }
                    LOG.debug("{} received:{}", new Object[]{logPrefix, container});

                    if (shuffleTimeoutId == null) {
                        LOG.debug("{} req:{}  already timed out", new Object[]{logPrefix, content.msgId, respSrc});
                        return;
                    }
                    cancelShuffleTimeout();

                    gradientFingers.merge(content.exchangeGC, selfView);
                    gradientFingers.merge(content.selfGC, selfView);
                }
            };

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
                gradientFingers.clean(event.dest);
            }
        }
    };

    private void schedulePeriodicShuffle() {
        LOG.info("{}shuffle period:{}", logPrefix, gradientConfig.shufflePeriod);
        if (shuffleCycleId != null) {
            LOG.warn("{} double starting periodic shuffle", logPrefix);
            return;
        }
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(gradientConfig.shufflePeriod, gradientConfig.shufflePeriod);
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
            return "TGradient<" + overlayId + ">ShuffleCycle<" + getTimeoutId()+ ">";
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
            return "TGradient<" + overlayId + ">ShuffleTimeout<" + getTimeoutId()+ ">";
        }
    }

    public static class Init extends se.sics.kompics.Init<TreeGradientComp> {

        public final KAddress selfAdr;
        public final OverlayId overlayId;
        public final GradientFilter gradientFilter;

        public Init(KAddress selfAdr, OverlayId overlayId, GradientFilter gradientFilter) {
            this.selfAdr = selfAdr;
            this.overlayId = overlayId;
            this.gradientFilter = gradientFilter;
        }
    }
}

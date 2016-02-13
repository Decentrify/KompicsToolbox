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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
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
import se.sics.ktoolbox.gradient.GradientFilter;
import se.sics.ktoolbox.gradient.GradientKCWrapper;
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.gradient.event.GradientEvent;
import se.sics.ktoolbox.gradient.event.GradientSample;
import se.sics.ktoolbox.gradient.msg.GradientShuffle;
import se.sics.ktoolbox.gradient.temp.RankUpdate;
import se.sics.ktoolbox.gradient.temp.RankUpdatePort;
import se.sics.ktoolbox.gradient.util.GradientContainer;
import se.sics.ktoolbox.gradient.util.GradientLocalView;
import se.sics.ktoolbox.gradient.util.ViewConfig;
import se.sics.ktoolbox.util.address.AddressUpdate;
import se.sics.ktoolbox.util.address.AddressUpdatePort;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.network.basic.DecoratedHeader;
import se.sics.ktoolbox.util.other.AgingContainer;
import se.sics.ktoolbox.util.update.view.ViewUpdatePort;
import se.sics.ktoolbox.tgradient.util.TGParentView;
import se.sics.ktoolbox.util.aggregation.CompTracker;
import se.sics.ktoolbox.util.aggregation.CompTrackerImpl;
import se.sics.ktoolbox.util.update.view.OverlayViewUpdate;
import se.sics.ktoolbox.util.update.view.View;

/**
 *
 * Main Gradient class responsible for shuffling peer views with neighbors. It
 * is responsible for maintaining the gradient and returning periodically
 * gradient sample, to the application.
 *
 */
public class TreeGradientComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(TreeGradientComp.class);
    private final String logPrefix;

    private final GradientKCWrapper gradientConfig;
    private final TGradientKCWrapper tgradientConfig;
    private KAddress self;

    private GradientFilter filter;
    private GradientContainer selfView;
    private final TGParentView parents;

    private UUID shuffleCycleId;
    private UUID shuffleTimeoutId;

    private List<GradientContainer> neighbours = new ArrayList<>();

    // == Identify Ports.
    Negative tGradient = provides(GradientPort.class);
    Negative gradientViewUpdate = provides(ViewUpdatePort.class);
    Positive network = requires(Network.class);
    Positive timer = requires(Timer.class);
    Positive viewUpdate = requires(ViewUpdatePort.class);
    Positive addressUpdate = requires(AddressUpdatePort.class);
    Positive croupier = requires(CroupierPort.class);
    Positive gradient = requires(GradientPort.class);
    Positive rankUpdate = requires(RankUpdatePort.class);
    
    private CompTracker compTracker;
    
    public TreeGradientComp(TreeGradientInit init) {
        this.gradientConfig = new GradientKCWrapper(config(), init.seed, init.overlayId);
        this.tgradientConfig = new TGradientKCWrapper(config());
        this.self = init.self;
        this.logPrefix = "<oid:" + gradientConfig.overlayId + ":nid:" + self.getId().toString() + "> ";
        LOG.info("{} initializing with seed:{}", logPrefix, gradientConfig.seed);
        ViewConfig viewConfig = new ViewConfig(gradientConfig.viewSize, gradientConfig.softMaxTemp, gradientConfig.oldThreshold);
        this.parents = new TGParentView(gradientConfig, tgradientConfig, logPrefix, init.gradientFilter);
        this.filter = init.gradientFilter;

        setCompTracker();
        
        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleSelfAddressUpdate, addressUpdate);
        subscribe(handleViewUpdate, viewUpdate);
        subscribe(handleRankUpdate, rankUpdate);

        subscribe(handleCroupierSample, croupier);
        subscribe(handleGradientSample, gradient);
        subscribe(handleShuffleRequest, network);
        subscribe(handleShuffleResponse, network);
        subscribe(handleShuffleCycle, timer);
        subscribe(handleShuffleTimeout, timer);
    }

    //*********Control**********************************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{} starting...", logPrefix);
            compTracker.start();
        }
    };

    Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            LOG.info("{} stopping...", logPrefix);

        }
    };
    
    Handler handleSelfAddressUpdate = new Handler<AddressUpdate.Indication>() {
        @Override
        public void handle(AddressUpdate.Indication update) {
            LOG.debug("{} update self address:{}", logPrefix, update.localAddress);
            self = update.localAddress;
        }
    };
    
    Handler handleViewUpdate = new Handler<OverlayViewUpdate.Indication<View>>() {
        @Override
        public void handle(OverlayViewUpdate.Indication<View> update) {
            LOG.debug("{} update self view:{}", logPrefix, update.view);
            if (selfView != null && filter.cleanOldView(selfView.getContent(), update.view)) {
                neighbours = new ArrayList<GradientContainer>();
                parents.clean(update.view);
            }
            int rank = (selfView == null ? Integer.MAX_VALUE : selfView.rank);
            selfView = new GradientContainer(self, update.view, 0, rank);
            trigger(update, gradientViewUpdate);
        }
    };

    Handler handleRankUpdate = new Handler<RankUpdate>() {
        @Override
        public void handle(RankUpdate update) {
            LOG.trace("{} {}", logPrefix, update);
            //TODO Alex - update mixup is possible
            if (selfView == null) {
                throw new RuntimeException("update mixup");
            }
            selfView = new GradientContainer(selfView.getSource(), selfView.getContent(), selfView.getAge(), update.rank);
            if (!connected() && haveShufflePartners()) {
                schedulePeriodicShuffle();
            }
        }
    };
    
    private boolean haveShufflePartners() {
        return !parents.isEmpty();
    }

    private boolean connected() {
        return shuffleCycleId != null;
    }

    //***************************STATE TRACKING*********************************
    private void setCompTracker() {
        switch (tgradientConfig.tgradientAggLevel) {
            case NONE:
                break;
            case BASIC:
                compTracker = new CompTrackerImpl(proxy, Pair.with(LOG, logPrefix), tgradientConfig.tgradientAggPeriod);
                break;
            case FULL:
                compTracker = new CompTrackerImpl(proxy, Pair.with(LOG, logPrefix), tgradientConfig.tgradientAggPeriod);
                compTracker.registerPositivePort(network);
                compTracker.registerPositivePort(timer);
                compTracker.registerNegativePort(gradient);
                compTracker.registerNegativePort(rankUpdate);
                compTracker.registerPositivePort(croupier);
                compTracker.registerPositivePort(addressUpdate);
                compTracker.registerPositivePort(viewUpdate);
                break;
            default:
                throw new RuntimeException("Undefined:" + tgradientConfig.tgradientAggLevel);
        }
    }
    //**************************************************************************

    /**
     * Samples from Croupier used for bootstrapping gradient as well as faster
     * convergence(random samples)
     */
    Handler handleCroupierSample = new Handler<CroupierSample<GradientLocalView>>() {
        @Override
        public void handle(CroupierSample<GradientLocalView> sample) {
            if (selfView.rank == Integer.MAX_VALUE) {
                return;
            }
            LOG.debug("{} {}", logPrefix, sample);
            LOG.debug("{} \nCroupier public sample:{} \nCroupier private sample:{}",
                    new Object[]{logPrefix, sample.publicSample, sample.privateSample});

            Collection<GradientContainer> gradientCopy = new HashSet<GradientContainer>();
            for (AgingContainer<KAddress, GradientLocalView> container : sample.publicSample.values()) {
                int age= container.getAge();
                gradientCopy.add(new GradientContainer(container.getSource(), container.getContent().appView, age, container.getContent().rank));
            }
            for (AgingContainer<KAddress, GradientLocalView> container : sample.privateSample.values()) {
                int age = container.getAge();
                gradientCopy.add(new GradientContainer(container.getSource(), container.getContent().appView, age, container.getContent().rank));
            }
//             parents.merge(gradientCopy, selfView);
            if (!connected() && haveShufflePartners()) {
                schedulePeriodicShuffle();
            }
        }
    };

    /**
     * Samples from linear gradient used for bootstrapping gradient as well as
     * faster convergence(random samples)
     */
    Handler handleGradientSample = new Handler<GradientSample<GradientLocalView>>() {
        @Override
        public void handle(GradientSample<GradientLocalView> sample) {
            if (selfView.rank == Integer.MAX_VALUE) {
                return;
            }
            LOG.trace("{} {}", logPrefix, sample);
            LOG.debug("{} \n gradient sample:{}", new Object[]{logPrefix, sample.gradientSample});

            neighbours = (List)sample.gradientSample; //again java stupid generics
            parents.merge(neighbours, selfView);
            if (!connected() && haveShufflePartners()) {
                schedulePeriodicShuffle();
            }
        }
    };

    /**
     * Periodic Gradient Shuffle
     */
    Handler<ShuffleCycle> handleShuffleCycle = new Handler<ShuffleCycle>() {
        @Override
        public void handle(ShuffleCycle event) {
            if (selfView.rank == Integer.MAX_VALUE) {
                return;
            }

            LOG.trace("{} {}", logPrefix, event);

            if (!haveShufflePartners()) {
                LOG.warn("{} no shuffle partners - disconnected", logPrefix);
                cancelPeriodicShuffle();
                return;
            }

            if (!parents.isEmpty()) {
                LOG.debug("{} view:{}", logPrefix, parents.getAllCopy());
                trigger(new GradientSample(UUIDIdentifier.randomId(), selfView.getContent(), parents.getAllCopy()), tGradient);
            }

            // NOTE:
            GradientContainer partner = parents.getShuffleNode(selfView);
            parents.incrementAges();

            DecoratedHeader<KAddress> requestHeader = new DecoratedHeader(new BasicHeader(self, partner.getSource(), Transport.UDP), gradientConfig.overlayId);
            GradientShuffle.Request requestContent = new GradientShuffle.Request(UUIDIdentifier.randomId(), selfView, neighbours);
            BasicContentMsg request = new BasicContentMsg(requestHeader, requestContent);
            LOG.debug("{} sending:{}", new Object[]{logPrefix, request});
            trigger(request, network);
            scheduleShuffleTimeout(partner.getSource());
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
                parents.clean(event.dest);
            }
        }
    };

    ClassMatchedHandler handleShuffleRequest
            = new ClassMatchedHandler<GradientShuffle.Request, BasicContentMsg<KAddress, DecoratedHeader<KAddress>, GradientShuffle.Request>>() {

                @Override
                public void handle(GradientShuffle.Request content, BasicContentMsg<KAddress, DecoratedHeader<KAddress>, GradientShuffle.Request> container) {
                    KAddress reqSrc = container.getHeader().getSource();
                    if (self.getId().equals(reqSrc.getId())) {
                        LOG.error("{} Tried to shuffle with myself", logPrefix);
                        throw new RuntimeException("tried to shuffle with myself");
                    }
                    LOG.debug("{} received:{}", new Object[]{logPrefix, container});
                    if (selfView == null) {
                        LOG.warn("{} not ready to shuffle - no self view available - {} tried to shuffle with me",
                                logPrefix, reqSrc);
                        return;
                    }

                    parents.incrementAges();

                    GradientShuffle.Response responseContent = new GradientShuffle.Response(content.id, selfView, neighbours);
                    BasicContentMsg response = container.answer(responseContent);
                    LOG.debug("{} sending:{}", new Object[]{logPrefix, response});
                    trigger(response, network);

                    parents.merge(content.exchangeNodes, selfView);
                    parents.merge(content.selfGC, selfView);
                }
            };

    ClassMatchedHandler handleShuffleResponse
            = new ClassMatchedHandler<GradientShuffle.Response, BasicContentMsg<KAddress, DecoratedHeader<KAddress>, GradientShuffle.Response>>() {

                @Override
                public void handle(GradientShuffle.Response content, BasicContentMsg<KAddress, DecoratedHeader<KAddress>, GradientShuffle.Response> container) {
                    KAddress respSrc = container.getHeader().getSource();
                    if (self.getId().equals(respSrc.getId())) {
                        LOG.error("{} Tried to shuffle with myself", logPrefix);
                        throw new RuntimeException("tried to shuffle with myself");
                    }
                    LOG.debug("{} received:{}", new Object[]{logPrefix, container});

                    if (shuffleTimeoutId == null) {
                        LOG.debug("{} req:{}  already timed out", new Object[]{logPrefix, content.id, respSrc});
                        return;
                    }
                    cancelShuffleTimeout();

                    parents.merge(content.exchangeNodes, selfView);
                    parents.merge(content.selfGC, selfView);
                }
            };

    private void schedulePeriodicShuffle() {
        LOG.warn("{} period:{}", logPrefix, gradientConfig.shufflePeriod);
        if (shuffleCycleId != null) {
            LOG.warn("{} double starting periodic shuffle", logPrefix);
            return;
        }
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(gradientConfig.shufflePeriod, gradientConfig.shufflePeriod);
        ShuffleCycle sc = new ShuffleCycle(spt);
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
        ShuffleTimeout sc = new ShuffleTimeout(spt, dest);
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

    public class ShuffleCycle extends Timeout {

        public ShuffleCycle(SchedulePeriodicTimeout request) {
            super(request);
        }

        @Override
        public String toString() {
            return "SHUFFLE_CYCLE";
        }
    }

    public class ShuffleTimeout extends Timeout implements GradientEvent {

        public final KAddress dest;

        public ShuffleTimeout(ScheduleTimeout request, KAddress dest) {
            super(request);
            this.dest = dest;
        }

        @Override
        public String toString() {
            return "SHUFFLE_TIMEOUT";
        }

        @Override
        public Identifier getId() {
            return new UUIDIdentifier(getTimeoutId());
        }
    }

    public static class TreeGradientInit extends Init<TreeGradientComp> {
        public final KAddress self;
        public final GradientFilter gradientFilter;
        public final long seed;
        public final Identifier overlayId;

        public TreeGradientInit(KAddress self, GradientFilter gradientFilter, long seed, Identifier overlayId) {
            this.self = self;
            this.gradientFilter = gradientFilter;
            this.seed = seed;
            this.overlayId = overlayId;
        }
    }
}

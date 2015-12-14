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

import com.google.common.base.Optional;
import se.sics.ktoolbox.gradient.util.GradientView;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
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
import se.sics.ktoolbox.gradient.event.GradientSample;
import se.sics.ktoolbox.gradient.event.GradientUpdate;
import se.sics.ktoolbox.gradient.msg.GradientShuffle;
import se.sics.ktoolbox.gradient.temp.RankUpdate;
import se.sics.ktoolbox.gradient.temp.RankUpdatePort;
import se.sics.ktoolbox.gradient.util.GradientContainer;
import se.sics.ktoolbox.gradient.util.GradientLocalView;
import se.sics.ktoolbox.util.address.AddressUpdate;
import se.sics.ktoolbox.util.address.AddressUpdatePort;
import se.sics.ktoolbox.util.compare.WrapperComparator;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.IntIdentifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.network.basic.DecoratedHeader;
import se.sics.ktoolbox.util.other.AgingContainer;
import se.sics.ktoolbox.util.update.view.OverlayView;
import se.sics.ktoolbox.util.update.view.View;
import se.sics.ktoolbox.util.update.view.ViewUpdate;
import se.sics.ktoolbox.util.update.view.ViewUpdatePort;

/**
 * Main Gradient class responsible for shuffling peer views with neighbors. It
 * is responsible for maintaining the gradient and returning periodically
 * gradient sample, to the application.
 *
 */
public class GradientComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(GradientComp.class);
    private final String logPrefix;

    private final GradientKCWrapper config;
    private KAddress self;

    private GradientFilter filter;
    private GradientContainer selfView;
    private final GradientView view;
    private final Comparator<GradientContainer> utilityComp;

    private UUID shuffleCycleId;
    private UUID shuffleTimeoutId;

    // == Identify Ports.
    Negative gradient = provides(GradientPort.class);
    Negative rankUpdate = provides(RankUpdatePort.class);
    Negative<ViewUpdatePort> croupierViewUpdate = provides(ViewUpdatePort.class);
    Positive network = requires(Network.class);
    Positive timer = requires(Timer.class);
    Positive croupier = requires(CroupierPort.class);
    Positive viewUpdate = requires(ViewUpdatePort.class);
    Positive addressUpdate = requires(AddressUpdatePort.class);

    public GradientComp(GradientInit init) {
        this.config = new GradientKCWrapper(config(), init.seed, init.overlayId);
        this.self = init.self;
        this.logPrefix = "<oid:" + config.overlayId + ":nid:" + self.getId().toString() + "> ";
        LOG.info("{} initializing with seed:{}", logPrefix, config.seed);
        this.utilityComp = new WrapperComparator<GradientContainer>(init.utilityComparator);
        this.view = new GradientView(config, logPrefix, init.utilityComparator, init.gradientFilter);
        this.filter = init.gradientFilter;

        subscribe(handleStart, control);
        subscribe(handleViewUpdate, viewUpdate);
        subscribe(handleAddressUpdate, addressUpdate);

        subscribe(handleCroupierSample, croupier);
        subscribe(handleShuffleRequest, network);
        subscribe(handleShuffleResponse, network);
        subscribe(handleShuffleCycle, timer);
        subscribe(handleShuffleTimeout, timer);
    }

    //*************************Control******************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{} starting...", logPrefix);
        }
    };

    Handler handleAddressUpdate = new Handler<AddressUpdate.Indication>() {
        @Override
        public void handle(AddressUpdate.Indication update) {
            LOG.info("{} updating self address:{}", new Object[]{logPrefix, update.localAddress});
            self = update.localAddress;
        }
    };

    Handler handleViewUpdate = new Handler<GradientUpdate>() {
        @Override
        public void handle(GradientUpdate update) {
            LOG.info("{} updating self view:{}", new Object[]{logPrefix, update.view});
            if (selfView != null && filter.cleanOldView(selfView.getContent(), update.view)) {
                view.clean(update.view);
            }
            int rank = (selfView == null ? Integer.MAX_VALUE : selfView.rank);
            selfView = new GradientContainer(self, update.view, 0, rank);
            if (!connected() && haveShufflePartners()) {
                schedulePeriodicShuffle();
            }
            trigger(new ViewUpdate.Indication(UUIDIdentifier.randomId(), new OverlayView(false, Optional.of(
                    (View) new GradientLocalView(update.view, selfView.rank)))), croupierViewUpdate);
        }
    };

    private boolean haveShufflePartners() {
        return !view.isEmpty();
    }

    private boolean connected() {
        return shuffleCycleId != null;
    }

    //**************************************************************************
    /**
     * Samples from Croupier used for bootstrapping gradient as well as faster
     * convergence(random samples)
     */
    Handler handleCroupierSample = new Handler<CroupierSample<GradientLocalView>>() {
        @Override
        public void handle(CroupierSample<GradientLocalView> sample) {
            LOG.trace("{} {}", logPrefix, sample);
            LOG.debug("{} \nCroupier public sample:{} \nCroupier private sample:{}",
                    new Object[]{logPrefix, sample.publicSample, sample.privateSample});

            Set<GradientContainer> gradientCopy = new HashSet<GradientContainer>();
            for (AgingContainer<KAddress, GradientLocalView> container : sample.publicSample.values()) {
                int age = container.getAge();
                gradientCopy.add(new GradientContainer(container.getSource(), container.getContent().appView, age, container.getContent().rank));
            }
            for (AgingContainer<KAddress, GradientLocalView> container : sample.privateSample.values()) {
                int age = container.getAge();
                gradientCopy.add(new GradientContainer(container.getSource(), container.getContent().appView, age, container.getContent().rank));
            }
            view.merge(gradientCopy, selfView);
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
            LOG.trace("{} {}", logPrefix, event);

            if (view.checkIfTop(selfView) && selfView.rank != 0) {
                selfView = new GradientContainer(selfView.getSource(), selfView.getContent(), selfView.getAge(), 0);
                LOG.debug("{} am top", logPrefix, view.getAllCopy());
                trigger(new ViewUpdate.Indication(UUIDIdentifier.randomId(), new OverlayView(false, Optional.of(
                        (View) new GradientLocalView(selfView.getContent(), selfView.rank)))), croupierViewUpdate);
                trigger(new RankUpdate(UUIDIdentifier.randomId(), selfView.rank), rankUpdate);
            }
            LOG.debug("{} rank:{}", logPrefix, selfView.rank);

            if (!haveShufflePartners()) {
                LOG.warn("{} no shuffle partners - disconnected", logPrefix);
                cancelPeriodicShuffle();
                return;
            }

            if (!view.isEmpty()) {
                LOG.info("{} view:{}", logPrefix, view.getAllCopy());
                trigger(new GradientSample(UUIDIdentifier.randomId(), selfView.getContent(), view.getAllCopy()), gradient);
            }

            // NOTE:
            GradientContainer partner = view.getShuffleNode(selfView);
            view.incrementAges();

            Set<GradientContainer> exchangeGC = view.getExchangeCopy(partner, config.shuffleSize);
            DecoratedHeader<KAddress> requestHeader = new DecoratedHeader(
                    new BasicHeader(self, partner.getSource(), Transport.UDP), config.overlayId);
            GradientShuffle.Request requestContent = new GradientShuffle.Request(UUIDIdentifier.randomId(), selfView, exchangeGC);
            BasicContentMsg request = new BasicContentMsg(requestHeader, requestContent);
            LOG.debug("{} sending:{} to:{}", new Object[]{logPrefix, requestContent.exchangeNodes, partner.getSource()});
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
                view.clean(event.dest);
            }
        }
    };

    ClassMatchedHandler handleShuffleRequest
            = new ClassMatchedHandler<GradientShuffle.Request, BasicContentMsg<KAddress, DecoratedHeader<KAddress>, GradientShuffle.Request>>() {

                @Override
                public void handle(GradientShuffle.Request content, BasicContentMsg<KAddress, DecoratedHeader<KAddress>, GradientShuffle.Request> container) {
                    DecoratedHeader<KAddress> header = container.getHeader();
                    KAddress reqSrc = container.getHeader().getSource();
                    if (self.getId().equals(reqSrc.getId())) {
                        LOG.error("{} Tried to shuffle with myself", logPrefix);
                        throw new RuntimeException("tried to shuffle with myself");
                    }
                    LOG.debug("{} received:{} from:{}", new Object[]{logPrefix, content.exchangeNodes, reqSrc});
                    if (selfView == null) {
                        LOG.warn("{} not ready to shuffle - no self view available - {} tried to shuffle with me",
                                logPrefix, reqSrc);
                        return;
                    }

                    view.incrementAges();

                    Set<GradientContainer> exchangeGC = view.getExchangeCopy(content.selfGC, config.shuffleSize);
                    GradientShuffle.Response responseContent = new GradientShuffle.Response(content.id, selfView, exchangeGC);
                    BasicContentMsg response = container.answer(responseContent);
                    LOG.debug("{} sending:{} to:{}", new Object[]{logPrefix, responseContent.exchangeNodes, container.getHeader().getSource()});
                    trigger(response, network);

                    view.merge(content.exchangeNodes, selfView);
                    view.merge(content.selfGC, selfView);
                }
            };

    ClassMatchedHandler handleShuffleResponse
            = new ClassMatchedHandler<GradientShuffle.Response, BasicContentMsg<KAddress, DecoratedHeader<KAddress>, GradientShuffle.Response>>() {

                @Override
                public void handle(GradientShuffle.Response content, BasicContentMsg<KAddress, DecoratedHeader<KAddress>, GradientShuffle.Response> container) {
                    DecoratedHeader<KAddress> header = container.getHeader();
                    KAddress respSrc = container.getHeader().getSource();
                    if (self.getId().equals(respSrc.getId())) {
                        LOG.error("{} Tried to shuffle with myself", logPrefix);
                        throw new RuntimeException("tried to shuffle with myself");
                    }
                    LOG.debug("{} received:{} from:{}", new Object[]{logPrefix, content.exchangeNodes, respSrc});

                    if (shuffleTimeoutId == null) {
                        LOG.debug("{} req:{}  already timed out", new Object[]{logPrefix, content.id, respSrc});
                        return;
                    }
                    cancelShuffleTimeout();

                    view.merge(content.exchangeNodes, selfView);
                    view.merge(content.selfGC, selfView);

                    if (utilityComp.compare(selfView, content.selfGC) >= 0) {
                        return;
                    }
                    int nodeDist = view.getDistTo(selfView, content.selfGC);
                    if (content.selfGC.rank != Integer.MAX_VALUE && selfView.rank != content.selfGC.rank + nodeDist) {
                        selfView = new GradientContainer(selfView.getSource(), selfView.getContent(), selfView.getAge(), content.selfGC.rank + nodeDist);
                        LOG.debug("{} new rank:{} partner:{} partner rank:{}", new Object[]{logPrefix, selfView.rank, content.selfGC.getSource(), content.selfGC.rank});
                        trigger(new ViewUpdate.Indication(UUIDIdentifier.randomId(), new OverlayView(false, Optional.of(
                                                        (View) new GradientLocalView(selfView.getContent(), selfView.rank)))), croupierViewUpdate);
                        trigger(new RankUpdate(UUIDIdentifier.randomId(), selfView.rank), rankUpdate);
                    }
                }
            };

    private void schedulePeriodicShuffle() {
        if (shuffleCycleId != null) {
            LOG.warn("{} double starting periodic shuffle", logPrefix);
            return;
        }
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(config.shufflePeriod, config.shufflePeriod);
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
        ScheduleTimeout spt = new ScheduleTimeout(config.shufflePeriod / 2);
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

    public class ShuffleTimeout extends Timeout {

        public final KAddress dest;

        public ShuffleTimeout(ScheduleTimeout request, KAddress dest) {
            super(request);
            this.dest = dest;
        }

        @Override
        public String toString() {
            return "SHUFFLE_TIMEOUT";
        }
    }

    public static class GradientInit extends Init<GradientComp> {

        public final KAddress self;
        public final Comparator utilityComparator;
        public final GradientFilter gradientFilter;
        public final long seed;
        public final Identifier overlayId;

        public GradientInit(KAddress self, Comparator utilityComparator, GradientFilter gradientFilter, long seed, Identifier overlayId) {
            this.self = self;
            this.utilityComparator = utilityComparator;
            this.gradientFilter = gradientFilter;
            this.seed = seed;
            this.overlayId = overlayId;
        }
    }
}

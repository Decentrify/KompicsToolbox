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
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.gradient.GradientFilter;
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
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.simutil.identifiable.Identifiable;
import se.sics.kompics.simutil.identifiable.impl.IntIdentifier;
import se.sics.kompics.simutil.msg.impl.BasicContentMsg;
import se.sics.kompics.simutil.msg.impl.BasicHeader;
import se.sics.kompics.simutil.msg.impl.DecoratedHeader;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.croupier.CroupierPort;
import se.sics.ktoolbox.croupier.event.CroupierSample;
import se.sics.ktoolbox.gradient.msg.GradientSample;
import se.sics.ktoolbox.gradient.msg.GradientUpdate;
import se.sics.ktoolbox.gradient.msg.GradientShuffle;
import se.sics.ktoolbox.gradient.temp.RankUpdate;
import se.sics.ktoolbox.gradient.temp.RankUpdatePort;
import se.sics.ktoolbox.gradient.util.GradientContainer;
import se.sics.ktoolbox.gradient.util.GradientLocalView;
import se.sics.ktoolbox.util.address.resolution.AddressUpdate;
import se.sics.ktoolbox.util.address.resolution.AddressUpdatePort;
import se.sics.ktoolbox.util.compare.WrapperComparator;
import se.sics.ktoolbox.util.other.AgingContainer;
import se.sics.ktoolbox.util.other.Container;
import se.sics.ktoolbox.util.update.view.View;
import se.sics.ktoolbox.util.update.view.ViewUpdate;
import se.sics.ktoolbox.util.update.view.ViewUpdatePort;
import se.sics.ktoolbox.util.update.view.impl.OverlayView;

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
    private Address self;

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
        this.config = init.config;
        this.self = init.self;
        this.logPrefix = "<oid:" + config.overlayId + ":nid:" + ((Identifiable) self).getId().toString() + "> ";
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
            trigger(new ViewUpdate.Indication(UUID.randomUUID(), new OverlayView(false, Optional.of(
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
            for (AgingContainer<Address, GradientLocalView> container : sample.publicSample.values()) {
                int age = container.getAge();
                gradientCopy.add(new GradientContainer(container.getSource(), container.getContent().appView, age, container.getContent().rank));
            }
            for (AgingContainer<Address, GradientLocalView> container : sample.privateSample.values()) {
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
                trigger(new ViewUpdate.Indication(UUID.randomUUID(), new OverlayView(false, Optional.of(
                        (View) new GradientLocalView(selfView.getContent(), selfView.rank)))), croupierViewUpdate);
                trigger(new RankUpdate(selfView.rank), rankUpdate);
            }
            LOG.debug("{} rank:{}", logPrefix, selfView.rank);

            if (!haveShufflePartners()) {
                LOG.warn("{} no shuffle partners - disconnected", logPrefix);
                cancelPeriodicShuffle();
                return;
            }

            if (!view.isEmpty()) {
                LOG.info("{} view:{}", logPrefix, view.getAllCopy());
                trigger(new GradientSample(selfView.getContent(), view.getAllCopy()), gradient);
            }

            // NOTE:
            GradientContainer partner = view.getShuffleNode(selfView);
            view.incrementAges();

            Set<GradientContainer> exchangeGC = view.getExchangeCopy(partner, config.shuffleSize);
            DecoratedHeader<Address> requestHeader = new DecoratedHeader(
                    new BasicHeader(self, partner.getSource(), Transport.UDP), new IntIdentifier(config.overlayId));
            GradientShuffle.Request requestContent = new GradientShuffle.Request(UUID.randomUUID(), selfView, exchangeGC);
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
            = new ClassMatchedHandler<GradientShuffle.Request, BasicContentMsg<Address, DecoratedHeader<Address>, GradientShuffle.Request>>() {

                @Override
                public void handle(GradientShuffle.Request content, BasicContentMsg<Address, DecoratedHeader<Address>, GradientShuffle.Request> container) {
                    DecoratedHeader<Address> header = container.getHeader();
                    Address reqSrc = container.getHeader().getSource();
                    if (((Identifiable) self).getId().equals(((Identifiable) reqSrc).getId())) {
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
                    DecoratedHeader<Address> responseHeader = new DecoratedHeader(
                            new BasicHeader(self, container.getHeader().getSource(), Transport.UDP), new IntIdentifier(config.overlayId));
                    GradientShuffle.Response responseContent = new GradientShuffle.Response(content.id, selfView, exchangeGC);
                    BasicContentMsg request = new BasicContentMsg(responseHeader, responseContent);
                    LOG.debug("{} sending:{} to:{}", new Object[]{logPrefix, responseContent.exchangeNodes, container.getHeader().getSource()});
                    trigger(request, network);

                    view.merge(content.exchangeNodes, selfView);
                    view.merge(content.selfGC, selfView);
                }
            };

    ClassMatchedHandler handleShuffleResponse
            = new ClassMatchedHandler<GradientShuffle.Response, BasicContentMsg<Address, DecoratedHeader<Address>, GradientShuffle.Response>>() {

                @Override
                public void handle(GradientShuffle.Response content, BasicContentMsg<Address, DecoratedHeader<Address>, GradientShuffle.Response> container) {
                    DecoratedHeader<Address> header = container.getHeader();
                    Address respSrc = container.getHeader().getSource();
                    if (((Identifiable) self).getId().equals(((Identifiable) respSrc).getId())) {
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
                        trigger(new ViewUpdate.Indication(UUID.randomUUID(), new OverlayView(false, Optional.of(
                                                        (View) new GradientLocalView(selfView.getContent(), selfView.rank)))), croupierViewUpdate);
                        trigger(new RankUpdate(selfView.rank), rankUpdate);
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

    private void scheduleShuffleTimeout(Address dest) {
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

        public final Address dest;

        public ShuffleTimeout(ScheduleTimeout request, Address dest) {
            super(request);
            this.dest = dest;
        }

        @Override
        public String toString() {
            return "SHUFFLE_TIMEOUT";
        }
    }

    public static class GradientInit extends Init<GradientComp> {

        public final GradientKCWrapper config;
        public final Address self;
        public final Comparator utilityComparator;
        public final GradientFilter gradientFilter;

        public GradientInit(GradientKCWrapper config, Address self,
                Comparator utilityComparator, GradientFilter gradientFilter) {
            this.config = config;
            this.self = self;
            this.utilityComparator = utilityComparator;
            this.gradientFilter = gradientFilter;
        }
    }
}

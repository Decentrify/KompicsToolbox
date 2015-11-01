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
package se.sics.p2ptoolbox.croupier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
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
import se.sics.p2ptoolbox.croupier.msg.CroupierDisconnected;
import se.sics.p2ptoolbox.croupier.msg.CroupierJoin;
import se.sics.p2ptoolbox.croupier.msg.CroupierSample;
import se.sics.p2ptoolbox.croupier.msg.CroupierUpdate;
import se.sics.p2ptoolbox.croupier.msg.CroupierShuffle;
import se.sics.p2ptoolbox.croupier.util.CroupierContainer;
import se.sics.p2ptoolbox.croupier.util.CroupierLocalView;
import se.sics.p2ptoolbox.util.config.KConfigCore;
import se.sics.p2ptoolbox.util.nat.NatedTrait;
import se.sics.p2ptoolbox.util.network.ContentMsg;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.BasicHeader;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;
import se.sics.p2ptoolbox.util.traits.OverlayMember;
import se.sics.p2ptoolbox.util.update.SelfAddressUpdate;
import se.sics.p2ptoolbox.util.update.SelfAddressUpdatePort;
import se.sics.p2ptoolbox.util.update.SelfViewUpdatePort;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierComp extends ComponentDefinition {

    private final static Logger LOG = LoggerFactory.getLogger(CroupierComp.class);
    private String logPrefix = "";

    Negative<CroupierControlPort> croupierControlPort = negative(CroupierControlPort.class);
    Negative<CroupierPort> croupierPort = negative(CroupierPort.class);
    Positive<Network> network = requires(Network.class);
    Positive<Timer> timer = requires(Timer.class);
    Positive<SelfAddressUpdatePort> selfAddressUpdate = requires(SelfAddressUpdatePort.class);
    Positive<SelfViewUpdatePort> selfViewUpdate = requires(SelfViewUpdatePort.class);

    private final CroupierKCWrapper config;
    private final int overlayId;
    private DecoratedAddress self;

    private List<DecoratedAddress> bootstrapNodes;
    private boolean observer;
    private Object selfView;
    private CroupierLocalView publicView;
    private CroupierLocalView privateView;

    private UUID shuffleCycleId;
    private UUID shuffleTimeoutId;

    public CroupierComp(CroupierInit init) {
        this.config = init.config;
        this.self = init.self;
        this.overlayId = init.overlayId;
        this.logPrefix = "<oid:" + overlayId + ",nid:" + self.getBase().toString() + ">";
        Random rand = new Random(init.seed);
        LOG.info("{} initiating with seed:{}", logPrefix, init.seed);
        this.bootstrapNodes = new ArrayList<>();
        this.selfView = null;
        this.shuffleCycleId = null;
        this.shuffleTimeoutId = null;

        this.publicView = new CroupierLocalView(self.getBase(), config.viewSize, rand);
        this.privateView = new CroupierLocalView(self.getBase(), config.viewSize, rand);

        subscribe(handleStart, control);
        subscribe(handleJoin, croupierControlPort);
        subscribe(handleSelfViewUpdate, selfViewUpdate);
        subscribe(handleSelfAddressUpdate, selfAddressUpdate);
        subscribe(handleShuffleRequest, network);
        subscribe(handleShuffleResponse, network);
        subscribe(handleShuffleCycle, timer);
        subscribe(handleShuffleTimeout, timer);
    }

    //****************************CONTROL***************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{} starting...", logPrefix);
        }
    };

    Handler handleSelfAddressUpdate = new Handler<SelfAddressUpdate>() {
        @Override
        public void handle(SelfAddressUpdate update) {
            LOG.info("{} updating selfAddress:{}", new Object[]{logPrefix, update.self});
            self = update.self;
        }
    };

    Handler handleSelfViewUpdate = new Handler<CroupierUpdate>() {
        @Override
        public void handle(CroupierUpdate update) {
            LOG.info("{} updating selfView:{}", new Object[]{logPrefix, update.selfView});

            observer = update.observer;
            selfView = (update.selfView.isPresent() ? update.selfView : selfView);

            if (!connected()) {
                startShuffle();
            }
        }
    };

    Handler handleJoin = new Handler<CroupierJoin>() {
        @Override
        public void handle(CroupierJoin join) {
            LOG.debug("{} joining using nodes:{}", logPrefix, join.peers);

            bootstrapNodes.addAll(join.peers);
            cleanSelf();
            if (!connected()) {
                startShuffle();
            }
        }
    };

    private void startShuffle() {
        if (selfView == null) {
            LOG.warn("{} no self view - not shuffling", new Object[]{logPrefix});
            return;
        }
        if (!haveShufflePartners()) {
            LOG.warn("{} no partners - not shuffling", new Object[]{logPrefix});
            return;
        }
        LOG.info("{} started shuffle", new Object[]{logPrefix});
        schedulePeriodicShuffle();
    }

    private void stopShuffle() {
        cancelPeriodicShuffle();
        LOG.warn("{} stopped shuffle", new Object[]{logPrefix});
        trigger(new CroupierDisconnected(overlayId), croupierControlPort);
    }

    private boolean connected() {
        return shuffleCycleId != null;
    }

    private boolean haveShufflePartners() {
        return !bootstrapNodes.isEmpty() || !publicView.isEmpty() || !privateView.isEmpty();
    }

    private void cleanSelf() {
        Iterator<DecoratedAddress> it = bootstrapNodes.iterator();
        while (it.hasNext()) {
            DecoratedAddress node = it.next();
            if (node.getBase().equals(self.getBase())) {
                it.remove();
            }
        }
    }
    //**************************************************************************

    private DecoratedAddress selectPeerToShuffleWith(double temperature) {
        if (!bootstrapNodes.isEmpty()) {
            return bootstrapNodes.remove(0);
        }
        DecoratedAddress node = null;
        if (!publicView.isEmpty()) {
            node = publicView.selectPeerToShuffleWith(config.policy, true, temperature);
        } else if (!privateView.isEmpty()) {
            node = privateView.selectPeerToShuffleWith(config.policy, true, temperature);
        }
        return node;
    }

    Handler<ShuffleCycle> handleShuffleCycle = new Handler<ShuffleCycle>() {
        @Override
        public void handle(ShuffleCycle event) {
            LOG.trace("{} {}", logPrefix, event);
            LOG.debug("{} public view size:{}, private view size:{}, bootstrap nodes size:{}", new Object[]{logPrefix, publicView.size(), privateView.size(), bootstrapNodes.size()});

            if (!haveShufflePartners()) {
                LOG.warn("{} no shuffle partners - disconnected", logPrefix);
                stopShuffle();
                return;
            }

            if (!publicView.isEmpty() || !privateView.isEmpty()) {
                CroupierSample cs = new CroupierSample(overlayId, publicView.getAllCopy(), privateView.getAllCopy());
                LOG.info("{} publishing sample \n public nodes:{} \n private nodes:{}", new Object[]{logPrefix, cs.publicSample, cs.privateSample});
                trigger(cs, croupierPort);
            }

            DecoratedAddress peer = selectPeerToShuffleWith(config.softMaxTemp);
            if (peer == null || peer.getBase().equals(self.getBase())) {
                LOG.error("{} this should not happen - logic error selecting peer", logPrefix);
                throw new RuntimeException("Error selecting peer");
            }

            if (NatedTrait.isOpen(peer)) {
                LOG.debug("{} did not pick a public node for shuffling - public view size:{}", new Object[]{logPrefix, publicView.getAllCopy().size()});
            }

            // NOTE:
            publicView.incrementDescriptorAges();
            privateView.incrementDescriptorAges();

            Set<CroupierContainer> publicDescCopy = publicView.initiatorCopySet(config.shuffleSize, peer);
            Set<CroupierContainer> privateDescCopy = privateView.initiatorCopySet(config.shuffleSize, peer);

            if (!observer) {
                if (NatedTrait.isOpen(self)) {
                    publicDescCopy.add(new CroupierContainer(self, selfView));
                } else {
                    privateDescCopy.add(new CroupierContainer(self, selfView));
                }
            }

            DecoratedHeader<DecoratedAddress> requestHeader = new DecoratedHeader(new BasicHeader(self, peer, Transport.UDP), null, overlayId);
            CroupierShuffle.Request requestContent = new CroupierShuffle.Request(UUID.randomUUID(), publicDescCopy, privateDescCopy);
            ContentMsg request = new BasicContentMsg(requestHeader, requestContent);
            LOG.trace("{} sending:{} to:{}", new Object[]{logPrefix, requestContent, peer});
            trigger(request, network);
            scheduleShuffleTimeout(peer);
        }
    };

    ClassMatchedHandler handleShuffleRequest
            = new ClassMatchedHandler<CroupierShuffle.Request, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, CroupierShuffle.Request>>() {

                @Override
                public void handle(CroupierShuffle.Request content, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, CroupierShuffle.Request> container) {
                    DecoratedHeader<DecoratedAddress> header = container.getHeader();
                    if (header.getTrait(OverlayMember.class).getOverlayId() != overlayId) {
                        LOG.error("{} message with header:{} not belonging to croupier overlay:{}", new Object[]{logPrefix, header, overlayId});
                        throw new RuntimeException("message not belonging to croupier overlay");
                    }
                    DecoratedAddress reqSrc = container.getHeader().getSource();
                    if (self.getBase().equals(reqSrc.getBase())) {
                        LOG.error("{} Tried to shuffle with myself", logPrefix);
                        throw new RuntimeException("tried to shuffle with myself");
                    }
                    LOG.trace("{} received:{} from:{}", new Object[]{logPrefix, content, reqSrc});
                    if (selfView == null) {
                        LOG.warn("{} not ready to shuffle - no self view available - {} tried to shuffle with me",
                                logPrefix, reqSrc);
                        return;
                    }

                    LOG.debug("{} received from:{} \n public nodes:{} \n private nodes:{}",
                            new Object[]{logPrefix, container.getHeader().getSource(), content.publicNodes, content.privateNodes});

                    publicView.incrementDescriptorAges();
                    privateView.incrementDescriptorAges();

                    Set<CroupierContainer> publicDescCopy = publicView.receiverCopySet(config.shuffleSize, reqSrc);
                    Set<CroupierContainer> privateDescCopy = privateView.receiverCopySet(config.shuffleSize, reqSrc);
                    if (NatedTrait.isOpen(self)) {
                        publicDescCopy.add(new CroupierContainer(self, selfView));
                    } else {
                        privateDescCopy.add(new CroupierContainer(self, selfView));
                    }

                    DecoratedHeader<DecoratedAddress> responseHeader = new DecoratedHeader(
                            new BasicHeader(self, reqSrc, Transport.UDP), null, overlayId);
                    CroupierShuffle.Response responseContent = new CroupierShuffle.Response(content.getId(), publicDescCopy, privateDescCopy);
                    ContentMsg response = new BasicContentMsg(responseHeader, responseContent);

                    LOG.trace("{} sending:{} to:{}", new Object[]{logPrefix, responseContent, reqSrc});
                    trigger(response, network);

                    publicView.selectToKeep(reqSrc, content.publicNodes);
                    privateView.selectToKeep(reqSrc, content.privateNodes);
                    if (!connected() && haveShufflePartners()) {
                        startShuffle();
                    }
                }
            };

    ClassMatchedHandler handleShuffleResponse
            = new ClassMatchedHandler<CroupierShuffle.Response, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, CroupierShuffle.Response>>() {

                @Override
                public void handle(CroupierShuffle.Response content, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, CroupierShuffle.Response> container) {
                    DecoratedHeader<DecoratedAddress> header = container.getHeader();
                    if (header.getTrait(OverlayMember.class).getOverlayId() != overlayId) {
                        LOG.error("{} message with header:{} not belonging to croupier overlay:{}", new Object[]{logPrefix, header, overlayId});
                        throw new RuntimeException("message not belonging to croupier overlay");
                    }
                    DecoratedAddress respSrc = container.getHeader().getSource();
                    if (self.getBase().equals(respSrc.getBase())) {
                        LOG.error("{} Tried to shuffle with myself", logPrefix);
                        throw new RuntimeException("tried to shuffle with myself");
                    }
                    LOG.trace("{} received:{} from:{}", new Object[]{logPrefix, content, respSrc});

                    if (shuffleTimeoutId == null) {
                        LOG.debug("{} req:{}  already timed out", new Object[]{logPrefix, content.getId(), respSrc});
                        return;
                    }

                    publicView.selectToKeep(respSrc, content.publicNodes);
                    privateView.selectToKeep(respSrc, content.privateNodes);
                    cancelShuffleTimeout();
                }
            };

    Handler<ShuffleTimeout> handleShuffleTimeout = new Handler<ShuffleTimeout>() {
        @Override
        public void handle(ShuffleTimeout timeout) {
            LOG.info("{} node:{} timed out", logPrefix, timeout.dest);

            shuffleTimeoutId = null;
            if (!NatedTrait.isOpen(timeout.dest)) {
                publicView.timedOut(timeout.dest);
            } else {
                privateView.timedOut(timeout.dest);
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

    private void scheduleShuffleTimeout(DecoratedAddress dest) {
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

    public static class CroupierInit extends Init<CroupierComp> {

        public final CroupierKCWrapper config;
        public final DecoratedAddress self;
        public final int overlayId;
        public final long seed;

        public CroupierInit(KConfigCore configCore, DecoratedAddress self, int overlayId, long seed) {
            this.config = new CroupierKCWrapper(configCore);
            this.self = self;
            this.overlayId = overlayId;
            this.seed = seed;
        }
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

        public final DecoratedAddress dest;

        public ShuffleTimeout(ScheduleTimeout request, DecoratedAddress dest) {
            super(request);
            this.dest = dest;
        }

        @Override
        public String toString() {
            return "SHUFFLE_TIMEOUT";
        }
    }
}

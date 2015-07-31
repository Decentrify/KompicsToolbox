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
import se.sics.kompics.Stop;
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
import se.sics.p2ptoolbox.croupier.util.CroupierView;
import se.sics.p2ptoolbox.util.config.SystemConfig;
import se.sics.p2ptoolbox.util.network.ContentMsg;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.BasicHeader;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;
import se.sics.p2ptoolbox.util.traits.Nated;
import se.sics.p2ptoolbox.util.traits.OverlayMember;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierComp extends ComponentDefinition {

    private final static Logger log = LoggerFactory.getLogger(CroupierComp.class);

    Negative<CroupierControlPort> croupierControlPort = negative(CroupierControlPort.class);
    Negative<CroupierPort> croupierPort = negative(CroupierPort.class);
    Positive<Network> network = requires(Network.class);
    Positive<Timer> timer = requires(Timer.class);

    private final SystemConfig systemConfig;
    private final CroupierConfig croupierConfig;
    private final String logPrefix;
    private final int overlayId;

    private List<DecoratedAddress> bootstrapNodes;
    private Object selfView;
    private CroupierView publicView;
    private CroupierView privateView;

    private UUID shuffleCycleId;
    private UUID shuffleTimeoutId;

    public CroupierComp(CroupierInit init) {
        this.systemConfig = init.systemConfig;
        this.croupierConfig = init.croupierConfig;
        this.overlayId = init.overlayId;
        this.logPrefix = "<oid:" + overlayId + ",nid:" + systemConfig.self.getBase().toString() + ">";
        this.bootstrapNodes = new ArrayList<DecoratedAddress>(systemConfig.bootstrapNodes);
        cleanSelf();

        log.info("{} initiating with bootstrap nodes:{} ...", logPrefix, bootstrapNodes);

        this.selfView = null;
        this.shuffleCycleId = null;
        this.shuffleTimeoutId = null;

        Random rand = new Random(systemConfig.seed + overlayId);
        this.publicView = new CroupierView(systemConfig.self, croupierConfig.viewSize, rand);
        this.privateView = new CroupierView(systemConfig.self, croupierConfig.viewSize, rand);

        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleJoin, croupierControlPort);
        subscribe(handleUpdate, croupierPort);
        subscribe(handleShuffleRequest, network);
        subscribe(handleShuffleResponse, network);
        subscribe(handleShuffleCycle, timer);
        subscribe(handleShuffleTimeout, timer);
    }

    Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.info("{} starting...", logPrefix);
        }

    };

    Handler<Stop> handleStop = new Handler<Stop>() {

        @Override
        public void handle(Stop event) {
            log.info("{} stopping...", logPrefix);
            stopShuffle();
        }

    };

    private void startShuffle() {
        if (selfView == null) {
            log.info("{} no self view - not shuffling", new Object[]{logPrefix});
            return;
        }
        if (!haveShufflePartners()) {
            log.info("{} no partners - not shuffling", new Object[]{logPrefix});
            return;
        }
        log.info("{} started shuffle", new Object[]{logPrefix});
        schedulePeriodicShuffle();
    }

    private void stopShuffle() {
        cancelPeriodicShuffle();
        log.info("{} stopped shuffle", new Object[]{logPrefix});
        trigger(new CroupierDisconnected(overlayId), croupierControlPort);
    }

    private boolean connected() {
        return shuffleCycleId != null;
    }

    private boolean haveShufflePartners() {
        return !bootstrapNodes.isEmpty() || !publicView.isEmpty() || !privateView.isEmpty();
    }

    Handler<CroupierJoin> handleJoin = new Handler<CroupierJoin>() {
        @Override
        public void handle(CroupierJoin join) {
            log.trace("{} {}", logPrefix, join);
            log.debug("{} joining using nodes:{}", logPrefix, join.peers);

            bootstrapNodes.addAll(join.peers);
            cleanSelf();
            if (!connected()) {
                startShuffle();
            }
        }
    };

    private void cleanSelf() {
        Iterator<DecoratedAddress> it = bootstrapNodes.iterator();
        while (it.hasNext()) {
            DecoratedAddress node = it.next();
            if (node.getBase().equals(systemConfig.self.getBase())) {
                it.remove();
            }
        }
    }
    
    Handler<CroupierUpdate> handleUpdate = new Handler<CroupierUpdate>() {
        @Override
        public void handle(CroupierUpdate update) {
            log.trace("{} {}", logPrefix, update);
            log.info("{} updating selfView:{}", new Object[]{logPrefix, update.selfView});

            selfView = (update.selfView == null ? selfView : update.selfView);

            if (!connected()) {
                startShuffle();
            }
        }
    };

    private DecoratedAddress selectPeerToShuffleWith(double temperature) {
        if (!bootstrapNodes.isEmpty()) {
            return bootstrapNodes.remove(0);
        }
        DecoratedAddress node = null;
        if (!publicView.isEmpty()) {
            node = publicView.selectPeerToShuffleWith(croupierConfig.policy, true, temperature);
        } else if (!privateView.isEmpty()) {
            node = privateView.selectPeerToShuffleWith(croupierConfig.policy, true, temperature);
        }
        return node;
    }

    Handler<ShuffleCycle> handleShuffleCycle = new Handler<ShuffleCycle>() {
        @Override
        public void handle(ShuffleCycle event) {
            log.trace("{} {}", logPrefix, event);
            log.debug("{} public view size:{}, private view size:{}, bootstrap nodes size:{}", new Object[]{logPrefix, publicView.size(), privateView.size(), bootstrapNodes.size()});

            if (!haveShufflePartners()) {
                log.warn("{} no shuffle partners - disconnected", logPrefix);
                stopShuffle();
                return;
            }

            if (!publicView.isEmpty() || !privateView.isEmpty()) {
                CroupierSample cs = new CroupierSample(overlayId, publicView.getAllCopy(), privateView.getAllCopy());
                log.info("{} publishing sample \n public nodes:{} \n private nodes:{}", new Object[]{logPrefix, cs.publicSample, cs.privateSample});
                trigger(cs, croupierPort);
            }

            DecoratedAddress peer = selectPeerToShuffleWith(croupierConfig.softMaxTemperature);
            if (peer == null || peer.getBase().equals(systemConfig.self.getBase())) {
                log.error("{} this should not happen - logic error selecting peer", logPrefix);
                throw new RuntimeException("Error selecting peer");
            }

            if (peer.hasTrait(Nated.class)) {
                log.debug("{} did not pick a public node for shuffling - public view size:{}", new Object[]{logPrefix, publicView.getAllCopy().size()});
            }

            // NOTE:
            publicView.incrementDescriptorAges();
            privateView.incrementDescriptorAges();

            Set<CroupierContainer> publicDescCopy = publicView.initiatorCopySet(croupierConfig.shuffleSize, peer);
            Set<CroupierContainer> privateDescCopy = privateView.initiatorCopySet(croupierConfig.shuffleSize, peer);

            if (systemConfig.self.hasTrait(Nated.class)) {
                privateDescCopy.add(new CroupierContainer(systemConfig.self, selfView));
            } else {
                publicDescCopy.add(new CroupierContainer(systemConfig.self, selfView));
            }

            DecoratedHeader<DecoratedAddress> requestHeader = new DecoratedHeader(new BasicHeader(systemConfig.self, peer, Transport.UDP), null, overlayId);
            CroupierShuffle.Request requestContent = new CroupierShuffle.Request(UUID.randomUUID(), publicDescCopy, privateDescCopy);
            ContentMsg request = new BasicContentMsg(requestHeader, requestContent);
            log.trace("{} sending:{} to:{}", new Object[]{logPrefix, requestContent, peer});
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
                        log.error("{} message with header:{} not belonging to croupier overlay:{}", new Object[]{logPrefix, header, overlayId});
                        throw new RuntimeException("message not belonging to croupier overlay");
                    }
                    DecoratedAddress reqSrc = container.getHeader().getSource();
                    if (systemConfig.self.getBase().equals(reqSrc.getBase())) {
                        log.error("{} Tried to shuffle with myself", logPrefix);
                        throw new RuntimeException("tried to shuffle with myself");
                    }
                    log.trace("{} received:{} from:{}", new Object[]{logPrefix, content, reqSrc});
                    if (selfView == null) {
                        log.warn("{} not ready to shuffle - no self view available - {} tried to shuffle with me",
                                logPrefix, reqSrc);
                        return;
                    }

                    log.debug("{} received from:{} \n public nodes:{} \n private nodes:{}",
                            new Object[]{logPrefix, container.getHeader().getSource(), content.publicNodes, content.privateNodes});

                    publicView.incrementDescriptorAges();
                    privateView.incrementDescriptorAges();

                    Set<CroupierContainer> publicDescCopy = publicView.receiverCopySet(croupierConfig.shuffleSize, reqSrc);
                    Set<CroupierContainer> privateDescCopy = privateView.receiverCopySet(croupierConfig.shuffleSize, reqSrc);
                    if (systemConfig.self.hasTrait(Nated.class)) {
                        privateDescCopy.add(new CroupierContainer(systemConfig.self, selfView));
                    } else {
                        publicDescCopy.add(new CroupierContainer(systemConfig.self, selfView));
                    }

                    DecoratedHeader<DecoratedAddress> responseHeader = new DecoratedHeader(new BasicHeader(systemConfig.self, reqSrc, Transport.UDP), null, overlayId);
                    CroupierShuffle.Response responseContent = new CroupierShuffle.Response(content.getId(), publicDescCopy, privateDescCopy);
                    ContentMsg response = new BasicContentMsg(responseHeader, responseContent);

                    log.trace("{} sending:{} to:{}", new Object[]{logPrefix, responseContent, reqSrc});
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
                        log.error("{} message with header:{} not belonging to croupier overlay:{}", new Object[]{logPrefix, header, overlayId});
                        throw new RuntimeException("message not belonging to croupier overlay");
                    }
                    DecoratedAddress respSrc = container.getHeader().getSource();
                    if (systemConfig.self.getBase().equals(respSrc.getBase())) {
                        log.error("{} Tried to shuffle with myself", logPrefix);
                        throw new RuntimeException("tried to shuffle with myself");
                    }
                    log.trace("{} received:{} from:{}", new Object[]{logPrefix, content, respSrc});

                    if (shuffleTimeoutId == null) {
                        log.debug("{} req:{}  already timed out", new Object[]{logPrefix, content.getId(), respSrc});
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
            log.info("{} node:{} timed out", logPrefix, timeout.dest);

            shuffleTimeoutId = null;
            if (!timeout.dest.hasTrait(Nated.class)) {
                publicView.timedOut(timeout.dest);
            } else {
                privateView.timedOut(timeout.dest);
            }
        }
    };

    private void schedulePeriodicShuffle() {
        if (shuffleCycleId != null) {
            log.warn("{} double starting periodic shuffle", logPrefix);
            return;
        }
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(croupierConfig.shufflePeriod, croupierConfig.shufflePeriod);
        ShuffleCycle sc = new ShuffleCycle(spt);
        spt.setTimeoutEvent(sc);
        shuffleCycleId = sc.getTimeoutId();
        trigger(spt, timer);
    }

    private void cancelPeriodicShuffle() {
        if (shuffleCycleId == null) {
            log.warn("{} double stopping periodic shuffle", logPrefix);
            return;
        }
        CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(shuffleCycleId);
        shuffleCycleId = null;
        trigger(cpt, timer);
    }

    private void scheduleShuffleTimeout(DecoratedAddress dest) {
        if (shuffleTimeoutId != null) {
            log.warn("{} double starting shuffle timeout", logPrefix);
            return;
        }
        ScheduleTimeout spt = new ScheduleTimeout(croupierConfig.shufflePeriod / 2);
        ShuffleTimeout sc = new ShuffleTimeout(spt, dest);
        spt.setTimeoutEvent(sc);
        shuffleTimeoutId = sc.getTimeoutId();
        trigger(spt, timer);
    }

    private void cancelShuffleTimeout() {
        if (shuffleTimeoutId == null) {
            log.warn("{} double stopping shuffle timeout", logPrefix);
        }
        CancelTimeout cpt = new CancelTimeout(shuffleTimeoutId);
        shuffleTimeoutId = null;
        trigger(cpt, timer);
    }

    public static class CroupierInit extends Init<CroupierComp> {

        public final SystemConfig systemConfig;
        public final CroupierConfig croupierConfig;
        public final int overlayId;

        public CroupierInit(SystemConfig systemConfig, CroupierConfig config, int overlayId) {
            this.systemConfig = systemConfig;
            this.croupierConfig = config;
            this.overlayId = overlayId;
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
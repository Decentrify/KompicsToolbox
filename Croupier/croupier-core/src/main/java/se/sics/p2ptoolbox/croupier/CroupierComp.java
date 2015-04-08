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
import se.sics.kompics.network.Header;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
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
import se.sics.p2ptoolbox.util.network.ContentMsg;
import se.sics.p2ptoolbox.util.network.NatedAddress;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.BasicOverlayHeader;
import se.sics.p2ptoolbox.util.traits.OverlayMember;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierComp extends ComponentDefinition {

    private final static Logger log = LoggerFactory.getLogger(CroupierComp.class);

    private final CroupierConfig config;
    private final String logPrefix;
    private final NatedAddress selfAddress;
    private final int overlayId;

    Negative<CroupierControlPort> croupierControlPort = negative(CroupierControlPort.class);
    Negative<CroupierPort> croupierPort = negative(CroupierPort.class);
    Positive<Network> network = requires(Network.class);
    Positive<Timer> timer = requires(Timer.class);

    private final List<NatedAddress> bootstrapNodes;
    private Object selfView;
    private CroupierView publicView;
    private CroupierView privateView;

    private UUID shuffleCycleId;
    private UUID shuffleTimeoutId;

    public CroupierComp(CroupierInit init) {
        this.config = init.config;
        this.selfAddress = init.selfAddress;
        this.overlayId = init.overlayId;
        this.logPrefix = "<oid:" + overlayId + ",nid:" + selfAddress + ">";

        log.info("{} initiating...", logPrefix);
        this.bootstrapNodes = init.bootstrapNodes;
        this.selfView = null;
        this.shuffleCycleId = null;
        this.shuffleTimeoutId = null;

        Random rand = new Random(init.seed + overlayId);
        this.publicView = new CroupierView(selfAddress, config.viewSize, rand);
        this.privateView = new CroupierView(selfAddress, config.viewSize, rand);

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
            if (bootstrapNodes.contains(selfAddress)) {
                log.warn("{} trying to bootstrap with myself", new Object[]{logPrefix, overlayId});
                bootstrapNodes.remove(selfAddress);
            }

            if (!connected()) {
                startShuffle();
            }
        }
    };

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

    private NatedAddress selectPeerToShuffleWith(double temperature) {
        if (!bootstrapNodes.isEmpty()) {
            return bootstrapNodes.remove(0);
        }
        NatedAddress node = null;
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
            log.trace("{} {}", logPrefix, event);
            log.debug("{} public view size:{}, private view size:{}", new Object[]{logPrefix, publicView.size(), privateView.size()});

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

            NatedAddress peer = selectPeerToShuffleWith(config.temperature);
            if (peer == null || peer.equals(selfAddress)) {
                log.error("{} this should not happen - logic error selecting peer", logPrefix);
                throw new RuntimeException("Error selecting peer");
            }

            if (!peer.isOpen()) {
                log.debug("{} did not pick a public node for shuffling - public view size:{}", new Object[]{logPrefix, publicView.getAllCopy().size()});
            }

            // NOTE:
            publicView.incrementDescriptorAges();
            privateView.incrementDescriptorAges();

            Set<CroupierContainer> publicDescCopy = publicView.initiatorCopySet(config.shuffleLength, peer);
            Set<CroupierContainer> privateDescCopy = privateView.initiatorCopySet(config.shuffleLength, peer);

            if (selfAddress.isOpen()) {
                publicDescCopy.add(new CroupierContainer(selfAddress, selfView));
            } else {
                privateDescCopy.add(new CroupierContainer(selfAddress, selfView));
            }

            BasicOverlayHeader<NatedAddress> requestHeader = new BasicOverlayHeader(selfAddress, peer, Transport.UDP, overlayId);
            CroupierShuffle.Request requestContent = new CroupierShuffle.Request(UUID.randomUUID(), publicDescCopy, privateDescCopy);
            ContentMsg request = new BasicContentMsg(requestHeader, requestContent);
            log.trace("{} sending:{} to:{}", new Object[]{logPrefix, requestContent, peer});
            trigger(request, network);
            scheduleShuffleTimeout(peer);
        }
    };

    ClassMatchedHandler<CroupierShuffle.Request, ContentMsg<? extends NatedAddress, ? extends Header<? extends NatedAddress>, CroupierShuffle.Request>> handleShuffleRequest
            = new ClassMatchedHandler<CroupierShuffle.Request, ContentMsg<? extends NatedAddress, ? extends Header<? extends NatedAddress>, CroupierShuffle.Request>>() {

                @Override
                public void handle(CroupierShuffle.Request content, ContentMsg<? extends NatedAddress, ? extends Header<? extends NatedAddress>, CroupierShuffle.Request> container) {
                    Header header = container.getHeader();
                    if (!(header instanceof OverlayMember) && ((OverlayMember) header).getOverlayId() != overlayId) {
                        log.error("{} message with header:{} not belonging to croupier overlay:{}", new Object[]{logPrefix, header, overlayId});
                        throw new RuntimeException("message not belonging to croupier overlay");
                    }
                    NatedAddress reqSrc = container.getHeader().getSource();
                    if (selfAddress.equals(reqSrc)) {
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

                    Set<CroupierContainer> publicDescCopy = publicView.receiverCopySet(config.shuffleLength, reqSrc);
                    Set<CroupierContainer> privateDescCopy = privateView.receiverCopySet(config.shuffleLength, reqSrc);
                    if (selfAddress.isOpen()) {
                        publicDescCopy.add(new CroupierContainer(selfAddress, selfView));
                    } else {
                        privateDescCopy.add(new CroupierContainer(selfAddress, selfView));
                    }

                    BasicOverlayHeader<NatedAddress> responseHeader = new BasicOverlayHeader(selfAddress, reqSrc, Transport.UDP, overlayId);
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

    ClassMatchedHandler<CroupierShuffle.Response, ContentMsg<NatedAddress, Header<NatedAddress>, CroupierShuffle.Response>> handleShuffleResponse
            = new ClassMatchedHandler<CroupierShuffle.Response, ContentMsg<NatedAddress, Header<NatedAddress>, CroupierShuffle.Response>>() {

                @Override
                public void handle(CroupierShuffle.Response content, ContentMsg<NatedAddress, Header<NatedAddress>, CroupierShuffle.Response> container) {
                    Header header = container.getHeader();
                    if (!(header instanceof OverlayMember) && ((OverlayMember) header).getOverlayId() != overlayId) {
                        log.error("{} message with header:{} not belonging to croupier overlay:{}", new Object[]{logPrefix, header, overlayId});
                        throw new RuntimeException("message not belonging to croupier overlay");
                    }
                    NatedAddress respSrc = container.getHeader().getSource();
                    if (selfAddress.equals(respSrc)) {
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
            if (timeout.dest.isOpen()) {
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
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(config.shufflePeriod, config.shufflePeriod);
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
        CancelTimeout cpt = new CancelTimeout(shuffleCycleId);
        shuffleCycleId = null;
        trigger(cpt, timer);
    }

    private void scheduleShuffleTimeout(NatedAddress dest) {
        if (shuffleTimeoutId != null) {
            log.warn("{} double starting shuffle timeout", logPrefix);
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
            log.warn("{} double stopping shuffle timeout", logPrefix);
        }
        CancelTimeout cpt = new CancelTimeout(shuffleTimeoutId);
        shuffleTimeoutId = null;
        trigger(cpt, timer);
    }

    public static class CroupierInit extends Init<CroupierComp> {

        public final long seed;
        public final CroupierConfig config;
        public final int overlayId;
        public final NatedAddress selfAddress;
        public final List<NatedAddress> bootstrapNodes;

        public CroupierInit(CroupierConfig config, int overlayId, NatedAddress selfAddress, List<NatedAddress> bootstrapNodes, long seed) {
            this.config = config;
            this.seed = seed;
            this.overlayId = overlayId;
            this.selfAddress = selfAddress;
            this.bootstrapNodes = bootstrapNodes;
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

        public final NatedAddress dest;

        public ShuffleTimeout(ScheduleTimeout request, NatedAddress dest) {
            super(request);
            this.dest = dest;
        }

        @Override
        public String toString() {
            return "SHUFFLE_TIMEOUT";
        }
    }
}

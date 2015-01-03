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
package se.sics.p2ptoolbox.croupier.core;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.timer.CancelPeriodicTimeout;
import se.sics.gvod.timer.CancelTimeout;
import se.sics.gvod.timer.SchedulePeriodicTimeout;
import se.sics.gvod.timer.ScheduleTimeout;
import se.sics.gvod.timer.TimeoutId;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Stop;
import se.sics.p2ptoolbox.croupier.api.CroupierControlPort;
import se.sics.p2ptoolbox.croupier.api.CroupierPort;
import se.sics.p2ptoolbox.croupier.api.msg.CroupierDisconnected;
import se.sics.p2ptoolbox.croupier.api.msg.CroupierJoin;
import se.sics.p2ptoolbox.croupier.api.msg.CroupierSample;
import se.sics.p2ptoolbox.croupier.api.msg.CroupierUpdate;
import se.sics.p2ptoolbox.croupier.api.util.CroupierPeerView;
import se.sics.p2ptoolbox.croupier.api.util.PeerView;
import se.sics.p2ptoolbox.croupier.core.msg.Shuffle;
import se.sics.p2ptoolbox.croupier.core.msg.ShuffleCycle;
import se.sics.p2ptoolbox.croupier.core.msg.ShuffleNet;
import se.sics.p2ptoolbox.croupier.core.msg.ShuffleTimeout;
import se.sics.p2ptoolbox.croupier.core.util.CroupierStats;
import se.sics.p2ptoolbox.croupier.core.util.CroupierView;

/**
 *
 */
public class Croupier extends ComponentDefinition {

    private final static Logger log = LoggerFactory.getLogger(Croupier.class);

    private final CroupierConfig config;
    private final String croupierLogPrefix;
    private final VodAddress selfAddress;
    private final int overlayId;

    Negative<CroupierControlPort> croupierControlPort = negative(CroupierControlPort.class);
    Negative<CroupierPort> croupierPort = negative(CroupierPort.class);
    Positive<VodNetwork> network = requires(VodNetwork.class);
    Positive<Timer> timer = requires(Timer.class);

    private final List<VodAddress> bootstrapNodes;
    private PeerView selfView;
    private CroupierView publicView;
    private CroupierView privateView;

    private TimeoutId shuffleCycleId;
    private TimeoutId shuffleTimeoutId;

    public Croupier(CroupierInit init) {
        this.config = init.config;
        this.selfAddress = init.selfAddress;
        this.overlayId = init.overlayId;
        this.croupierLogPrefix = "CROUPIER<oid:" + overlayId + ",nid:" + selfAddress.getId() + ">";

        log.info("{} creating...", croupierLogPrefix);
        this.bootstrapNodes = new ArrayList<VodAddress>();
        this.selfView = null;
        this.publicView = new CroupierView(selfAddress, config.viewSize, init.rand);
        this.privateView = new CroupierView(selfAddress, config.viewSize, init.rand);
        CroupierStats.addNode(selfAddress);

        subscribe(handleStop, control);
        subscribe(handleJoin, croupierControlPort);
        subscribe(handleUpdate, croupierPort);
    }

    Handler<Stop> handleStop = new Handler<Stop>() {

        @Override
        public void handle(Stop event) {
            stopShuffle();
            log.info("{} stopped", croupierLogPrefix);
        }

    };

    Handler<CroupierJoin> handleJoin = new Handler<CroupierJoin>() {
        @Override
        public void handle(CroupierJoin join) {
            log.info("{} {}", croupierLogPrefix, join);
            log.debug("{} joining using nodes:{}", croupierLogPrefix, join.peers);

            bootstrapNodes.addAll(join.peers);
            if (bootstrapNodes.contains(selfAddress)) {
                log.warn("{} trying to bootstrap with myself", new Object[]{croupierLogPrefix, overlayId});
                bootstrapNodes.remove(selfAddress);
            }

            tryInitiateShuffle();
        }
    };

    Handler<CroupierUpdate> handleUpdate = new Handler<CroupierUpdate>() {
        @Override
        public void handle(CroupierUpdate update) {
            log.info("{} updating selfView:{}", new Object[]{croupierLogPrefix, update.selfView});

            selfView = (update.selfView == null ? selfView : update.selfView);

            if (shuffleCycleId == null) {
                tryInitiateShuffle();
            }
        }
    };

    Handler<ShuffleTimeout> handleTimeout = new Handler<ShuffleTimeout>() {
        @Override
        public void handle(ShuffleTimeout timeout) {
            log.debug("{} shuffle to {} timed out", new Object[]{croupierLogPrefix, timeout.dest});
            publicView.timedOut(timeout.dest);
            privateView.timedOut(timeout.dest);
        }
    };

    private VodAddress selectPeerToShuffleWith() {
        if (!bootstrapNodes.isEmpty()) {
            return bootstrapNodes.remove(0);
        }
        VodAddress node = null;
        if (!publicView.isEmpty()) {
            node = publicView.selectPeerToShuffleWith(config.policy, true, 0.75d);
        } else if (!privateView.isEmpty()) {
            node = privateView.selectPeerToShuffleWith(config.policy, true, 0.85d);
        }
        return node;
    }

    Handler<ShuffleCycle> handleShuffleCycle = new Handler<ShuffleCycle>() {
        @Override
        public void handle(ShuffleCycle event) {
            log.debug("{} public view size:{}, private view size:{}", new Object[]{croupierLogPrefix, publicView.size(), privateView.size()});

            if (!publicView.isEmpty() || !privateView.isEmpty()) {
                CroupierSample cs = new CroupierSample(UUID.randomUUID(), overlayId, publicView.getAll(), privateView.getAll());
                log.info("{} publishing sample \n public nodes:{} \n private nodes:{}", new Object[]{croupierLogPrefix, cs.publicSample, cs.privateSample});
                trigger(cs, croupierPort);
            }
            // If I don't have any references to any public nodes and I am a global Croupier
            // disconnect
            if (bootstrapNodes.isEmpty() && publicView.isEmpty()) {
                log.warn("{} disconnected", croupierLogPrefix);
                trigger(new CroupierDisconnected(UUID.randomUUID(), overlayId), croupierControlPort);
                cancelShuffleCycle();
                return;
            }
            VodAddress peer = selectPeerToShuffleWith();
            if (peer == null || peer.equals(selfAddress)) {
                throw new RuntimeException("Error selecting peer");
            }

            if (!peer.isOpen()) {
                log.debug("{} did not pick a public node for shuffling - public view size:{}", new Object[]{croupierLogPrefix, publicView.getAll().size()});
            }

            CroupierStats.instance(selfAddress).incSelectedTimes();
            shuffle(config.shuffleLength, peer);
            scheduleShuffleTimeout(peer);
            //TODO Alex - is it correct to increment descriptors here only? 
            publicView.incrementDescriptorAges();
            privateView.incrementDescriptorAges();

        }
    };

    Handler<ShuffleNet.Request> handleShuffleRequest = new Handler<ShuffleNet.Request>() {
        @Override
        public void handle(ShuffleNet.Request req) {
            log.debug("{} received {}", new Object[]{croupierLogPrefix, req});
            log.trace("{} received from:{} \n public nodes:{} \n private nodes:{}",
                    new Object[]{croupierLogPrefix, req.getVodSource(), req.content.publicNodes, req.content.privateNodes});

            if (selfAddress.equals(req.getVodSource())) {
                log.warn("{} Tried to shuffle with myself", croupierLogPrefix);
                return;
            }

            CroupierStats.instance(selfAddress).incShuffleRecvd(req.getVodSource());
            List<CroupierPeerView> toSendPublicDescs = publicView.selectToSendAtReceiver(config.shuffleLength, req.getVodSource());
            List<CroupierPeerView> toSendPrivateDescs = privateView.selectToSendAtReceiver(config.shuffleLength, req.getVodSource());
            ShuffleNet.Response resp = req.getResponse(new Shuffle(toSendPublicDescs, toSendPrivateDescs));

            publicView.selectToKeep(req.getVodSource(), req.content.publicNodes);
            privateView.selectToKeep(req.getVodSource(), req.content.privateNodes);

            log.debug("{} sending {}", croupierLogPrefix, resp);
            log.trace("{} sending to:{} \n public nodes:{} \n private nodes:{}",
                    new Object[]{croupierLogPrefix, resp.getVodDestination(), resp.content.publicNodes, resp.content.privateNodes});

            trigger(resp, network);
        }
    };

    Handler<ShuffleNet.Response> handleShuffleResponse = new Handler<ShuffleNet.Response>() {
        @Override
        public void handle(ShuffleNet.Response resp) {
            if (shuffleTimeoutId == null) {
                log.debug("{} received {}, but it already timed out", new Object[]{croupierLogPrefix, resp});
                return;
            } else {
                log.debug("{} received {}", new Object[]{croupierLogPrefix, resp});
                log.trace("{} received from:{} \n public nodes:{} \n private nodes:{}",
                        new Object[]{croupierLogPrefix, resp.getVodSource(), resp.content.publicNodes, resp.content.privateNodes});
                cancelShuffleTimeout();
            }

            CroupierStats.instance(selfAddress).incShuffleResp();

            publicView.selectToKeep(resp.getVodSource(), resp.content.publicNodes);
            privateView.selectToKeep(resp.getVodSource(), resp.content.privateNodes);
        }
    };
    Handler<ShuffleTimeout> handleShuffleTimeout = new Handler<ShuffleTimeout>() {
        @Override
        public void handle(ShuffleTimeout timeout) {
            log.info("{} node: {} timed out", croupierLogPrefix, timeout.dest);
            CroupierStats.instance(selfAddress).incShuffleTimeout();

            if (timeout.dest.isOpen()) {
                publicView.timedOut(timeout.dest);
            } else {
                privateView.timedOut(timeout.dest);
            }
        }
    };

    private void tryInitiateShuffle() {
        if (selfView == null) {
            log.info("{} no self view - not shuffling", new Object[]{croupierLogPrefix, overlayId});
            return;
        }
        if (bootstrapNodes.isEmpty()) {
            log.info("{} no insiders - not shuffling", new Object[]{croupierLogPrefix, overlayId});
            return;
        }
        log.info("{} initiating shuffle", new Object[]{croupierLogPrefix});
        scheduleShuffleCycle();

        subscribe(handleShuffleRequest, network);
        subscribe(handleShuffleResponse, network);
        subscribe(handleShuffleCycle, timer);
        subscribe(handleTimeout, timer);
    }

    private void stopShuffle() {
        if (shuffleTimeoutId != null) {
            cancelShuffleTimeout();
        }
        if (shuffleCycleId != null) {
            cancelShuffleCycle();
        }
        log.info("{} stopping shuffle", new Object[]{croupierLogPrefix});
        unsubscribe(handleShuffleRequest, network);
        unsubscribe(handleShuffleResponse, network);
        unsubscribe(handleShuffleCycle, timer);
        unsubscribe(handleTimeout, timer);
    }

    private void shuffle(int shuffleSize, VodAddress node) {
        List<CroupierPeerView> publicDescriptors = publicView.selectToSendAtInitiator(shuffleSize, node);
        List<CroupierPeerView> privateDescriptors = privateView.selectToSendAtInitiator(shuffleSize, node);

        if (selfAddress.isOpen()) {
            publicDescriptors.add(new CroupierPeerView(selfView, selfAddress));
        } else {
            privateDescriptors.add(new CroupierPeerView(selfView, selfAddress));
        }

        Shuffle content = new Shuffle(publicDescriptors, privateDescriptors);
        ShuffleNet.Request req = new ShuffleNet.Request(selfAddress, node, UUID.randomUUID(), overlayId, content);
        log.debug("{} sending {}", croupierLogPrefix, req);
        trigger(req, network);
    }

    private void scheduleShuffleTimeout(VodAddress dest) {
        ScheduleTimeout spt = new ScheduleTimeout(config.shufflePeriod/2);
        ShuffleTimeout sc = new ShuffleTimeout(spt, overlayId, dest);
        spt.setTimeoutEvent(sc);
        shuffleTimeoutId = sc.getTimeoutId();

        log.debug("{} scheduling {}", new Object[]{croupierLogPrefix, sc});
        trigger(spt, timer);
    }

    private void cancelShuffleTimeout() {
        log.debug("{} canceling shuffle timeout", new Object[]{croupierLogPrefix});
        CancelTimeout cpt = new CancelTimeout(shuffleTimeoutId);
        trigger(cpt, timer);
        shuffleTimeoutId = null;
    }

    private void scheduleShuffleCycle() {
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(config.shufflePeriod, config.shufflePeriod);
        ShuffleCycle sc = new ShuffleCycle(spt, overlayId);
        spt.setTimeoutEvent(sc);
        shuffleCycleId = sc.getTimeoutId();

        log.debug("{} scheduling {} with period:{}", new Object[]{croupierLogPrefix, sc, config.shufflePeriod});
        trigger(spt, timer);
    }

    private void cancelShuffleCycle() {
        log.debug("{} canceling shuffle cycle", new Object[]{croupierLogPrefix});
        CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(shuffleCycleId);
        trigger(cpt, timer);
        shuffleCycleId = null;
    }

    public static class CroupierInit extends Init<Croupier> {

        public final Random rand;
        public final CroupierConfig config;
        public final int overlayId;
        public final VodAddress selfAddress;

        public CroupierInit(CroupierConfig config, Random rand, int overlayId, VodAddress selfAddress) {
            this.config = config;
            this.rand = rand;
            this.overlayId = overlayId;
            this.selfAddress = selfAddress;
        }
    }
}

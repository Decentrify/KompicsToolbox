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
package se.sics.p2ptoolbox.croupier.core;

import se.sics.p2ptoolbox.croupier.api.CroupierControlPort;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.croupier.CroupierConfig;
import se.sics.gvod.croupier.CroupierPort;
import se.sics.gvod.croupier.msg.intern.CroupierNettyMsg;
import se.sics.gvod.croupier.msg.intern.Shuffle;
import se.sics.gvod.croupier.msg.intern.ShuffleCycle;
import se.sics.gvod.croupier.pub.msg.CroupierDisconnected;
import se.sics.gvod.croupier.pub.msg.CroupierJoin;
import se.sics.gvod.croupier.pub.util.PeerView;
import se.sics.gvod.croupier.pub.util.PublicViewFilter;
import se.sics.gvod.croupier.util.View;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.timer.SchedulePeriodicTimeout;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Croupier extends ComponentDefinition {

    private static final Logger logger = LoggerFactory.getLogger(Croupier.class);

    Negative<CroupierControlPort> croupierControlPort = provides(CroupierControlPort.class);
    Negative<CroupierPort> croupierPort = provides(CroupierPort.class);
    Positive<VodNetwork> network = requires(VodNetwork.class);
    Positive<Timer> timer = requires(Timer.class);

    private final CroupierConfig config;
    private final int croupierId;
    private final String compName;
    private final PublicViewFilter.Base isPublicView;
    private PeerView self;

    private final View publicView;
    private final View privateView;

    private boolean firstSuccessfulShuffle = false;

    private Map<Integer, Long> shuffleTimes = new HashMap<Integer, Long>();

    private final List<VodAddress> bootstrapNodes;

    public Croupier(CroupierInit init) {

        this.config = init.config;
        this.croupierId = init.overlayId;
        this.self = init.self;
        this.isPublicView = init.isPublicView;

        this.compName = "(" + self.getAddress().getId() + ", " + self.getOverlayId() + ") ";
        this.publicView = new View(self, config.rand, config.viewSize);
        this.privateView = new View(self, config.rand, config.viewSize);
        this.bootstrapNodes = new ArrayList<VodAddress>();

        subscribe(handleJoin, croupierControlPort);
    }

    Handler<CroupierJoin> handleJoin = new Handler<CroupierJoin>() {
        @Override
        public void handle(CroupierJoin join) {
            logger.debug("{} joining using nodes:{}", compName, join.peers.size());

            if (!initializeCaches(join.peers)) {
                logger.warn("{} no insiders, not shuffling", compName);
                return;
            } else {
                logger.trace("{} initiate periodic shuffle", compName);
                SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(config.shufflePeriod, config.shufflePeriod);
                spt.setTimeoutEvent(new ShuffleCycle(spt, self.getOverlayId()));
                trigger(spt, timer);
            }
        }
    };

    private boolean initializeCaches(Set<VodAddress> bootstrapNodes) {
        if (bootstrapNodes == null || bootstrapNodes.isEmpty()) {
            return false;
        }
        for (VodAddress peer : bootstrapNodes) {
            if (self.getAddress().equals(peer)) {
                logger.warn("{} Trying to bootstrap with myself: {}", compName, self.getAddress());
            } else {
                bootstrapNodes.add(peer);
            }
        }
        return true;
    }

    private VodAddress selectPeerToShuffleWith() {
        VodAddress node = null;
        if (!bootstrapNodes.isEmpty()) {
            node = bootstrapNodes.remove(0);
        } else if (!publicView.isEmpty()) {
            node = publicView.selectPeerToShuffleWith(config.policy, true, 0.75d);
        } else if (!privateView.isEmpty()) {
            node = privateView.selectPeerToShuffleWith(config.policy, true, 0.85d);
        }
        return node;
    }

    private void shuffle(int shuffleSize, VodAddress node) {
        Assert.assertFalse(node == null);
        Assert.assertNotEquals(, this);
        
        List<PeerView> publicDescriptors = publicView.selectToSendAtInitiator(shuffleSize, node);
        List<PeerView> privateDescriptors = privateView.selectToSendAtInitiator(shuffleSize, node);

        if (isPublic(self)) {
            publicDescriptors.add(self);
        } else {
            privateDescriptors.add(self);
        }

        Shuffle.Request req = new Shuffle.Request(UUID.randomUUID(), croupierId, publicDescriptors, privateDescriptors, self);
        CroupierNettyMsg.Request<Shuffle.Request> netReq = new CroupierNettyMsg.Request<Shuffle.Request>(self.getAddress(), node, req);

        logger.debug("{} sending {}", compName, netReq);
        trigger(netReq, network);
    }

    private boolean isPublic(PeerView peer) {
        if (isPublicView instanceof PublicViewFilter.Simple) {
            PublicViewFilter.Simple simpleFilter = (PublicViewFilter.Simple) isPublicView;
            return simpleFilter.apply(peer);
        } else if (isPublicView instanceof PublicViewFilter.CompareToSelf) {
            PublicViewFilter.CompareToSelf compareToSelf = (PublicViewFilter.CompareToSelf) isPublicView;
            return compareToSelf.apply(self, peer);
        }
        throw new RuntimeException("unknown filter type");
    }

    Handler<ShuffleCycle> handleCycle = new Handler<ShuffleCycle>() {
        @Override
        public void handle(ShuffleCycle event) {
            logger.trace("{} shuffle time - current public view size:{}, private view size:{}",
                    new Object[]{compName, publicView.size(), privateView.size()});

            VodAddress peer = selectPeerToShuffleWith();
            if (peer == null) {
                logger.warn("{} croupier {} disconnected", compName, croupierId);
                trigger(new CroupierDisconnected(croupierId), croupierControlPort);
                return;
            }

            shuffle(config.shuffleLength, peer);
            publicView.incrementDescriptorAges();
            privateView.incrementDescriptorAges();
        }
    };

    /**
     * handle requests to shuffle
     */
    Handler<ShuffleMsg.Request> handleShuffleRequest = new Handler<ShuffleMsg.Request>() {
        @Override
        public void handle(ShuffleMsg.Request msg) {
            logger.debug(compName + "shuffle_req recvd by {} from {} with timeoutId: " + msg.getTimeoutId(),
                    msg.getVodDestination(),
                    msg.getDesc().getVodAddress());

            if (msg.getVodSource().getId() == self.getId()) {
                logger.warn("Tried to shuffle with myself");
                return;
            }

            VodAddress srcAddress = msg.getDesc().getVodAddress();
            CroupierStats.instance(self).incShuffleRecvd(msg.getVodSource());
            DescriptorBuffer recBuffer = msg.getBuffer();
            List<VodDescriptor> recPublicDescs = recBuffer.getPublicDescriptors();
            List<VodDescriptor> recPrivateDescs = recBuffer.getPublicDescriptors();
            List<VodDescriptor> toSendPublicDescs = publicView.selectToSendAtReceiver(
                    recPublicDescs.size(), srcAddress);
            List<VodDescriptor> toSendPrivateDescs = privateView.selectToSendAtReceiver(
                    recPrivateDescs.size(), srcAddress);

            DescriptorBuffer toSendBuffer
                    = new DescriptorBuffer(self.getAddress(), toSendPublicDescs, toSendPrivateDescs);

            publicView.selectToKeep(srcAddress, recBuffer.getPublicDescriptors());
            privateView.selectToKeep(srcAddress, recBuffer.getPrivateDescriptors());

            logger.trace(compName + "SHUFFLE_REQ from {}. r={} public + {} private s={} public + {} private", new Object[]{srcAddress.getId(),
                recPublicDescs.size(), recPrivateDescs.size(), toSendPublicDescs.size(), toSendPrivateDescs.size()});

            logger.trace(compName + " Next dest is: " + msg.getNextDest());

            ShuffleMsg.Response response = new ShuffleMsg.Response(self.getAddress(),
                    msg.getVodSource(), msg.getClientId(), msg.getRemoteId(),
                    msg.getNextDest(), msg.getTimeoutId(), RelayMsgNetty.Status.OK,
                    toSendBuffer, self.getDescriptor());

            logger.trace(compName + "sending ShuffleMsg.Response");

            delegator.doTrigger(response, network);

            publishSample();

        }
    };
    /**
     * handle the response to a shuffle with the partial view of the other node
     */
    Handler<ShuffleMsg.Response> handleShuffleResponse = new Handler<ShuffleMsg.Response>() {
        @Override
        public void handle(ShuffleMsg.Response event) {
            logger.trace(compName + "shuffle_res from {} with ID {}", event.getVodSource().getId(),
                    event.getTimeoutId());
            if (delegator.doCancelRetry(event.getTimeoutId())) {

                if (self.getAddress() == null) {
                    logger.warn(compName + "self is null, not handling Shuffle Response");
                    return;
                }

                CroupierStats.instance(self).incShuffleResp();

                Long timeStarted = shuffleTimes.get(event.getTimeoutId().getId());
                if (timeStarted != null) {
                    RTTStore.addSample(self.getId(), event.getVodSource(), System.currentTimeMillis() - timeStarted);
                    logger.debug(compName + "Adding a RTT sample. TimeoutId: {}. Rtt={}", event.getTimeoutId().getId(), timeStarted);
                } else {
                    logger.warn(compName + "Time started was null when trying to add a RTT sample. TimeoutId: {}",
                            event.getTimeoutId().getId());
                    StringBuilder sb = new StringBuilder();
                    sb.append(compName).append("Existing timestamp ids: ");
                    for (Integer k : shuffleTimes.keySet()) {
                        sb.append(k).append(", ");
                    }
                    logger.warn(sb.toString());
                }
                shuffleTimes.remove(event.getTimeoutId().getId());
                if (!firstSuccessfulShuffle) {
                    firstSuccessfulShuffle = true;
                    delegator.doTrigger(new CroupierJoinCompleted(), croupierControlPort);
                }

                VodDescriptor srcDesc = event.getDesc();
                DescriptorBuffer recBuffer = event.getBuffer();
                List<VodDescriptor> recPublicDescs = recBuffer.getPublicDescriptors();
                List<VodDescriptor> recPrivateDescs = recBuffer.getPrivateDescriptors();

                publicView.selectToKeep(srcDesc.getVodAddress(), recPublicDescs);
                privateView.selectToKeep(srcDesc.getVodAddress(), recPrivateDescs);

                // If I don't have a RTT for new public descriptors, add one to the RTTStore
                // with a default RTO for the descriptor.
                for (VodDescriptor vd : recPublicDescs) {
                    if (!RTTStore.containsPublicSample(self.getId(), vd.getVodAddress())) {
                        RTTStore.addSample(self.getId(), vd.getVodAddress(),
                                config.getRto());
                    }
                }

                // send the new samples to other components
                publishSample();
            }
        }
    };

    public static class CroupierInit extends Init<Croupier> {

        public final CroupierConfig config;
        public final int overlayId;
        public final PeerView self;
        public final PublicViewFilter.Base isPublicView;

        public CroupierInit(CroupierConfig config, PeerView self, PublicViewFilter.Base isPublicView, int overlayId) {
            this.config = config;
            this.overlayId = overlayId;
            this.self = self;
            this.isPublicView = isPublicView;
        }
    }
}

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.util.update.view.ViewUpdatePort;
import se.sics.ktoolbox.croupier.event.CroupierControl;
import se.sics.ktoolbox.croupier.event.CroupierSample;
import se.sics.ktoolbox.croupier.msg.CroupierShuffle;
import se.sics.ktoolbox.croupier.behaviour.CroupierBehaviour;
import se.sics.ktoolbox.croupier.behaviour.CroupierObserver;
import se.sics.ktoolbox.croupier.event.CroupierDisconnected;
import se.sics.ktoolbox.croupier.event.CroupierEvent;
import se.sics.ktoolbox.croupier.event.CroupierJoin;
import se.sics.ktoolbox.croupier.history.DeleteAndForgetHistory;
import se.sics.ktoolbox.croupier.history.ShuffleHistory;
import se.sics.ktoolbox.croupier.util.CroupierSpeed;
import se.sics.ktoolbox.croupier.view.LocalView;
import se.sics.ktoolbox.util.address.AddressUpdate;
import se.sics.ktoolbox.util.address.AddressUpdatePort;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.network.basic.DecoratedHeader;
import se.sics.ktoolbox.util.network.nat.NatAwareAddress;
import se.sics.ktoolbox.util.network.nat.NatType;
import se.sics.ktoolbox.util.other.Container;
import se.sics.ktoolbox.util.update.view.BootstrapView;
import se.sics.ktoolbox.util.update.view.OverlayViewUpdate;
import se.sics.ktoolbox.util.update.view.View;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierComp extends ComponentDefinition {

    // INFO - status, memory statistics 
    // DEBUG - bootstraping, samples 
    // TRACE - protocol messages
    private final static Logger LOG = LoggerFactory.getLogger(CroupierComp.class);
    private String logPrefix = "";

    Negative<CroupierControlPort> croupierControlPort = negative(CroupierControlPort.class);
    Negative<CroupierPort> croupierPort = negative(CroupierPort.class);
    Positive<CroupierPort> bootstrapPort = positive(CroupierPort.class);
    Positive<Network> network = requires(Network.class);
    Positive<Timer> timer = requires(Timer.class);
    Positive<AddressUpdatePort> addressUpdate = requires(AddressUpdatePort.class);
    Positive<ViewUpdatePort> viewUpdate = requires(ViewUpdatePort.class);

    private final SystemKCWrapper systemConfig;
    private final CroupierKCWrapper croupierConfig;
    private final Identifier overlayId;
    private CroupierBehaviour behaviour;
    private final List<NatAwareAddress> bootstrapNodes = new ArrayList<>();
    private final Pair<LocalView, ShuffleHistory> publicView;
    private final Pair<LocalView, ShuffleHistory> privateView;

    private Pair<UUID, Long> shuffleCycle = Pair.with(null, 0l);
    private UUID shuffleTid;

    public CroupierComp(CroupierInit init) {
        systemConfig = new SystemKCWrapper(config());
        croupierConfig = new CroupierKCWrapper(config());
        overlayId = init.overlayId;
        logPrefix = "<nid:" + systemConfig.id + ",oid:" + overlayId + "> ";
        LOG.info("{}initiating...", logPrefix);

        behaviour = new CroupierObserver(init.selfAdr);
        LocalView publicLocalView = new LocalView(croupierConfig, new Random(systemConfig.seed + overlayId.partition(Integer.MAX_VALUE)));
        ShuffleHistory publicShuffleHistory = new DeleteAndForgetHistory();
        publicView = Pair.with(publicLocalView, publicShuffleHistory);
        LocalView privateLocalView = new LocalView(croupierConfig, new Random(systemConfig.seed + overlayId.partition(Integer.MAX_VALUE)));
        ShuffleHistory privateShuffleHistory = new DeleteAndForgetHistory();
        privateView = Pair.with(privateLocalView, privateShuffleHistory);

        subscribe(handleStart, control);
        subscribe(handleLegacyBootstrap, croupierControlPort);
        subscribe(handleReBootstrap, bootstrapPort);
        subscribe(handleViewUpdate, viewUpdate);
        subscribe(handleAddressUpdate, addressUpdate);
        subscribe(handleShuffleRequest, network);
        subscribe(handleShuffleResponse, network);
        subscribe(handleShuffleCycle, timer);
        subscribe(handleShuffleTimeout, timer);
    }

    //****************************CONTROL***************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            schedulePeriodicShuffle(croupierConfig.shufflePeriod);
//            trigger(new AddressUpdate.Request(UUIDIdentifier.randomId()), addressUpdate);
//            trigger(new OverlayViewUpdate.Request(UUIDIdentifier.randomId()), viewUpdate);
        }
    };

    Handler handleAddressUpdate = new Handler<AddressUpdate.Indication>() {
        @Override
        public void handle(AddressUpdate.Indication update) {
            LOG.info("{}update self address:{}", new Object[]{logPrefix, update.localAddress});
            behaviour = behaviour.processAddress(update);
            if (shuffleCycle == null) {
                schedulePeriodicShuffle(croupierConfig.shufflePeriod);
            }
        }
    };

    Handler handleViewUpdate = new Handler<OverlayViewUpdate.Indication<View>>() {
        @Override
        public void handle(OverlayViewUpdate.Indication<View> viewUpdate) {
            LOG.info("{}update observer:{} view:{}", new Object[]{logPrefix, viewUpdate.observer, viewUpdate.view});
            behaviour = behaviour.processView(viewUpdate);
        }
    };
    
    Handler handleLegacyBootstrap = new Handler<CroupierJoin>() {
        @Override
        public void handle(CroupierJoin join) {
            LOG.info("{}bootstraping with:{}", new Object[]{logPrefix, join.bootstrap});
            for(NatAwareAddress bootstrap : join.bootstrap) {
                if(!behaviour.getSelf().getId().equals(bootstrap.getId())) {
                    bootstrapNodes.add(bootstrap);
                }
            }
        }
    };

    Handler handleReBootstrap = new Handler<CroupierSample<BootstrapView>>() {
        @Override
        public void handle(CroupierSample<BootstrapView> sample) {
            while (bootstrapNodes.size() < croupierConfig.viewSize) {
                for (Container<KAddress, BootstrapView> container : sample.publicSample.values()) {
                    if (container.getContent().memberOf(overlayId)
                            && !container.getSource().getId().equals(behaviour.getSelf().getId())) {
                        LOG.debug("{}rebootstrap node:{}", logPrefix, container.getSource());
                        bootstrapNodes.add((NatAwareAddress) container.getSource());
                    }
                }
            }
        }
    };
    //**************************************************************************
    Handler<ShuffleCycle> handleShuffleCycle = new Handler<ShuffleCycle>() {
        @Override
        public void handle(ShuffleCycle event) {
            LOG.trace("{}{}", logPrefix, event);

            publishSample();
            if (connected()) {
                incrementAges();
                NatAwareAddress shufflePartner = selectPeerToShuffleWith();
                Map publicSample = publicView.getValue0().initiatorSample(publicView.getValue1(), shufflePartner);
                Map privateSample = privateView.getValue0().initiatorSample(privateView.getValue1(), shufflePartner);
                sendShuffleRequest(shufflePartner, publicSample, privateSample);
                scheduleShuffleTimeout(shufflePartner);
            } else {
                LOG.warn("{}no partners - not shuffling", new Object[]{logPrefix});
                //TODO Alex Disconnected -  legacy code
                trigger(new CroupierDisconnected(overlayId), croupierControlPort);
                trigger(new CroupierControl(overlayId, CroupierSpeed.FAST), croupierControlPort);
            }
        }
    };

    private void sendShuffleRequest(NatAwareAddress to, Map publicSample, Map privateSample) {
        DecoratedHeader requestHeader
                = new DecoratedHeader(new BasicHeader(behaviour.getSelf(), to, Transport.UDP), overlayId);
        CroupierShuffle.Request requestContent
                = new CroupierShuffle.Request(UUIDIdentifier.randomId(), behaviour.getView(), publicSample, privateSample);
        BasicContentMsg request = new BasicContentMsg(requestHeader, requestContent);
        LOG.trace("{}sending:{} to:{}", new Object[]{logPrefix, requestContent, request.getDestination()});
        trigger(request, network);
    }

    public void sendShuffleResponse(NatAwareAddress to, Map publicSample, Map privateSample) {
        DecoratedHeader responseHeader
                = new DecoratedHeader(new BasicHeader(behaviour.getSelf(), to, Transport.UDP), overlayId);
        CroupierShuffle.Response responseContent
                = new CroupierShuffle.Response(UUIDIdentifier.randomId(), behaviour.getView(), publicSample, privateSample);
        BasicContentMsg response = new BasicContentMsg(responseHeader, responseContent);
        LOG.trace("{}sending:{} to:{}", new Object[]{logPrefix, responseContent, response.getDestination()});
        trigger(response, network);
    }

    ClassMatchedHandler handleShuffleRequest
            = new ClassMatchedHandler<CroupierShuffle.Request, BasicContentMsg<NatAwareAddress, DecoratedHeader<NatAwareAddress>, CroupierShuffle.Request>>() {

                @Override
                public void handle(CroupierShuffle.Request content, BasicContentMsg<NatAwareAddress, DecoratedHeader<NatAwareAddress>, CroupierShuffle.Request> container) {
                    NatAwareAddress shufflePartner = container.getSource();
                    LOG.trace("{}received:{} from:{}", new Object[]{logPrefix, content, shufflePartner});

                    Map publicSample = publicView.getValue0().initiatorSample(publicView.getValue1(), shufflePartner);
                    Map privateSample = privateView.getValue0().initiatorSample(privateView.getValue1(), shufflePartner);
                    sendShuffleResponse(shufflePartner, publicSample, privateSample);

                    publicView.getValue0().selectToKeep(publicView.getValue1(), behaviour.getSelf(),
                            shufflePartner, content.selfView, content.publicNodes);
                    privateView.getValue0().selectToKeep(privateView.getValue1(), behaviour.getSelf(),
                            shufflePartner, content.selfView, content.privateNodes);
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

                    publicView.getValue0().selectToKeep(publicView.getValue1(), behaviour.getSelf(),
                            shufflePartner, content.selfView, content.publicNodes);
                    privateView.getValue0().selectToKeep(privateView.getValue1(), behaviour.getSelf(),
                            shufflePartner, content.selfView, content.privateNodes);
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
            if(NatType.isOpen(timeout.dest)) {
                publicView.getValue0().timedOut(publicView.getValue1(), timeout.dest);
            } else {
                privateView.getValue0().timedOut(privateView.getValue1(), timeout.dest);
            }
            bootstrapNodes.remove(timeout.dest);
            if(!connected()) {
                trigger(new CroupierDisconnected(overlayId), croupierControlPort);
            }
        }
    };

    //**************************************************************************
    private boolean connected() {
        return !bootstrapNodes.isEmpty() || !publicView.getValue0().isEmpty() || !privateView.getValue0().isEmpty();
    }

    private List<NatAwareAddress> copyCleanSelf(List<NatAwareAddress> addresses) {
        List<NatAwareAddress> result = new ArrayList<>();
        Iterator<NatAwareAddress> it = addresses.iterator();
        while (it.hasNext()) {
            NatAwareAddress node = it.next();
            if (!node.getId().equals(behaviour.getSelf().getId())) {
                result.add(node);
            }
        }
        return result;
    }

    private NatAwareAddress selectPeerToShuffleWith() {
        if (!bootstrapNodes.isEmpty()) {
            return bootstrapNodes.remove(0);
        }
        NatAwareAddress node = null;
        if (!publicView.getValue0().isEmpty()) {
            node = publicView.getValue0().selectPeerToShuffleWith(publicView.getValue1());
        } else if (!privateView.getValue0().isEmpty()) {
            node = privateView.getValue0().selectPeerToShuffleWith(privateView.getValue1());
        }
        return node;
    }

    private void incrementAges() {
        publicView.getValue0().incrementAges();
        privateView.getValue0().incrementAges();
    }

    private void publishSample() {
        CroupierSample cs = new CroupierSample(UUIDIdentifier.randomId(), overlayId,
                publicView.getValue0().publish(), privateView.getValue0().publish());
        LOG.debug("{}publishing public nodes:{}", new Object[]{logPrefix, cs.publicSample});
        LOG.debug("{}publishing private nodes:{}", new Object[]{logPrefix, cs.privateSample});
        trigger(cs, croupierPort);
    }

    public static class CroupierInit extends Init<CroupierComp> {

        public final Identifier overlayId;
        public final NatAwareAddress selfAdr;

        public CroupierInit(Identifier overlayId, KAddress selfAdr) {
            this.overlayId = overlayId;
            this.selfAdr = (NatAwareAddress)selfAdr;
        }
    }

    private void schedulePeriodicShuffle(long period) {
        if (shuffleCycle.getValue0() != null) {
            LOG.warn("{} double starting periodic shuffle", logPrefix);
            return;
        }
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(period, period);
        ShuffleCycle sc = new ShuffleCycle(spt);
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

    public static class ShuffleCycle extends Timeout implements CroupierEvent {
        public final long period;
        public ShuffleCycle(SchedulePeriodicTimeout request) {
            super(request);
            period = request.getPeriod();
        }

        @Override
        public String toString() {
            return "SHUFFLE_CYCLE";
        }

        @Override
        public Identifier getId() {
            return new UUIDIdentifier(getTimeoutId());
        }
    }

    private void scheduleShuffleTimeout(NatAwareAddress dest) {
        if (shuffleTid != null) {
            LOG.warn("{} double starting shuffle timeout", logPrefix);
        }
        ScheduleTimeout spt = new ScheduleTimeout(croupierConfig.shufflePeriod / 2);
        ShuffleTimeout sc = new ShuffleTimeout(spt, dest);
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

    public class ShuffleTimeout extends Timeout implements CroupierEvent {

        public final NatAwareAddress dest;

        public ShuffleTimeout(ScheduleTimeout request, NatAwareAddress dest) {
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
}

/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * KompicsToolbox is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.overlaymngr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatPort;
import se.sics.ktoolbox.cc.heartbeat.event.CCHeartbeat;
import se.sics.ktoolbox.cc.heartbeat.event.CCOverlaySample;
import se.sics.ktoolbox.croupier.CroupierComp;
import se.sics.ktoolbox.croupier.CroupierComp.CroupierInit;
import se.sics.ktoolbox.croupier.CroupierControlPort;
import se.sics.ktoolbox.croupier.CroupierPort;
import se.sics.ktoolbox.croupier.event.CroupierControl;
import se.sics.ktoolbox.croupier.event.CroupierJoin;
import se.sics.ktoolbox.gradient.GradientComp;
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.gradient.temp.RankUpdatePort;
import se.sics.ktoolbox.overlaymngr.events.OMngrCroupier;
import se.sics.ktoolbox.overlaymngr.events.OMngrTGradient;
import se.sics.ktoolbox.overlaymngr.util.ServiceView;
import se.sics.ktoolbox.tgradient.TreeGradientComp;
import se.sics.ktoolbox.util.address.AddressUpdate;
import se.sics.ktoolbox.util.address.AddressUpdatePort;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;
import se.sics.ktoolbox.util.network.nat.NatAwareAddress;
import se.sics.ktoolbox.util.selectors.OverlaySelector;
import se.sics.ktoolbox.util.update.view.OverlayViewUpdate;
import se.sics.ktoolbox.util.update.view.ViewUpdatePort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OverlayMngrComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(OverlayMngrComp.class);
    private String logPrefix = "";

    private final Negative overlayMngr = provides(OverlayMngrPort.class);
    private final Positive timer = requires(Timer.class);
    private final Positive network = requires(Network.class);
    private final Positive addressUpdate = requires(AddressUpdatePort.class);
    private final Positive heartbeat = requires(CCHeartbeatPort.class);

    //internal
    private final Positive internalCStatus = requires(CroupierControlPort.class);
    private final Positive internalBootstrap = requires(CroupierPort.class);

    private final SystemKCWrapper systemConfig;

    private NatAwareAddress self;
    private Pair<Component, Channel[]> globalCroupier;

    //croupier
    //<croupierId, <croupier, internalChannels>
    private final Map<Identifier, Pair<Component, Channel[]>> croupierLayers = new HashMap<>();
    private final Map<Identifier, OMngrCroupier.ConnectRequest> croupierContext = new HashMap<>();

//tgradients (with their, croupierPort and gradient)
    //<tgradientId, <tGradient, internalChannels>>
    private final Map<Identifier, Pair<Component, Channel[]>> gradientLayers = new HashMap<>();
    private final Map<Identifier, Pair<Component, Channel[]>> tgradientLayers = new HashMap<>();
    private final Map<Identifier, OMngrTGradient.ConnectRequest> tgradientContext = new HashMap<>();

//    private final Set<Identifier> pendingJoin = new HashSet<>();
    public OverlayMngrComp(OverlayMngrInit init) {
        systemConfig = new SystemKCWrapper(config());
        logPrefix = "<nid:" + systemConfig.id + "> ";
        LOG.info("{}initiating with seed:{}", logPrefix, systemConfig.seed);

        self = init.self;
        connectGlobalCroupier();

        subscribe(handleStart, control);
        subscribe(handleAddressUpdate, addressUpdate);
        subscribe(handleExternalSample, heartbeat);
        subscribe(handleDisconnect, internalCStatus);
        subscribe(handleConnectCroupier, overlayMngr);
        subscribe(handleConnectTGradient, overlayMngr);
//        subscribe(handleGlobalSample, internalBootstrap);
    }

    private void connectGlobalCroupier() {
        Identifier gcId = OverlayMngrConfig.getGlobalCroupierIntegerId();
        Component gcComp = create(CroupierComp.class, new CroupierInit(gcId, self));
        Channel[] gcChannels = new Channel[5];
        gcChannels[0] = connect(gcComp.getNegative(Network.class), network, new OverlaySelector(gcId, true), Channel.TWO_WAY);
        gcChannels[1] = connect(gcComp.getNegative(Timer.class), timer, Channel.TWO_WAY);
        gcChannels[2] = connect(gcComp.getNegative(AddressUpdatePort.class), addressUpdate, Channel.TWO_WAY);
        gcChannels[3] = connect(gcComp.getPositive(CroupierPort.class), internalBootstrap.getPair(), Channel.TWO_WAY);
        gcChannels[4] = connect(gcComp.getPositive(CroupierControlPort.class), internalCStatus.getPair(), Channel.TWO_WAY);
        globalCroupier = Pair.with(gcComp, gcChannels);
    }
    //****************************CONTROL***************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting global croupier...", logPrefix);
            startGlobalCroupier();
        }
    };

    private void startGlobalCroupier() {
        Identifier gcId = OverlayMngrConfig.getGlobalCroupierIntegerId();
        trigger(new OverlayViewUpdate.Indication(UUIDIdentifier.randomId(), false, new ServiceView()),
                globalCroupier.getValue0().getNegative(ViewUpdatePort.class));
        trigger(new CCOverlaySample.Request(gcId), heartbeat);
        trigger(new CCHeartbeat.Start(gcId), heartbeat);
    }

    @Override
    public Fault.ResolveAction handleFault(Fault fault) {
        LOG.info("{}component:{} fault:{}",
                new Object[]{logPrefix, fault.getSource().getClass(), fault.getCause().getMessage()});
        return Fault.ResolveAction.ESCALATE;
    }

    Handler handleAddressUpdate = new Handler<AddressUpdate.Indication>() {
        @Override
        public void handle(AddressUpdate.Indication update) {
            LOG.info("{}address change from:{} to:{}", new Object[]{logPrefix, self, update.localAddress});
            self = (NatAwareAddress) update.localAddress;
        }
    };

    //**************************************************************************
    Handler handleExternalSample = new Handler<CCOverlaySample.Response>() {

        @Override
        public void handle(CCOverlaySample.Response resp) {
            LOG.info("{}overlay:{} external bootstrap:{}", new Object[]{logPrefix, resp.req.overlayId, resp.overlaySample});
            if (OverlayMngrConfig.isGlobalCroupier(resp.req.overlayId)) {
                trigger(new CroupierJoin(new ArrayList<>(resp.overlaySample)), globalCroupier.getValue0().getPositive(CroupierControlPort.class));
            } else if (croupierLayers.containsKey(resp.req.overlayId)) {
                Component croupierComp = croupierLayers.get(resp.req.overlayId).getValue0();
                trigger(new CroupierJoin(new ArrayList<>(resp.overlaySample)), croupierComp.getPositive(CroupierControlPort.class));
            } else {
                LOG.warn("{}unexpected external sample for overlay:{}", new Object[]{logPrefix, resp.req.overlayId});
            }
        }
    };

    Handler handleDisconnect = new Handler<CroupierControl>() {
        @Override
        public void handle(CroupierControl event) {
            if (OverlayMngrConfig.isGlobalCroupier(event.overlayId)) {
                LOG.warn("{}global croupier disconnected", logPrefix);
                trigger(new CCOverlaySample.Request(event.overlayId), heartbeat);
            } else if (croupierLayers.containsKey(event.overlayId)) {
                LOG.info("{}croupier:{} disconnected", new Object[]{logPrefix, event.overlayId});
                trigger(new CCOverlaySample.Request(event.overlayId), heartbeat);
            } else {
                LOG.warn("{}possible late disconnected from deleted croupier:{}",
                        new Object[]{logPrefix, event.overlayId});
            }
        }
    };

//    Handler handleGlobalSample = new Handler<CroupierSample<ServiceView>>() {
//        @Override
//        public void handle(CroupierSample<ServiceView> sample) {
//            LOG.debug("{}sample public:{} private:{}", new Object[]{logPrefix, sample.publicSample, sample.privateSample});
//            //rebootstrap disconnected layers;
//            LOG.debug("{}services awaiting bootstrap:{}", logPrefix, pendingJoin.size());
//            Iterator<Identifier> it = pendingJoin.iterator();
//            while (it.hasNext()) {
//                Identifier serviceId = it.next();
//                if (!croupierLayers.containsKey(serviceId)) {
//                    LOG.trace("{}leftover croupierPort connect", logPrefix);
//                    it.remove();
//                    //maybe the node left that overlay and killed its CroupierComp
//                    continue;
//                }
//                Component croupierPort = croupierLayers.get(serviceId).getValue0();
//                List<KAddress> bootstrap = new ArrayList<>();
//                for (AdrContainer<KAddress, ServiceView> cc : sample.publicSample.values()) {
//                    LOG.trace("{}target:{} services:{}", new Object[]{logPrefix, cc.getSource().getId(), cc.getContent().runningServices});
//                    if (cc.getContent().runningServices.contains(serviceId)) {
//                        bootstrap.add(cc.getSource());
//
//                    }
//                }
//                if (!bootstrap.isEmpty()) {
//                    trigger(new CroupierJoin(UUIDIdentifier.randomId(), bootstrap), croupierPort.getPositive(CroupierControlPort.class
//                    ));
//                    it.remove();
//                }
//            }
//        }
//    };
//
    //**********************************CROUPIER********************************
    Handler handleConnectCroupier = new Handler<OMngrCroupier.ConnectRequest>() {
        @Override
        public void handle(OMngrCroupier.ConnectRequest req) {
            LOG.info("{}connecting croupier:{}", new Object[]{logPrefix, req.croupierId});

//            if (!OverlayMngrConfig.isGlobalCroupier(req.parentId)) {
//                LOG.error("{}for the moment allow only one layer croupierPort - parent can be only global croupierPort");
//                throw new RuntimeException("for the moment allow only one layer croupierPort - parent can be only global croupierPort");
//            }
            if (croupierLayers.containsKey(req.croupierId) || OverlayMngrConfig.isGlobalCroupier(req.croupierId)) {
                LOG.error("{}double start of croupier", logPrefix);
                throw new RuntimeException("double start of croupier");
            }

            Component croupier = create(CroupierComp.class, new CroupierInit(req.croupierId, self));
            Channel[] croupierChannels = new Channel[6];
            croupierChannels[0] = connect(croupier.getNegative(Network.class), network,
                    new OverlaySelector(req.croupierId, true), Channel.TWO_WAY);
            croupierChannels[1] = connect(croupier.getNegative(Timer.class), timer, Channel.TWO_WAY);
            croupierChannels[2] = connect(croupier.getNegative(AddressUpdatePort.class), addressUpdate, Channel.TWO_WAY);
            croupierChannels[3] = connect(croupier.getNegative(ViewUpdatePort.class), req.viewUpdatePort, Channel.TWO_WAY);
            croupierChannels[4] = connect(croupier.getPositive(CroupierPort.class), req.croupierPort, Channel.TWO_WAY);
            croupierChannels[5] = connect(croupier.getPositive(CroupierControlPort.class), internalCStatus.getPair(), Channel.TWO_WAY);

            croupierLayers.put(req.croupierId, Pair.with(croupier, croupierChannels));
            croupierContext.put(req.croupierId, req);

            trigger(new CCHeartbeat.Start(req.croupierId), heartbeat);
            trigger(new CCOverlaySample.Request(req.croupierId), heartbeat);
            trigger(Start.event, croupier.control());
            answer(req, req.answer());
        }
    };

    //*******************************GRADIENT***********************************
    Handler handleConnectTGradient = new Handler<OMngrTGradient.ConnectRequest>() {
        @Override
        public void handle(OMngrTGradient.ConnectRequest req) {
            LOG.info("{}connecting croupier:{} gradient:{} tgradient:{}",
                    new Object[]{logPrefix, req.croupierId, req.gradientId, req.tgradientId});

//            if (!OverlayMngrConfig.isGlobalCroupier(req.parentId)) {
//                LOG.error("{}for the moment allow only one layer croupier - parent can be only global croupier");
//                throw new RuntimeException("for the moment allow only one layer croupier - parent can be only global croupier");
//            }
            if (croupierLayers.containsKey(req.croupierId) || tgradientLayers.containsKey(req.gradientId) || tgradientLayers.containsKey(req.tgradientId)) {
                LOG.error("{}double start of gradient", logPrefix);
                throw new RuntimeException("double start of gradient");
            }

            //croupier
            Component croupierComp = create(CroupierComp.class, new CroupierInit(req.croupierId, self));
            Channel[] croupierChannels = new Channel[4];
            croupierChannels[0] = connect(croupierComp.getNegative(Network.class), network,
                    new OverlaySelector(req.croupierId, true), Channel.TWO_WAY);
            croupierChannels[1] = connect(croupierComp.getNegative(Timer.class), timer, Channel.TWO_WAY);
            croupierChannels[2] = connect(croupierComp.getNegative(AddressUpdatePort.class), addressUpdate, Channel.TWO_WAY);
            croupierChannels[3] = connect(croupierComp.getPositive(CroupierControlPort.class), internalCStatus.getPair(), Channel.TWO_WAY);
            //viewUpdate, croupier - connected by gradient˜
            //bootstrap port not yet fully defined
            croupierLayers.put(req.croupierId, Pair.with(croupierComp, croupierChannels));

            //gradient
            Component gradientComp = create(GradientComp.class, new GradientComp.GradientInit(req.gradientId, self,
                    req.utilityComparator, req.gradientFilter));
            Channel[] gradientChannels = new Channel[5];
            gradientChannels[0] = connect(gradientComp.getNegative(Network.class), network,
                    new OverlaySelector(req.gradientId, true), Channel.TWO_WAY);
            gradientChannels[1] = connect(gradientComp.getNegative(Timer.class), timer, Channel.TWO_WAY);
            gradientChannels[2] = connect(gradientComp.getNegative(AddressUpdatePort.class), addressUpdate, Channel.TWO_WAY);
            gradientChannels[3] = connect(gradientComp.getPositive(ViewUpdatePort.class), croupierComp.getNegative(ViewUpdatePort.class), Channel.TWO_WAY);
            gradientChannels[4] = connect(gradientComp.getNegative(CroupierPort.class), croupierComp.getPositive(CroupierPort.class), Channel.TWO_WAY);
            //viewUpdate, gradient, rankUpdate connected by tgradinet
            gradientLayers.put(req.gradientId, Pair.with(gradientComp, gradientChannels));

            //tgradient
            Component tgradientComp = create(TreeGradientComp.class, new TreeGradientComp.TreeGradientInit(
                    req.tgradientId, self, req.gradientFilter)); 
            Channel[] tgradientChannels = new Channel[9];
            tgradientChannels[0] = connect(tgradientComp.getNegative(Network.class), network,
                    new OverlaySelector(req.tgradientId, true), Channel.TWO_WAY);
            tgradientChannels[1] = connect(tgradientComp.getNegative(Timer.class), timer, Channel.TWO_WAY);
            tgradientChannels[2] = connect(tgradientComp.getNegative(AddressUpdatePort.class), addressUpdate, Channel.TWO_WAY);
            tgradientChannels[3] = connect(tgradientComp.getNegative(CroupierPort.class), croupierComp.getPositive(CroupierPort.class), Channel.TWO_WAY);
            tgradientChannels[4] = connect(tgradientComp.getNegative(GradientPort.class), gradientComp.getPositive(GradientPort.class), Channel.TWO_WAY);
            tgradientChannels[5] = connect(tgradientComp.getPositive(ViewUpdatePort.class), gradientComp.getNegative(ViewUpdatePort.class), Channel.TWO_WAY);
            tgradientChannels[6] = connect(tgradientComp.getNegative(RankUpdatePort.class), gradientComp.getPositive(RankUpdatePort.class), Channel.TWO_WAY);
            tgradientChannels[7] = connect(tgradientComp.getNegative(ViewUpdatePort.class), req.viewUpdatePort, Channel.TWO_WAY);
            tgradientChannels[8] = connect(tgradientComp.getPositive(GradientPort.class), req.tgradientPort, Channel.TWO_WAY);
            tgradientLayers.put(req.tgradientId, Pair.with(tgradientComp, tgradientChannels));   
            tgradientContext.put(req.getId(), req);
            
            trigger(new CCHeartbeat.Start(req.croupierId), heartbeat);
            trigger(new CCOverlaySample.Request(req.croupierId), heartbeat);
            trigger(Start.event, croupierComp.control());
            trigger(Start.event, gradientComp.control());
            trigger(Start.event, tgradientComp.control());
            answer(req, req.answer());
        }
    };

    public static class OverlayMngrInit extends Init<OverlayMngrComp> {

        public final NatAwareAddress self;
        public final List<NatAwareAddress> bootstrap;

        public OverlayMngrInit(NatAwareAddress self, List<NatAwareAddress> bootstrap) {
            this.self = self;
            this.bootstrap = bootstrap;
        }
    }
}

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

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Ints;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import se.sics.kompics.Kill;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.croupier.CroupierComp;
import se.sics.ktoolbox.croupier.CroupierComp.CroupierInit;
import se.sics.ktoolbox.croupier.CroupierControlPort;
import se.sics.ktoolbox.croupier.CroupierPort;
import se.sics.ktoolbox.croupier.event.CroupierControl;
import se.sics.ktoolbox.croupier.event.CroupierJoin;
import se.sics.ktoolbox.croupier.event.CroupierSample;
import se.sics.ktoolbox.overlaymngr.events.OMngrCroupier;
import se.sics.ktoolbox.overlaymngr.events.OMngrTGradient;
import se.sics.ktoolbox.overlaymngr.events.OverlayMngrEvent;
import se.sics.ktoolbox.overlaymngr.util.ServiceView;
import se.sics.ktoolbox.util.address.AddressUpdate;
import se.sics.ktoolbox.util.address.AddressUpdatePort;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicAddress;
import se.sics.ktoolbox.util.network.nat.NatAwareAddress;
import se.sics.ktoolbox.util.other.AdrContainer;
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

    //internal
    private final Positive internalCStatus = requires(CroupierControlPort.class);
    private final Positive internalBootstrap = requires(CroupierPort.class);

    private final SystemKCWrapper systemConfig;
    private final List<KAddress> bootstrap = new ArrayList<>();

    private boolean globalObserver;
    private NatAwareAddress self;
    private Pair<Component, Channel[]> globalCroupier;
    
    //croupier
    //<croupierId, <croupier, internalChannels>
    private final Map<Identifier, Pair<Component, Channel[]>> croupierLayers = new HashMap<>();
    //<overlayId, <req, externalChannels>>
    private final Map<Identifier, Pair<OMngrCroupier.ConnectRequest, Channel[]>> croupierContext = new HashMap<>();
    
    //tgradients (with their, croupier and gradient)
    //<tgradientId, <tGradient, gradient, internalChannels>>
    private final Map<Identifier, Triplet<Component, Component, Channel[]>> gradientLayers = new HashMap<>();
    //<tgradientId, <req, externalChannels>>
    private final Map<Identifier, Pair<OMngrTGradient, Channel[]>> gradientContext = new HashMap<>();

    //croupiers waiting for bootstraping
    private final Set<Identifier> pendingJoin = new HashSet<>();

    public OverlayMngrComp(OverlayMngrInit init) {
        systemConfig = new SystemKCWrapper(config());
        logPrefix = "<nid:" + systemConfig.id + "> ";
        LOG.info("{}initiating with seed:{}", logPrefix, systemConfig.seed);

        globalObserver = init.observer;
        self = init.self;
        bootstrap.addAll(init.bootstrap);
        connectGlobalCroupier();

        subscribe(handleStart, control);
        subscribe(handleAddressUpdate, addressUpdate);
        subscribe(handleConnectCroupier, overlayMngr);
        subscribe(handleDisconnect, internalCStatus);
        subscribe(handleGlobalSample, internalBootstrap);
    }

    //****************************CONTROL***************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting global croupier with bootstrap:{}", logPrefix, bootstrap);
            trigger(new CroupierJoin(UUIDIdentifier.randomId(), bootstrap),
                    globalCroupier.getValue0().getPositive(CroupierControlPort.class));
            if (globalObserver) {
                trigger(new OverlayViewUpdate.Indication(UUIDIdentifier.randomId(), true, null),
                        globalCroupier.getValue0().getNegative(ViewUpdatePort.class));
            } else {
                trigger(new OverlayViewUpdate.Indication(UUIDIdentifier.randomId(), false, new ServiceView(new ArrayList<Identifier>())),
                        globalCroupier.getValue0().getNegative(ViewUpdatePort.class));
            }
        }
    };

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
    private void connectGlobalCroupier() {
        Identifier gcId = OverlayMngrConfig.getGlobalCroupierIntegerId();
        Component gcComp = create(CroupierComp.class, new CroupierInit(self, gcId));
        Channel[] gcChannels = new Channel[5];
        gcChannels[0] = connect(gcComp.getNegative(Network.class), network, new OverlaySelector(gcId, true), Channel.TWO_WAY);
        gcChannels[1] = connect(gcComp.getNegative(Timer.class), timer, Channel.TWO_WAY);
        gcChannels[2] = connect(gcComp.getNegative(AddressUpdatePort.class), addressUpdate, Channel.TWO_WAY);
        gcChannels[3] = connect(internalBootstrap.getPair(), gcComp.getPositive(CroupierPort.class), Channel.TWO_WAY);
        gcChannels[4] = connect(internalCStatus.getPair(), gcComp.getPositive(CroupierControlPort.class), Channel.TWO_WAY);
        globalCroupier = Pair.with(gcComp, gcChannels);
    }

    Handler handleDisconnect = new Handler<CroupierControl>() {
        @Override
        public void handle(CroupierControl event) {
            if (OverlayMngrConfig.isGlobalCroupier(event.overlayId)) {
                LOG.error("{}global disconnect, shutting down", logPrefix);
                throw new RuntimeException("global disconnect");
            } else if (croupierLayers.containsKey(event.overlayId)) {
                LOG.info("{}croupier:{} disconnected", new Object[]{logPrefix, event.overlayId});
                pendingJoin.add(event.overlayId);
            } else {
                LOG.warn("{}possible late disconnected from deleted croupier:{}",
                        new Object[]{logPrefix, event.overlayId});
            }
        }
    };

    Handler handleGlobalSample = new Handler<CroupierSample<ServiceView>>() {
        @Override
        public void handle(CroupierSample<ServiceView> sample) {
            LOG.debug("{}sample public:{} private:{}", new Object[]{logPrefix, sample.publicSample, sample.privateSample});
            //rebootstrap disconnected layers;
            LOG.debug("{}services awaiting bootstrap:{}", logPrefix, pendingJoin.size());
            Iterator<Identifier> it = pendingJoin.iterator();
            while (it.hasNext()) {
                Identifier serviceId = it.next();
                if (!croupierLayers.containsKey(serviceId)) {
                    LOG.trace("{}leftover croupier connect", logPrefix);
                    it.remove();
                    //maybe the node left that overlay and killed its CroupierComp
                    continue;
                }
                Component croupier = croupierLayers.get(serviceId).getValue0();
                List<KAddress> bootstrap = new ArrayList<>();
                for (AdrContainer<KAddress, ServiceView> cc : sample.publicSample.values()) {
                    LOG.trace("{}target:{} services:{}", new Object[]{logPrefix, cc.getSource().getId(), cc.getContent().runningServices});
                    if (cc.getContent().runningServices.contains(serviceId)) {
                        bootstrap.add(cc.getSource());

                    }
                }
                if (!bootstrap.isEmpty()) {
                    trigger(new CroupierJoin(UUIDIdentifier.randomId(), bootstrap), croupier.getPositive(CroupierControlPort.class
                    ));
                    it.remove();
                }
            }
        }
    };

    //**********************************CROUPIER********************************
    Handler handleConnectCroupier = new Handler<OMngrCroupier.ConnectRequest>() {
        @Override
        public void handle(OMngrCroupier.ConnectRequest req) {
            LOG.info("{}connecting croupier:{}", new Object[]{logPrefix, req.croupierId});

            if (!OverlayMngrConfig.isGlobalCroupier(req.parentId)) {
                LOG.error("{}for the moment allow only one layer croupier - parent can be only global croupier");
                throw new RuntimeException("for the moment allow only one layer croupier - parent can be only global croupier");
            }
            if (croupierLayers.containsKey(req.croupierId)) {
                LOG.error("{}double start of croupier", logPrefix);
                throw new RuntimeException("double start of croupier");
            }

            Component comp = create(CroupierComp.class, new CroupierInit(self, req.croupierId));
            Channel[] channels = new Channel[6];
            channels[0] = connect(comp.getNegative(Network.class), network,
                    new OverlaySelector(req.croupierId, true), Channel.TWO_WAY);
            channels[1] = connect(comp.getNegative(Timer.class), timer, Channel.TWO_WAY);
            channels[2] = connect(comp.getNegative(AddressUpdatePort.class), addressUpdate, Channel.TWO_WAY);
            channels[3] = connect(internalCStatus.getPair(), comp.getPositive(CroupierControlPort.class), Channel.TWO_WAY);
            channels[4] = connect(comp.getNegative(ViewUpdatePort.class), req.viewUpdate, Channel.TWO_WAY);
            channels[5] = connect(comp.getPositive(CroupierPort.class), req.croupier, Channel.TWO_WAY);

            croupierLayers.put(req.croupierId, Pair.with(comp, channels));
            pendingJoin.add(req.croupierId);
            connectContexts.put(req.croupierId, req);

            trigger(Start.event, comp.control());
            if (!globalObserver && !req.observer) {
                //tell global croupier about new overlayservice
                Set<Identifier> services = new HashSet<>(croupierLayers.keySet());
                trigger(new OverlayViewUpdate.Indication(UUIDIdentifier.randomId(), globalObserver, new ServiceView(services)), 
                        globalCroupier.getValue0().getNegative(ViewUpdatePort.class));
            }
            answer(req, req.answer());
        }
    };

    Handler handleDisconnectCroupier = new Handler<OMngrCroupier.Disconnect>() {
        @Override
        public void handle(OMngrCroupier.Disconnect event) {
            OMngrCroupier.ConnectRequest context = (OMngrCroupier.ConnectRequest) connectContexts.remove(event.croupierId);
            Pair<Component, Channel[]> croupier = croupierLayers.remove(event.croupierId);
            if (context == null && croupier == null) {
                LOG.warn("{}possible multiple croupier disconnects");
                return;
            }
            LOG.info("{}disconnecting croupier:{}", new Object[]{logPrefix, event.croupierId});
            for(Channel c : croupier.getValue1()) {
                disconnect(c);
            }
            trigger(Kill.event, croupier.getValue0().control());
        }
    };

    //*******************************GRADIENT***********************************
    private Component connectGradient(byte[] gradientId, Component croupier, Comparator utilityComparator, GradientFilter gradientFilter) {
        int overlayId = Ints.fromByteArray(gradientId);
        long gradientSeed = config.system.seed + overlayId;

        GradientKCWrapper gradientConfig = new GradientKCWrapper(config.configCore, gradientSeed, overlayId);
        GradientInit gradientInit = new GradientInit(gradientConfig, self, utilityComparator, gradientFilter);
        Component gradient = create(GradientComp.class, gradientInit);
        connect(gradient.getNegative(Timer.class), timer);
        connect(gradient.getNegative(Network.class), network);
        connect(gradient.getNegative(SelfAddressUpdatePort.class), addressUpdate);
        connect(gradient.getPositive(SelfViewUpdatePort.class), croupier.getNegative(SelfViewUpdatePort.class));
        connect(gradient.getNegative(CroupierPort.class), croupier.getPositive(CroupierPort.class));

        gradientLayers.put(ByteBuffer.wrap(gradientId), gradient);
        return gradient;
    }

    private Component connectTGradient(byte[] tgradientId, Component gradient, GradientFilter gradientFilter) {
        int overlayId = Ints.fromByteArray(tgradientId);
        long tgradientSeed = config.system.seed + overlayId;

        GradientKCWrapper gradientConfig = new GradientKCWrapper(config.configCore, tgradientSeed, overlayId);
        TGradientKCWrapper tgradientConfig = new TGradientKCWrapper(config.configCore);
        TreeGradientInit tgradientInit = new TreeGradientInit(gradientConfig, tgradientConfig, self, gradientFilter);
        Component tgradient = create(TreeGradientComp.class, tgradientInit);
        connect(tgradient.getNegative(Timer.class), timer);
        connect(tgradient.getNegative(Network.class), network);
        connect(tgradient.getNegative(SelfAddressUpdatePort.class), addressUpdate);
        connect(tgradient.getPositive(SelfViewUpdatePort.class), gradient.getNegative(SelfViewUpdatePort.class));
        connect(tgradient.getNegative(GradientPort.class), gradient.getPositive(GradientPort.class));

        gradientLayers.put(ByteBuffer.wrap(tgradientId), tgradient);
        return tgradient;
    }
    Handler handleConnectGradient = new Handler<OMngrTGradient.ConnectRequest>() {
        @Override
        public void handle(OMngrTGradient.ConnectRequest req) {
            LOG.info("{}connecting croupier:{} gradient:{} tgradient",
                    new Object[]{logPrefix, req.croupierId, req.gradientId, req.tgradientId});

            if (!OverlayMngrConfig.isGlobalCroupier(req.parentId)) {
                LOG.error("{}for the moment allow only one layer croupier - parent can be only global croupier");
                throw new RuntimeException("for the moment allow only one layer croupier - parent can be only global croupier");
            }
            if (croupierLayers.containsKey(req.croupierId) || gradientLayers.containsKey(req.gradientId) || gradientLayers.containsKey(req.tgradientId)) {
                LOG.error("{}double start of gradient", logPrefix);
                throw new RuntimeException("double start of gradient");
            }
            
            Component comp = create(CroupierComp.class, new CroupierInit(self, req.croupierId));
            Channel[] channels = new Channel[6];
            channels[0] = connect(comp.getNegative(Network.class), network,
                    new OverlaySelector(req.croupierId, true), Channel.TWO_WAY);
            channels[1] = connect(comp.getNegative(Timer.class), timer, Channel.TWO_WAY);
            channels[2] = connect(comp.getNegative(AddressUpdatePort.class), addressUpdate, Channel.TWO_WAY);
            channels[3] = connect(internalCStatus.getPair(), comp.getPositive(CroupierControlPort.class), Channel.TWO_WAY);
            channels[4] = connect(comp.getNegative(ViewUpdatePort.class), req.viewUpdate, Channel.TWO_WAY);
            channels[5] = connect(comp.getPositive(CroupierPort.class), req.croupier, Channel.TWO_WAY);

            croupierLayers.put(req.croupierId, Pair.with(comp, channels));
            pendingJoin.add(req.croupierId);
            connectContexts.put(req.croupierId, req);


            Component croupier = connectCroupier(req.croupierId);
            Component gradient = connectGradient(req.gradientId, croupier, req.utilityComparator, req.gradientFilter);
            Component tgradient = connectTGradient(req.tgradientId, gradient, req.gradientFilter);
            connect(tgradient.getNegative(SelfViewUpdatePort.class), req.viewUpdate);
            connect(tgradient.getPositive(GradientPort.class), req.tgradient);

            trigger(Start.event, croupier.control());
            trigger(Start.event, gradient.control());
            trigger(Start.event, tgradient.control());
            //tell global croupier about new overlayservice - no gradient observers yet
            trigger(CroupierUpdate.update(new ServiceView(getServices())), globalCroupier.getNegative(SelfViewUpdatePort.class));
            connectContexts.put(ByteBuffer.wrap(req.croupierId), req);
        }
    };

    Handler handleDisconnectTGradient = new Handler<OMngrCroupier.Disconnect>() {
        @Override
        public void handle(OMngrCroupier.Disconnect event) {
            OMngrTGradient.ConnectRequest context = (OMngrTGradient.ConnectRequest) connectContexts.remove(event.croupierId);
            Pair<Component, Handler> croupier = croupierLayers.remove(event.croupierId);
            if (context == null && croupier == null) {
                LOG.warn("{}possible multiple croupier disconnects");
                return;
            }
            Component gradient = gradientLayers.remove(context.gradientId);
            Component tgradient = gradientLayers.remove(context.tgradientId);
            if (context == null || croupier == null || gradient == null || tgradient == null) {
                LOG.error("{}inconsitency logic error", logPrefix);
                throw new RuntimeException("inconsitency logic error");
            }
            LOG.info("{}disconnecting croupier:{} gradient:{}, tgradient:{}", new Object[]{logPrefix,
                BaseEncoding.base16().encode(context.croupierId), BaseEncoding.base16().encode(context.gradientId),
                BaseEncoding.base16().encode(context.tgradientId)});
            //croupier
            disconnect(croupier.getValue0().getNegative(Timer.class), timer);
            disconnect(croupier.getValue0().getNegative(Network.class), network);
            disconnect(croupier.getValue0().getNegative(SelfAddressUpdatePort.class), addressUpdate);
            unsubscribe(croupier.getValue1(), croupier.getValue0().getPositive(CroupierControlPort.class));

            //gradient
            disconnect(gradient.getNegative(Timer.class), timer);
            disconnect(gradient.getNegative(Network.class), network);
            disconnect(gradient.getNegative(SelfAddressUpdatePort.class), addressUpdate);
            disconnect(gradient.getPositive(SelfViewUpdatePort.class), croupier.getValue0().getNegative(SelfViewUpdatePort.class));
            disconnect(gradient.getNegative(CroupierPort.class), croupier.getValue0().getPositive(CroupierPort.class));

            //tgradient
            disconnect(tgradient.getNegative(Timer.class), timer);
            disconnect(tgradient.getNegative(Network.class), network);
            disconnect(tgradient.getNegative(SelfAddressUpdatePort.class), addressUpdate);
            disconnect(tgradient.getPositive(SelfViewUpdatePort.class), gradient.getNegative(SelfViewUpdatePort.class));
            disconnect(tgradient.getNegative(GradientPort.class), gradient.getPositive(GradientPort.class));
            disconnect(tgradient.getNegative(SelfViewUpdatePort.class), context.viewUpdate);
            disconnect(tgradient.getPositive(GradientPort.class), context.tgradient);

            trigger(Kill.event, croupier.getValue0().control());
            trigger(Kill.event, gradient.control());
            trigger(Kill.event, tgradient.control());
        }
    };

    public static class OverlayMngrInit extends Init<OverlayMngrComp> {

        public final boolean observer;
        public final NatAwareAddress self;
        public final List<BasicAddress> bootstrap;

        public OverlayMngrInit(boolean observer, NatAwareAddress self, List<BasicAddress> bootstrap) {
            this.observer = observer;
            this.self = self;
            this.bootstrap = bootstrap;
        }
    }
}

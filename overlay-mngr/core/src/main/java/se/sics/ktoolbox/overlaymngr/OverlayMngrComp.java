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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import se.sics.ktoolbox.overlaymngr.events.OMngrCroupier;
import se.sics.ktoolbox.overlaymngr.events.OMngrTGradient;
import se.sics.ktoolbox.overlaymngr.events.OverlayMngrEvent;
import se.sics.ktoolbox.overlaymngr.util.ServiceView;
import se.sics.p2ptoolbox.croupier.CroupierComp;
import se.sics.p2ptoolbox.croupier.CroupierComp.CroupierInit;
import se.sics.p2ptoolbox.croupier.CroupierControlPort;
import se.sics.p2ptoolbox.croupier.CroupierPort;
import se.sics.p2ptoolbox.croupier.msg.CroupierDisconnected;
import se.sics.p2ptoolbox.croupier.msg.CroupierJoin;
import se.sics.p2ptoolbox.croupier.msg.CroupierSample;
import se.sics.p2ptoolbox.croupier.msg.CroupierUpdate;
import se.sics.p2ptoolbox.gradient.GradientComp;
import se.sics.p2ptoolbox.gradient.GradientComp.GradientInit;
import se.sics.p2ptoolbox.gradient.GradientFilter;
import se.sics.p2ptoolbox.gradient.GradientKCWrapper;
import se.sics.p2ptoolbox.gradient.GradientPort;
import se.sics.p2ptoolbox.tgradient.TGradientKCWrapper;
import se.sics.p2ptoolbox.tgradient.TreeGradientComp;
import se.sics.p2ptoolbox.tgradient.TreeGradientComp.TreeGradientInit;
import se.sics.p2ptoolbox.util.Container;
import se.sics.p2ptoolbox.util.config.KConfigCore;
import se.sics.p2ptoolbox.util.filters.IntegerOverlayFilter;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.update.SelfAddressUpdate;
import se.sics.p2ptoolbox.util.update.SelfAddressUpdatePort;
import se.sics.p2ptoolbox.util.update.SelfViewUpdatePort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OverlayMngrComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(OverlayMngrComp.class);
    private String logPrefix = "";

    private final Negative<OverlayMngrPort> overlayMngr = provides(OverlayMngrPort.class);
    private final Positive<Timer> timer = requires(Timer.class);
    private final Positive<Network> network = requires(Network.class);
    private final Positive<SelfAddressUpdatePort> addressUpdate = requires(SelfAddressUpdatePort.class);

    private final OverlayMngrKCWrapper config;
    private DecoratedAddress self;

    private final boolean globalObserver;
    private Component globalCroupier;
    //all croupiers
    private final Map<ByteBuffer, Pair<Component, Handler>> croupierLayers = new HashMap<>();
    //other components - gradient, tgradient
    private final Map<ByteBuffer, Component> components = new HashMap<>();
    //connect requests - croupier, gradient, tgradient
    private final Map<ByteBuffer, OverlayMngrEvent> connectContexts = new HashMap<>();
    //croupiers waiting for bootstraping
    private final Set<ByteBuffer> pendingJoin = new HashSet<>();

    public OverlayMngrComp(OverlayMngrInit init) {
        config = init.config;
        self = init.self;
        globalObserver = init.globalObserver;
        logPrefix = "<nid:" + config.system.id + "> ";
        LOG.info("{}initiating with seed:{}", logPrefix, config.system.seed);

        subscribe(handleStart, control);
        subscribe(handleSelfAddressUpdate, addressUpdate);
        subscribe(handleConnectCroupier, overlayMngr);

        connectGlobalCroupier();
    }

    //****************************CONTROL***************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting global croupier with bootstrap:{}", logPrefix, config.bootstrap);
            trigger(new CroupierJoin(config.bootstrap), globalCroupier.getPositive(CroupierControlPort.class));
            if (globalObserver) {
                trigger(CroupierUpdate.observer(),
                        globalCroupier.getNegative(SelfViewUpdatePort.class));
            } else {
                trigger(CroupierUpdate.update(new ServiceView(new ArrayList<ByteBuffer>())),
                        globalCroupier.getNegative(SelfViewUpdatePort.class));
            }
        }
    };

    @Override
    public Fault.ResolveAction handleFault(Fault fault) {
        LOG.info("{}component:{} fault:{}",
                new Object[]{logPrefix, fault.getSource().getClass(), fault.getCause().getMessage()});
        return Fault.ResolveAction.ESCALATE;
    }

    Handler handleSelfAddressUpdate = new Handler<SelfAddressUpdate>() {
        @Override
        public void handle(SelfAddressUpdate update) {
            LOG.info("{}address change from:{} to:{}", new Object[]{logPrefix, self, update.self});
            self = update.self;
        }
    };

    //**************************************************************************
    private void connectGlobalCroupier() {
        int gcId = OverlayMngrConfig.getGlobalCroupierIntegerId();
        globalCroupier = create(CroupierComp.class, new CroupierInit(config.configCore, self, gcId, config.system.seed));
        connect(globalCroupier.getNegative(Network.class), network, new IntegerOverlayFilter(gcId));
        connect(globalCroupier.getNegative(Timer.class), timer);
        connect(globalCroupier.getNegative(SelfAddressUpdatePort.class), addressUpdate);
        subscribe(handleGlobalDisconnect, globalCroupier.getPositive(CroupierControlPort.class));
        subscribe(handleGlobalSample, globalCroupier.getPositive(CroupierPort.class));
    }

    Handler handleGlobalDisconnect = new Handler<CroupierDisconnected>() {
        @Override
        public void handle(CroupierDisconnected event) {
            LOG.error("{}global disconnect, shutting down", logPrefix);
            trigger(new CroupierJoin(config.bootstrap), globalCroupier.getPositive(CroupierControlPort.class));
//            throw new RuntimeException("global disconnect");
        }
    };

    Handler handleGlobalSample = new Handler<CroupierSample<ServiceView>>() {
        @Override
        public void handle(CroupierSample<ServiceView> sample) {
            LOG.debug("{}sample public:{} private:{}", new Object[]{logPrefix, sample.publicSample, sample.privateSample});
            //rebootstrap disconnected layers;
            LOG.debug("{}services awaiting bootstrap:{}", logPrefix, pendingJoin.size());
            Iterator<ByteBuffer> it = pendingJoin.iterator();
            while (it.hasNext()) {
                ByteBuffer serviceId = it.next();
                if (!croupierLayers.containsKey(serviceId)) {
                    LOG.trace("{}leftover croupier connect", logPrefix);
                    it.remove();
                    //maybe the node left that overlay and killed its CroupierComp
                    continue;
                }
                Component croupier = croupierLayers.get(serviceId).getValue0();
                List<DecoratedAddress> bootstrap = new ArrayList<>();
                for (Container<DecoratedAddress, ServiceView> cc : sample.publicSample) {
                    LOG.trace("{}target:{} services:{}", new Object[]{logPrefix, cc.getSource().getBase(), cc.getContent().runningServices});
                    if (cc.getContent().runningServices.contains(serviceId)) {
                        bootstrap.add(cc.getSource());
                    }
                }
                if (!bootstrap.isEmpty()) {
                    trigger(new CroupierJoin(bootstrap), croupier.getPositive(CroupierControlPort.class));
                    it.remove();
                }
            }
        }
    };

    //**********************************CROUPIER********************************
    private Component connectCroupier(byte[] croupierId) {
        int overlayId = Ints.fromByteArray(croupierId);
        long croupierSeed = config.system.seed + overlayId;

        Component croupier = create(CroupierComp.class, new CroupierInit(config.configCore, self, overlayId, croupierSeed));
        connect(croupier.getNegative(Timer.class), timer);
        connect(croupier.getNegative(Network.class), network, new IntegerOverlayFilter(overlayId));
        connect(croupier.getNegative(SelfAddressUpdatePort.class), addressUpdate);

        Handler disconnectHandler = new Handler<CroupierDisconnected>() {
            @Override
            public void handle(CroupierDisconnected event) {
                ByteBuffer overlayId = ByteBuffer.wrap(Ints.toByteArray(event.overlayId));
                if (!croupierLayers.containsKey(overlayId)) {
                    LOG.warn("{}possible late disconnected from deleted croupier:{}",
                            new Object[]{logPrefix, BaseEncoding.base16().encode(overlayId.array())});
                    return;
                }
                LOG.info("{}croupier:{} disconnected",
                        new Object[]{logPrefix, BaseEncoding.base16().encode(overlayId.array())});
                pendingJoin.add(overlayId);
                //TODO Alex possibly speed up with caracal here
            }
        };

        croupierLayers.put(ByteBuffer.wrap(croupierId), Pair.with(croupier, disconnectHandler));
        pendingJoin.add(ByteBuffer.wrap(croupierId));
        subscribe(disconnectHandler, croupier.getPositive(CroupierControlPort.class));
        return croupier;
    }

    Handler handleConnectCroupier = new Handler<OMngrCroupier.ConnectRequest>() {
        @Override
        public void handle(OMngrCroupier.ConnectRequest req) {
            LOG.info("{}connecting croupier:{}", new Object[]{logPrefix, BaseEncoding.base16().encode(req.croupierId)});

            if (!OverlayMngrConfig.isGlobalCroupier(req.parentId)) {
                LOG.error("{}for the moment allow only one layer croupier - parent can be only global croupier");
                throw new RuntimeException("for the moment allow only one layer croupier - parent can be only global croupier");
            }
            if (req.croupierId.length != 4) {
                LOG.error("{}for the moment allow only int overlayId");
                throw new RuntimeException("{}for the moment allow only int overlayId");
            }
            if (croupierLayers.containsKey(req.croupierId)) {
                LOG.error("{}double start of croupier", logPrefix);
                throw new RuntimeException("double start of croupier");
            }

            Component croupier = connectCroupier(req.croupierId);
            connect(croupier.getNegative(SelfViewUpdatePort.class), req.viewUpdate);
            connect(croupier.getPositive(CroupierPort.class), req.croupier);

            connectContexts.put(ByteBuffer.wrap(req.croupierId), req);

            trigger(Start.event, croupier.control());
            if (!globalObserver && !req.observer) {
                //tell global croupier about new overlayservice
                trigger(CroupierUpdate.update(new ServiceView(getServices())), globalCroupier.getNegative(SelfViewUpdatePort.class));
            }
            answer(req, req.answer());
        }
    };

    Handler handleDisconnectCroupier = new Handler<OMngrCroupier.Disconnect>() {
        @Override
        public void handle(OMngrCroupier.Disconnect event) {
            OMngrCroupier.ConnectRequest context = (OMngrCroupier.ConnectRequest) connectContexts.remove(event.croupierId);
            Pair<Component, Handler> croupier = croupierLayers.remove(event.croupierId);
            if (context == null && croupier == null) {
                LOG.warn("{}possible multiple croupier disconnects");
                return;
            }
            if (context == null || croupier == null) {
                LOG.error("{}inconsitency logic error", logPrefix);
                throw new RuntimeException("inconsitency logic error");
            }
            LOG.info("{}disconnecting croupier:{}", new Object[]{logPrefix, BaseEncoding.base16().encode(event.croupierId)});
            disconnect(croupier.getValue0().getNegative(Timer.class), timer);
            disconnect(croupier.getValue0().getNegative(Network.class), network);
            disconnect(croupier.getValue0().getNegative(SelfAddressUpdatePort.class), addressUpdate);
            disconnect(croupier.getValue0().getNegative(SelfViewUpdatePort.class), context.viewUpdate);
            disconnect(croupier.getValue0().getPositive(CroupierPort.class), context.croupier);
            unsubscribe(croupier.getValue1(), croupier.getValue0().getPositive(CroupierControlPort.class));
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

        components.put(ByteBuffer.wrap(gradientId), gradient);
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

        components.put(ByteBuffer.wrap(tgradientId), tgradient);
        return tgradient;
    }
    Handler handleConnectGradient = new Handler<OMngrTGradient.ConnectRequest>() {
        @Override
        public void handle(OMngrTGradient.ConnectRequest req) {
            LOG.info("{}connecting croupier:{} gradient:{} tgradient",
                    new Object[]{logPrefix, BaseEncoding.base16().encode(req.croupierId),
                        BaseEncoding.base16().encode(req.gradientId), BaseEncoding.base16().encode(req.tgradientId)});

            if (!OverlayMngrConfig.isGlobalCroupier(req.parentId)) {
                LOG.error("{}for the moment allow only one layer croupier - parent can be only global croupier");
                throw new RuntimeException("for the moment allow only one layer croupier - parent can be only global croupier");
            }
            if (req.croupierId.length != 4 || req.gradientId.length != 4 || req.tgradientId.length != 4) {
                LOG.error("{}for the moment allow only int overlayId");
                throw new RuntimeException("{}for the moment allow only int overlayId");
            }
            if (croupierLayers.containsKey(req.croupierId) || components.containsKey(req.gradientId) || components.containsKey(req.tgradientId)) {
                LOG.error("{}double start of gradient", logPrefix);
                throw new RuntimeException("double start of gradient");
            }

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
            Component gradient = components.remove(context.gradientId);
            Component tgradient = components.remove(context.tgradientId);
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

    private List<ByteBuffer> getServices() {
        List<ByteBuffer> services = new ArrayList<>();
        for (ByteBuffer service : croupierLayers.keySet()) {
            services.add(service);
        }
        return services;
    }

    public static class OverlayMngrInit extends Init<OverlayMngrComp> {

        public final OverlayMngrKCWrapper config;
        public final DecoratedAddress self;
        public final boolean globalObserver;

        public OverlayMngrInit(KConfigCore configCore, DecoratedAddress self, boolean globalObserver) {
            this.config = new OverlayMngrKCWrapper(configCore);
            this.self = self;
            this.globalObserver = globalObserver;
        }
    }
}

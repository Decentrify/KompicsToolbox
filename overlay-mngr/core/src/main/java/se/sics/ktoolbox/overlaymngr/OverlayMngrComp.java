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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatPort;
import se.sics.ktoolbox.cc.heartbeat.msg.CCHeartbeat;
import se.sics.ktoolbox.cc.heartbeat.msg.CCOverlaySample;
import se.sics.ktoolbox.overlaymngr.event.Croupier;
import se.sics.ktoolbox.overlaymngr.util.OverlayMngrHelper;
import se.sics.p2ptoolbox.croupier.CroupierComp;
import se.sics.p2ptoolbox.croupier.CroupierComp.CroupierInit;
import se.sics.p2ptoolbox.croupier.CroupierConfig;
import se.sics.p2ptoolbox.croupier.CroupierControlPort;
import se.sics.p2ptoolbox.croupier.CroupierPort;
import se.sics.p2ptoolbox.croupier.msg.CroupierDisconnected;
import se.sics.p2ptoolbox.croupier.msg.CroupierJoin;
import se.sics.p2ptoolbox.croupier.msg.CroupierSample;
import se.sics.p2ptoolbox.util.Container;
import se.sics.p2ptoolbox.util.config.SystemConfig;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.update.SelfAddressUpdate;
import se.sics.p2ptoolbox.util.update.SelfAddressUpdatePort;
import se.sics.p2ptoolbox.util.update.SelfViewUpdatePort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OverlayMngrComp extends ComponentDefinition {

    private final static Logger LOG = LoggerFactory.getLogger(OverlayMngrComp.class);
    private String logPrefix;

    private final Negative overlayMngr = provides(OverlayMngrPort.class);
    private final Positive timer = requires(Timer.class);
    private final Positive network = requires(Network.class);
    private final Positive addressUpdate = requires(SelfAddressUpdatePort.class);
    private final Positive heartbeat = requires(CCHeartbeatPort.class);
    private final Positive globalServiceView = requires(SelfViewUpdatePort.class);

    private DecoratedAddress selfAddress;
    private final SystemConfig systemConfig;
    private final OverlayMngrConfig oMngrConfig;

    private List<DecoratedAddress> bootstrapSample;
    private Component globalCroupier;
    private Map<Byte, Component> croupierOverlays;
    private Map<Byte, Pair<Component, Byte>> gradientOverlays;
    private Map<Byte, Pair<Component, Byte>> tgradientOverlays;
    private Map<UUID, StartOverlay> pendingOverlays;

    private UUID internalStateCheck = null;

    public OverlayMngrComp(OverlayMngrInit init) {
        this.systemConfig = init.systemConfig;
        this.selfAddress = systemConfig.self;
        this.logPrefix = selfAddress.getBase() + " ";
        LOG.info("{}initiating...", logPrefix);

        this.oMngrConfig = new OverlayMngrConfig();
        this.croupierOverlays = new HashMap<Byte, Component>();
        this.gradientOverlays = new HashMap<Byte, Pair<Component, Byte>>();
        this.tgradientOverlays = new HashMap<Byte, Pair<Component, Byte>>();
        this.pendingOverlays = new HashMap<UUID, StartOverlay>();

        connectGlobalCroupier();

        subscribe(handleStart, control);
        subscribe(handleInternalStateCheck, timer);
        subscribe(handleSelfAddressUpdate, addressUpdate);
        subscribe(handleBootstrap, heartbeat);
//        subscribe(handleCroupierStart, overlayMngr);
    }

    //********************************CONTROL***********************************
    private final Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            scheduleInternalStateCheck();
        }
    };

    @Override
    public void tearDown() {
        cancelInternalStateCheck();
    }

    @Override
    public final Fault.ResolveAction handleFault(Fault fault) {
        LOG.error("{}fault:{} from component:{}",
                new Object[]{logPrefix, fault.getCause().getMessage(), fault.getSource().getClass()});
        return Fault.ResolveAction.ESCALATE;
    }

    private final Handler handleSelfAddressUpdate = new Handler<SelfAddressUpdate>() {
        @Override
        public void handle(SelfAddressUpdate update) {
            LOG.info("{}update self address from:{} to:{}",
                    new Object[]{logPrefix, selfAddress, update.self});
            selfAddress = update.self;
        }
    };

    private final Handler handleInternalStateCheck = new Handler<InternalStateCheck>() {
        @Override
        public void handle(InternalStateCheck event) {
            LOG.info("{}croupier:{} gradient:{} tgradient:{} pending:{}",
                    new Object[]{croupierOverlays.size(), gradientOverlays.size(),
                        tgradientOverlays.size(), pendingOverlays.size()});
        }
    };

    //**************************GLOBAL_CROUPIER*********************************
    private void connectGlobalCroupier() {
        globalCroupier = create(CroupierComp.class, new CroupierInit(systemConfig,
                new CroupierConfig(systemConfig.config), OverlayMngrHelper.getCroupierIntOverlayId(OverlayMngrHelper.getGlobalCroupierId())));
        connect(globalCroupier.getNegative(Timer.class), timer);
        connect(globalCroupier.getNegative(Network.class), network);
        connect(globalCroupier.getNegative(SelfViewUpdatePort.class), globalServiceView);
        connect(globalCroupier.getNegative(SelfAddressUpdatePort.class), addressUpdate);

        subscribe(handleGlobalSample, globalCroupier.getNegative(CroupierPort.class));
        subscribe(handleGlobalDisconnect, globalCroupier.getNegative(CroupierControlPort.class));
    }

    private void startGlobalCroupier(boolean started) {
        if (!started) {
            trigger(Start.event, globalCroupier.control());
        }
        byte[] overlayId = OverlayMngrHelper.getCroupierOverlayId(OverlayMngrHelper.getGlobalCroupierId());
        trigger(new CCOverlaySample.Request(overlayId), heartbeat);
    }

    private final Handler handleGlobalSample = new Handler<CroupierSample>() {
        @Override
        public void handle(CroupierSample sample) {
            LOG.trace("");
            Iterator<Container<DecoratedAddress, Object>> it = sample.publicSample.iterator();
            bootstrapSample = new ArrayList<>();
            while (it.hasNext()) {
                bootstrapSample.add(it.next().getSource());
            }
        }
    };

    private final Handler handleGlobalDisconnect = new Handler<CroupierDisconnected>() {
        @Override
        public void handle(CroupierDisconnected event) {
            LOG.warn("{}global croupier disconnected", logPrefix);
            byte[] overlayId = OverlayMngrHelper.getCroupierOverlayId(OverlayMngrHelper.getGlobalCroupierId());
            trigger(new CCOverlaySample.Request(overlayId), heartbeat);
        }
    };
    //***************************INTERNAL_BEHAVIOUR*****************************
    private final Handler handleBootstrap = new Handler<CCOverlaySample.Response>() {
        @Override
        public void handle(CCOverlaySample.Response resp) {
            LOG.debug("{}received croupier:<{},{}> bootstrap:{}",
                    new Object[]{logPrefix, resp.overlayId[2], resp.overlayId[3], resp.overlaySample});
            Component croupier = croupierOverlays.get(resp.overlayId[3]);
            if (croupier == null) {
                LOG.warn("{}croupier:<{},{}> does not exist, might have been killed");
                return;
            }
            trigger(new CroupierJoin(new ArrayList<>(resp.overlaySample)), croupier.getNegative(CroupierControlPort.class));
        }
    };
//    private final Handler handleCroupierStart = new Handler<Croupier.Start>() {
//        @Override
//        public void handle(Croupier.Start req) {
//            LOG.info("{}starting croupier:<{},{}>",
//                    new Object[]{logPrefix, req.overlayId.getValue0(), req.overlayId.getValue1()});
//            if (OverlayMngrHelper.isGlobalCroupierId(req.overlayId)) {
//                LOG.debug("{}returning global croupier", logPrefix);
//                answer(req, req.answer(globalCroupier.getPositive(CroupierPort.class).getPair()));
//            } else {
//                LOG.debug("{}waiting for bootstrap sample for croupier:<{},{}>",
//                        new Object[]{logPrefix, req.overlayId.getValue0(), req.overlayId.getValue1()});
//                pendingOverlays.put(req.id, new StartCroupier(req));
//                trigger(new CCOverlaySample.Request(req.overlayId.getValue1()), heartbeat);
//            }
//        }
//    };

    public static class OverlayMngrInit extends Init<OverlayMngrComp> {

        public final SystemConfig systemConfig;

        public OverlayMngrInit(SystemConfig systemConfig) {
            this.systemConfig = systemConfig;
        }
    }

    private void scheduleInternalStateCheck() {
        if (internalStateCheck != null) {
            LOG.warn("{} double starting internal state check", logPrefix);
            return;
        }
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(oMngrConfig.internalStateCheck, oMngrConfig.internalStateCheck);
        InternalStateCheck isc = new InternalStateCheck(spt);
        spt.setTimeoutEvent(isc);
        internalStateCheck = isc.getTimeoutId();
        trigger(spt, timer);
    }

    private void cancelInternalStateCheck() {
        if (internalStateCheck == null) {
            LOG.warn("{} double stopping internal state check", logPrefix);
            return;
        }
        CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(internalStateCheck);
        internalStateCheck = null;
        trigger(cpt, timer);
    }

    public class InternalStateCheck extends Timeout {

        public InternalStateCheck(SchedulePeriodicTimeout spt) {
            super(spt);
        }

        @Override
        public String toString() {
            return "INTERNAL_STATE_CHECK";
        }
    }

    interface StartOverlay {

        UUID getId();

        void bootstrap(List<DecoratedAddress> boostrap);
    }

    class StartCroupier implements StartOverlay {

        Croupier.Start req;

        public StartCroupier(Croupier.Start req) {
            this.req = req;
        }

        @Override
        public UUID getId() {
            return req.id;
        }

        @Override
        public void bootstrap(List<DecoratedAddress> bootstrap) {
            LOG.debug("{}received bootstrap:{} - starting croupier",
                    new Object[]{logPrefix, bootstrap});
//           Component croupier = create(CroupierComp.class, new CroupierComp.CroupierInit(null, null, overlayId));
        }
    }
}

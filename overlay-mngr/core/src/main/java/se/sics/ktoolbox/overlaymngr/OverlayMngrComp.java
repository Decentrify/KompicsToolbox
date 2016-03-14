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

import java.util.HashMap;
import java.util.Map;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Fault;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatPort;
import se.sics.ktoolbox.croupier.CroupierComp;
import se.sics.ktoolbox.croupier.CroupierControlPort;
import se.sics.ktoolbox.croupier.CroupierPort;
import se.sics.ktoolbox.gradient.GradientComp;
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.gradient.temp.RankUpdatePort;
import se.sics.ktoolbox.overlaymngr.bootstrap.CroupierBootstrapComp;
import se.sics.ktoolbox.overlaymngr.bootstrap.CroupierBootstrapPort;
import se.sics.ktoolbox.overlaymngr.events.OMngrCroupier;
import se.sics.ktoolbox.overlaymngr.events.OMngrTGradient;
import se.sics.ktoolbox.overlaymngr.bootstrap.OMCroupierBootstrap;
import se.sics.ktoolbox.tgradient.TreeGradientComp;
import se.sics.ktoolbox.util.address.AddressUpdatePort;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.IntIdentifier;
import se.sics.ktoolbox.util.identifier.OverlayIdHelper;
import se.sics.ktoolbox.util.network.ports.One2NChannel;
import se.sics.ktoolbox.util.overlays.EventOverlayIdExtractor;
import se.sics.ktoolbox.util.overlays.MsgOverlayIdExtractor;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdatePort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OverlayMngrComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(OverlayMngrComp.class);
    private String logPrefix = "";

    //*****************************CONNECTIONS**********************************
    //externally providing ports - CONNECT(externally) to these
    private final Negative overlayMngr = provides(OverlayMngrPort.class);
    private final One2NChannel<CroupierPort> croupierEnd
            = One2NChannel.getChannel(provides(CroupierPort.class), new EventOverlayIdExtractor());
    private final One2NChannel<GradientPort> tgradientEnd
            = One2NChannel.getChannel(provides(GradientPort.class), new EventOverlayIdExtractor());
    private final One2NChannel<OverlayViewUpdatePort> viewUpdateEnd
            = One2NChannel.getChannel(requires(OverlayViewUpdatePort.class), new EventOverlayIdExtractor());
     //externally provided ports - NO connecting - we don't like chaining ports
    private final ExtPort extPorts;
    private final One2NChannel<Network> networkEnd;
    //internal connection helper
    private One2NChannel<CroupierControlPort> bootstrapEnd;
    //***************************CLEANUP PURPOSES*******************************
    private Pair<Component, Channel[]> bootstrapComp;
    //croupier
    //<croupierId, <croupier, internalChannels>
    private final Map<Identifier, Pair<Component, Channel[]>> croupierLayers = new HashMap<>();
    private final Map<Identifier, OMngrCroupier.ConnectRequest> croupierContext = new HashMap<>();

    //tgradients (with their, croupierPort and gradient)
    //<tgradientId, <tGradient, internalChannels>>
    private final Map<Identifier, Pair<Component, Channel[]>> gradientLayers = new HashMap<>();
    private final Map<Identifier, Pair<Component, Channel[]>> tgradientLayers = new HashMap<>();
    private final Map<Identifier, OMngrTGradient.ConnectRequest> tgradientContext = new HashMap<>();

    public OverlayMngrComp(Init init) {
        SystemKCWrapper systemConfig = new SystemKCWrapper(config());
        logPrefix = "<nid:" + systemConfig.id + "> ";
        LOG.info("{}initiating with seed:{}", logPrefix, systemConfig.seed);

        extPorts = init.extPorts;
        networkEnd = One2NChannel.getChannel(extPorts.networkPort, new MsgOverlayIdExtractor());

        connectCroupierBootstrap();

        subscribe(handleStart, control);
        subscribe(handleConnectCroupier, overlayMngr);
        subscribe(handleConnectTGradient, overlayMngr);
    }

    //****************************CONTROL***************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
        }
    };

    @Override
    public Fault.ResolveAction handleFault(Fault fault) {
        LOG.info("{}component:{} fault:{}",
                new Object[]{logPrefix, fault.getSource().getClass(), fault.getCause().getMessage()});
        return Fault.ResolveAction.ESCALATE;
    }

    //*********************************BOOTSTRAP********************************
    private void connectCroupierBootstrap() {
        Component cBootComp = create(CroupierBootstrapComp.class, new CroupierBootstrapComp.Init());
        Channel[] cBootChannels = new Channel[2];
        cBootChannels[0] = connect(cBootComp.getNegative(Timer.class), extPorts.timerPort, Channel.TWO_WAY);
        cBootChannels[1] = connect(cBootComp.getNegative(CCHeartbeatPort.class), extPorts.heartbeatPort, Channel.TWO_WAY);
        //croupierControlPort - connect to croupiers
        bootstrapEnd = One2NChannel.getChannel(cBootComp.getNegative(CroupierControlPort.class), new EventOverlayIdExtractor());
        //bootstrapPort - used by parent;
        bootstrapComp = Pair.with(cBootComp, cBootChannels);
    }
    //**********************************CROUPIER********************************
    Handler handleConnectCroupier = new Handler<OMngrCroupier.ConnectRequest>() {
        @Override
        public void handle(OMngrCroupier.ConnectRequest req) {
            LOG.info("{}{}", new Object[]{logPrefix, req});

            if (croupierLayers.containsKey(req.croupierId) || OverlayMngrConfig.isGlobalCroupier(req.croupierId)) {
                LOG.error("{}double start of croupier", logPrefix);
                throw new RuntimeException("double start of croupier");
            }

            Component croupierComp = create(CroupierComp.class, new CroupierComp.Init(req.croupierId));
            Channel[] croupierChannels = new Channel[2];
            //provided external ports
            networkEnd.addChannel(req.croupierId, croupierComp.getNegative(Network.class));
            croupierChannels[0] = connect(croupierComp.getNegative(Timer.class), extPorts.timerPort, Channel.TWO_WAY);
            croupierChannels[1] = connect(croupierComp.getNegative(AddressUpdatePort.class), extPorts.addressUpdatePort, Channel.TWO_WAY);
            //providing external ports
            viewUpdateEnd.addChannel(req.croupierId, croupierComp.getNegative(OverlayViewUpdatePort.class));
            croupierEnd.addChannel(req.croupierId, croupierComp.getPositive(CroupierPort.class));
            //control
            bootstrapEnd.addChannel(req.croupierId, croupierComp.getPositive(CroupierControlPort.class));

            croupierLayers.put(req.croupierId, Pair.with(croupierComp, croupierChannels));
            croupierContext.put(req.croupierId, req);

            trigger(new OMCroupierBootstrap(req.croupierId),
                    bootstrapComp.getValue0().getPositive(CroupierBootstrapPort.class));
            trigger(Start.event, croupierComp.control());
            answer(req, req.answer());
        }
    };

    //*******************************GRADIENT***********************************
    Handler handleConnectTGradient = new Handler<OMngrTGradient.ConnectRequest>() {
        @Override
        public void handle(OMngrTGradient.ConnectRequest req) {
            LOG.info("{}{}", new Object[]{logPrefix, req});

            Identifier croupierId = OverlayIdHelper.changeOverlayType((IntIdentifier) req.tgradientId, OverlayIdHelper.Type.CROUPIER);
            Identifier gradientId = OverlayIdHelper.changeOverlayType((IntIdentifier) req.tgradientId, OverlayIdHelper.Type.GRADIENT);
            if (tgradientLayers.containsKey(req.tgradientId)
                    || croupierLayers.containsKey(croupierId)
                    || tgradientLayers.containsKey(gradientId)) {
                LOG.error("{}double start of gradient", logPrefix);
                throw new RuntimeException("double start of gradient");
            }

            //croupier
            Component croupierComp = create(CroupierComp.class, new CroupierComp.Init(croupierId));
            Channel[] croupierChannels = new Channel[2];
            //provided external ports
            networkEnd.addChannel(croupierId, croupierComp.getNegative(Network.class));
            croupierChannels[0] = connect(croupierComp.getNegative(Timer.class), extPorts.timerPort, Channel.TWO_WAY);
            croupierChannels[1] = connect(croupierComp.getNegative(AddressUpdatePort.class), extPorts.addressUpdatePort, Channel.TWO_WAY);
            //providing external ports
            croupierEnd.addChannel(croupierId, croupierComp.getPositive(CroupierPort.class));
            //control
            bootstrapEnd.addChannel(croupierId, croupierComp.getPositive(CroupierControlPort.class));
            //viewUpdate, croupier - connected by gradient˜
            //bootstrap port not yet fully defined
            
            croupierLayers.put(croupierId, Pair.with(croupierComp, croupierChannels));

            //gradient
            Component gradientComp = create(GradientComp.class, new GradientComp.Init(gradientId,
                    req.utilityComparator, req.gradientFilter));
            Channel[] gradientChannels = new Channel[4];
            //provided external ports
            networkEnd.addChannel(gradientId, gradientComp.getNegative(Network.class));
            gradientChannels[0] = connect(gradientComp.getNegative(Timer.class), extPorts.timerPort, Channel.TWO_WAY);
            gradientChannels[1] = connect(gradientComp.getNegative(AddressUpdatePort.class), extPorts.addressUpdatePort, Channel.TWO_WAY);
            //internal
            gradientChannels[2] = connect(gradientComp.getPositive(OverlayViewUpdatePort.class),
                    croupierComp.getNegative(OverlayViewUpdatePort.class), Channel.TWO_WAY);
            gradientChannels[3] = connect(gradientComp.getNegative(CroupierPort.class),
                    croupierComp.getPositive(CroupierPort.class), Channel.TWO_WAY);
            //viewUpdate, gradient, rankUpdate connected by tgradinet
            gradientLayers.put(gradientId, Pair.with(gradientComp, gradientChannels));

            //tgradient
            Component tgradientComp = create(TreeGradientComp.class, new TreeGradientComp.Init(
                    req.tgradientId, req.gradientFilter));
            Channel[] tgradientChannels = new Channel[6];
            //provided external ports
            networkEnd.addChannel(req.tgradientId, tgradientComp.getNegative(Network.class));
            tgradientChannels[0] = connect(tgradientComp.getNegative(Timer.class), extPorts.timerPort, Channel.TWO_WAY);
            tgradientChannels[1] = connect(tgradientComp.getNegative(AddressUpdatePort.class), extPorts.addressUpdatePort, Channel.TWO_WAY);
            //internal
            tgradientChannels[2] = connect(tgradientComp.getNegative(CroupierPort.class),
                    croupierComp.getPositive(CroupierPort.class), Channel.TWO_WAY);
            tgradientChannels[3] = connect(tgradientComp.getNegative(GradientPort.class),
                    gradientComp.getPositive(GradientPort.class), Channel.TWO_WAY);
            tgradientChannels[4] = connect(tgradientComp.getPositive(OverlayViewUpdatePort.class),
                    gradientComp.getNegative(OverlayViewUpdatePort.class), Channel.TWO_WAY);
            tgradientChannels[5] = connect(tgradientComp.getNegative(RankUpdatePort.class),
                    gradientComp.getPositive(RankUpdatePort.class), Channel.TWO_WAY);
            //providing external port
            viewUpdateEnd.addChannel(req.tgradientId, tgradientComp.getNegative(OverlayViewUpdatePort.class));
            tgradientEnd.addChannel(req.tgradientId, tgradientComp.getPositive(GradientPort.class));
            
            tgradientLayers.put(req.tgradientId, Pair.with(tgradientComp, tgradientChannels));
            tgradientContext.put(req.getId(), req);

            trigger(new OMCroupierBootstrap(croupierId),
                    bootstrapComp.getValue0().getPositive(CroupierBootstrapPort.class));
            trigger(Start.event, croupierComp.control());
            trigger(Start.event, gradientComp.control());
            trigger(Start.event, tgradientComp.control());
            answer(req, req.answer());
        }
    };

    public static class Init extends se.sics.kompics.Init<OverlayMngrComp> {

        public final ExtPort extPorts;

        public Init(ExtPort extPorts) {
            this.extPorts = extPorts;
        }
    }

    public static class ExtPort {

        public final Positive<Timer> timerPort;
        public final Positive<Network> networkPort;
        public final Positive<AddressUpdatePort> addressUpdatePort;
        public final Positive<CCHeartbeatPort> heartbeatPort;

        public ExtPort(Positive<Timer> timerPort, Positive<Network> networkPort, 
                Positive<AddressUpdatePort> addressUpdatePort, Positive<CCHeartbeatPort> heartbeatPort) {
            this.timerPort = timerPort;
            this.networkPort = networkPort;
            this.addressUpdatePort = addressUpdatePort;
            this.heartbeatPort = heartbeatPort;
        }
    }
}

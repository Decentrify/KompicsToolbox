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
package se.sics.ktoolbox.netmngr;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import se.sics.ktoolbox.netmngr.nxnet.NxNetPort;
import se.sics.ktoolbox.netmngr.event.NetMngrReady;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.netmngr.chunk.ChunkMngrComp;
import se.sics.ktoolbox.netmngr.ipsolver.IpSolverComp;
import se.sics.ktoolbox.netmngr.ipsolver.IpSolverPort;
import se.sics.ktoolbox.netmngr.ipsolver.IpSolve;
import se.sics.ktoolbox.netmngr.chunk.util.CMTrafficSelector;
import se.sics.ktoolbox.netmngr.nxnet.NxNetBind;
import se.sics.ktoolbox.netmngr.nxnet.NxNetComp;
import se.sics.ktoolbox.netmngr.nxnet.NxNetUnbind;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicAddress;
import se.sics.ktoolbox.util.network.nat.NatAwareAddress;
import se.sics.ktoolbox.util.network.nat.NatAwareAddressImpl;
import se.sics.ktoolbox.util.network.ports.One2NChannel;
import se.sics.ktoolbox.util.network.ports.ShortCircuitChannel;
import se.sics.ktoolbox.util.idextractor.MsgOverlayIdExtractor;
import se.sics.ktoolbox.util.status.Status;
import se.sics.ktoolbox.util.status.StatusPort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NetworkMngrComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkMngrComp.class);
    private String logPrefix = "";

    //*****************************CONNECTIONS**********************************
    //***************************EXTERNAL_CONNECT*******************************
    private final Negative<NetMngrPort> netMngrPort = provides(NetMngrPort.class);
    private final Negative<Network> networkPort = provides(Network.class);
    private final Negative<StatusPort> statusPort = provides(StatusPort.class);
    //*************************INTERNAL_NO_CONNECT******************************
    private final Positive<IpSolverPort> ipSolverPort = requires(IpSolverPort.class);
    private final Positive<NxNetPort> nxNetPort = requires(NxNetPort.class);
    //****************************CONFIGURATION*********************************
    private final SystemKCWrapper systemConfig;
    private final NetMngrKCWrapper netMngrConfig;
    //**************************INTERNAL_STATE**********************************
    private InetAddress boundIp;
    //**************************EXTERNAL_STATE**********************************
    private final ExtPort extPorts;
    //****************************AUX_STATE*************************************
    private Map<Identifier, NetMngrBind.Request> pendingBind = new HashMap<>();
    private Map<Identifier, NetMngrUnbind.Request> pendingUnbind = new HashMap<>();
    //*****************************CLEANUP**************************************
    private Component ipSolverComp;
    private Component nxNetComp;
    private Component chunkMngrComp;

    public NetworkMngrComp(Init init) {
        systemConfig = new SystemKCWrapper(config());
        logPrefix = "<nid:" + systemConfig.id + "> ";
        LOG.info("{}initializing...", logPrefix);

        netMngrConfig = new NetMngrKCWrapper(config());

        extPorts = init.extPorts;

        subscribe(handleStart, control);
        subscribe(handleIpDetected, ipSolverPort);
        subscribe(handleBindReq, netMngrPort);
        subscribe(handleBindResp, nxNetPort);
        subscribe(handleUnbindReq, netMngrPort);
        subscribe(handleUnbindResp, nxNetPort);
    }

    private Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            setIpSolver();
            setNxNet();
            setChunkMngr();
            trigger(Start.event, ipSolverComp.control());
            trigger(Start.event, nxNetComp.control());
            trigger(Start.event, chunkMngrComp.control());
            trigger(new IpSolve.Request(netMngrConfig.ipTypes), ipSolverPort);
        }
    };

    private void setIpSolver() {
        ipSolverComp = create(IpSolverComp.class, new IpSolverComp.IpSolverInit());
        connect(ipSolverComp.getPositive(IpSolverPort.class), ipSolverPort.getPair(), Channel.TWO_WAY);
    }

    private void setNxNet() {
        nxNetComp = create(NxNetComp.class, new NxNetComp.Init());
        connect(nxNetComp.getPositive(NxNetPort.class), nxNetPort.getPair(), Channel.TWO_WAY);
    }

    private void setChunkMngr() {
        chunkMngrComp = create(ChunkMngrComp.class, new ChunkMngrComp.Init());
        connect(chunkMngrComp.getNegative(Timer.class), extPorts.timerPort, Channel.TWO_WAY);
        ShortCircuitChannel.getChannel(
                nxNetComp.getPositive(Network.class), chunkMngrComp.getPositive(Network.class), new CMTrafficSelector.Outgoing(),
                networkPort, chunkMngrComp.getNegative(Network.class), new CMTrafficSelector.Incoming());
    }

    private Handler handleIpDetected = new Handler<IpSolve.Response>() {
        @Override
        public void handle(IpSolve.Response resp) {
            LOG.info("{}new ips detected", logPrefix);
            if (resp.boundIp == null) {
                throw new RuntimeException("no bound ip");
            }
            boundIp = resp.boundIp;
            NatAwareAddress systemAdr = NatAwareAddressImpl.open(new BasicAddress(resp.boundIp, systemConfig.port, systemConfig.id));
            LOG.info("{}binding system adr:{}", new Object[]{logPrefix, systemAdr});
            trigger(new NxNetBind.Request(systemAdr), nxNetPort);
        }
    };

    private Handler handleBindResp = new Handler<NxNetBind.Response>() {
        @Override
        public void handle(NxNetBind.Response resp) {
            LOG.trace("{}received:{}", logPrefix, resp);
            if (resp.req.bindAdr.getPort() == systemConfig.port) {
                //tell everyone
                NetMngrReady ready = new NetMngrReady((NatAwareAddress) resp.req.bindAdr);
                LOG.info("{}ready", logPrefix);
                trigger(new Status.Internal(ready), statusPort);
            } else {
                NetMngrBind.Request req = pendingBind.remove(resp.req.getId());
                if (req == null) {
                    throw new RuntimeException("logic error - cleanup problems");
                }
                answer(req, req.answer());
            }
        }
    };

    private Handler handleBindReq = new Handler<NetMngrBind.Request>() {
        @Override
        public void handle(NetMngrBind.Request req) {
            LOG.trace("{}received:{}", logPrefix, req);
            NatAwareAddress adr = NatAwareAddressImpl.open(new BasicAddress(boundIp, req.port, systemConfig.id));
            NxNetBind.Request bindReq = new NxNetBind.Request(adr);
            trigger(bindReq, nxNetPort);
            pendingBind.put(bindReq.getId(), req);
        }
    };

    private Handler handleUnbindReq = new Handler<NetMngrUnbind.Request>() {
        @Override
        public void handle(NetMngrUnbind.Request req) {
            LOG.trace("{}received:{}", logPrefix, req);
            NxNetUnbind.Request unbindReq = new NxNetUnbind.Request(req.port);
            trigger(unbindReq, nxNetPort);
            pendingUnbind.put(unbindReq.getId(), req);
        }
    };

    private Handler handleUnbindResp = new Handler<NxNetUnbind.Response>() {
        @Override
        public void handle(NxNetUnbind.Response resp) {
            LOG.trace("{}received:{}", logPrefix, resp);
            NetMngrUnbind.Request req = pendingUnbind.remove(resp.req.getId());
            if (req == null) {
                throw new RuntimeException("logic error - cleanup problems");
            }
            answer(req, req.answer());
        }
    };

    public static class Init extends se.sics.kompics.Init<NetworkMngrComp> {

        public final ExtPort extPorts;

        public Init(ExtPort extPorts) {
            this.extPorts = extPorts;
        }
    }

    public static class ExtPort {

        public final Positive<Timer> timerPort;

        public ExtPort(Positive<Timer> timerPort) {
            this.timerPort = timerPort;
        }
    }
}

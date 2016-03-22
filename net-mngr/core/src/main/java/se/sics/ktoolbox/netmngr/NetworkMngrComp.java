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
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicAddress;
import se.sics.ktoolbox.util.network.nat.NatAwareAddressImpl;
import se.sics.ktoolbox.util.network.ports.ShortCircuitChannel;
import se.sics.ktoolbox.util.status.Status;
import se.sics.ktoolbox.util.status.StatusPort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NetworkMngrComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkMngrComp.class);
    private String logPrefix = "";

    //*****************************CONNECTIONS**********************************
    //externally provided ports - NO connecting - we don't like chaining ports
    private final ExtPort extPorts;
    //externally providing ports - CONNECT
    private final Negative<Network> networkPort = provides(Network.class);
    private final Negative<StatusPort> statusPort = provides(StatusPort.class);
    //internal ports - NO connecting
    private final Positive<IpSolverPort> ipSolverPort = requires(IpSolverPort.class);
    //****************************CONFIGURATION*********************************
    private final SystemKCWrapper systemConfig;
    private final NetMngrKCWrapper netMngrConfig;
    //*******************************SELF***************************************
    private KAddress systemAdr;
    //*****************************CLEANUP**************************************
    private Pair<Component, Channel[]> ipSolver;
    private Component network;
    private Pair<Component, Channel[]> chunkMngr;

    public NetworkMngrComp(Init init) {
        systemConfig = new SystemKCWrapper(config());
        logPrefix = "<nid:" + systemConfig.id + "> ";
        LOG.info("{}initializing...", logPrefix);

        netMngrConfig = new NetMngrKCWrapper(config());

        extPorts = init.extPorts;

        subscribe(handleStart, control);

        connectIpSolver();
        subscribe(handleIpDetected, ipSolverPort);
    }

    private boolean ready() {
        if (systemAdr == null) {
            LOG.warn("{}self address not yet defined", logPrefix);
            return false;
        }
        return true;
    }
    //*****************************CONTROL**************************************
    private Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            trigger(new IpSolve.Request(netMngrConfig.ipTypes), ipSolverPort);
        }
    };

    private Handler handleIpDetected = new Handler<IpSolve.Response>() {
        @Override
        public void handle(IpSolve.Response event) {
            LOG.info("{}new ips detected", logPrefix);
            if (event.boundIp == null) {
                throw new RuntimeException("no bound ip");
            }
            systemAdr = NatAwareAddressImpl.open(new BasicAddress(event.boundIp, systemConfig.port, systemConfig.id));

            //tell everyone
            NetMngrReady ready = new NetMngrReady(systemAdr);
            LOG.trace("{}sending:{}", new Object[]{logPrefix, ready});
            trigger(new Status.Internal(ready), statusPort);

            connectNetwork();
            connectChunkMngr();
            trigger(Start.event, network.control());
            trigger(Start.event, chunkMngr.getValue0().control());
        }
    };

    //****************************CONNECTIONS***********************************

    private void connectIpSolver() {
        Component ipSolverComp = create(IpSolverComp.class, new IpSolverComp.IpSolverInit());
        Channel[] ipSolverChannels = new Channel[1];
        ipSolverChannels[0] = connect(ipSolverComp.getPositive(IpSolverPort.class), ipSolverPort.getPair(), Channel.TWO_WAY);
        ipSolver = Pair.with(ipSolverComp, ipSolverChannels);
    }

    private void connectNetwork() {
        network = create(NettyNetwork.class, new NettyInit(systemAdr));
    }

    private void connectChunkMngr() {
        Component chunkMngrComp = create(ChunkMngrComp.class, new ChunkMngrComp.Init());
        Channel[] chunkMngrChannels = new Channel[2];
        chunkMngrChannels[0] = connect(chunkMngrComp.getNegative(Timer.class), extPorts.timerPort, Channel.TWO_WAY);
        chunkMngrChannels[1] = ShortCircuitChannel.getChannel(
                network.getPositive(Network.class), chunkMngrComp.getPositive(Network.class), new CMTrafficSelector.Outgoing(),
                networkPort, chunkMngrComp.getNegative(Network.class), new CMTrafficSelector.Incoming());
        chunkMngr = Pair.with(chunkMngrComp, chunkMngrChannels);
    }

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

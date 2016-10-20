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
package se.sics.ktoolbox.netmngr.nxnet;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kill;
import se.sics.kompics.Negative;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.ktoolbox.netmngr.NetworkMngrComp;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.basic.IntIdFactory;
import se.sics.ktoolbox.util.idextractor.SourcePortIdExtractor;
import se.sics.ktoolbox.util.network.ports.One2NChannel;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NxNetComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkMngrComp.class);
    private String logPrefix = "";

    //*****************************CONNECTIONS**********************************
    //***************************EXTERNAL_CONNECT*******************************
    Negative<NxNetPort> nxNetPort = provides(NxNetPort.class);
    Negative<Network> networkPort = provides(Network.class);
    //*******************************CONFIG*************************************
    private SystemKCWrapper systemConfig;
    //*************************INTERNAL_NO_CONNECT******************************
    private One2NChannel<Network> networkEnd;
    //***************************INTERNAL_STATE*********************************
    private Map<Integer, Component> networks = new HashMap<>();

    public NxNetComp(Init init) {
        systemConfig = new SystemKCWrapper(config());
        logPrefix = "<nid:" + systemConfig.id + "> ";
        LOG.info("{}starting...", logPrefix);

        networkEnd = One2NChannel.getChannel("nxnet", networkPort, new SourcePortIdExtractor());

        subscribe(handleStart, control);
        subscribe(handleBindReq, nxNetPort);
        subscribe(handleUnbindReq, nxNetPort);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting", logPrefix);
        }
    };

    Handler handleBindReq = new Handler<NxNetBind.Request>() {
        @Override
        public void handle(NxNetBind.Request req) {
            LOG.trace("{}received:{}", logPrefix, req);
            if (networks.containsKey(req.bindAdr.getPort())) {
                LOG.warn("{}port:{} already bound - will not bind again", logPrefix, req.bindAdr.getPort());
                answer(req, req.answer());
                return;
            }
            Component network = create(NettyNetwork.class, new NettyInit(req.bindAdr));
            IntIdFactory intIdFactory = new IntIdFactory(null);
            networkEnd.addChannel(intIdFactory.rawId(req.bindAdr.getPort()), network.getPositive(Network.class));
            trigger(Start.event, network.control());
            networks.put(req.bindAdr.getPort(), network);
            LOG.info("{}binding port:{}", new Object[]{logPrefix, req.bindAdr.getPort()});
            answer(req, req.answer());
        }
    };

    Handler handleUnbindReq = new Handler<NxNetUnbind.Request>() {

        @Override
        public void handle(NxNetUnbind.Request req) {
            LOG.trace("{}received:{}", logPrefix, req);
            Component network = networks.remove(req.port);
            if (network == null) {
                LOG.warn("{}port:{} not bound", logPrefix, req.port);
                answer(req, req.answer());
                return;
            }
            IntIdFactory intIdFactory = new IntIdFactory(null);
            networkEnd.removeChannel(intIdFactory.rawId(req.port), network.getPositive(Network.class));
            trigger(Kill.event, network.control());
            LOG.info("{}unbinding port:{}", new Object[]{logPrefix, req.port});
            answer(req, req.answer());
        }
    };

    public static class Init extends se.sics.kompics.Init<NxNetComp> {
    }
}

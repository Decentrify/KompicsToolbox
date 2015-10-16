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
package se.sics.ktoolbox.networkmngr;

import com.google.common.base.Optional;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.ktoolbox.networkmngr.events.Bind;
import se.sics.ktoolbox.networkmngr.msg.NodeMsg;
import se.sics.p2ptoolbox.util.config.KConfigCache;
import se.sics.p2ptoolbox.util.config.KConfigCore;
import se.sics.p2ptoolbox.util.network.ContentMsg;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NodeComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(NodeComp.class);
    private String logPrefix = "";

    Positive<Network> network = requires(Network.class);
    Positive<NetworkMngrPort> networkMngr = requires(NetworkMngrPort.class);

    private final KConfigCache config;

    private final Pair<DecoratedAddress, DecoratedAddress> self;
    private final DecoratedAddress partner;

    private boolean bound1 = false;
    private boolean bound2 = false;

    public NodeComp(NodeInit init) {
        this.config = init.config;
        InetAddress publicIp;
        InetAddress partnerIp;
        try {
            publicIp = InetAddress.getByName(config.read(NodeConfig.selfPublicIp).get());
            partnerIp = InetAddress.getByName(config.read(NodeConfig.partnerIp).get());
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
        this.self = Pair.with(
                DecoratedAddress.open(publicIp, config.read(NodeConfig.selfPort1).get(),
                        config.read(NodeConfig.selfId).get()),
                DecoratedAddress.open(publicIp, config.read(NodeConfig.selfPort2).get(),
                        config.read(NodeConfig.selfId).get()));

        this.partner = DecoratedAddress.open(partnerIp, config.read(NodeConfig.partnerPort).get(), 
                config.read(NodeConfig.partnerId).get());
        this.logPrefix = self.getValue0().getIp() + " ";
        LOG.info("{}initiating...", logPrefix);

        subscribe(handleStart, control);
        subscribe(handleBindResp, networkMngr);
        subscribe(handleRequest, network);
        subscribe(handleResponse, network);
//        subscribe(handleMsg, network);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            InetAddress alternateBind;
            try {
                alternateBind = InetAddress.getByName(config.read(NodeConfig.selfAltBindIp).get());
            } catch (UnknownHostException ex) {
                throw new RuntimeException(ex);
            }
            trigger(new Bind.Request(UUID.randomUUID(), self.getValue0(), Optional.of(alternateBind), true), networkMngr);
            trigger(new Bind.Request(UUID.randomUUID(), self.getValue1(), Optional.of(alternateBind), true), networkMngr);
        }
    };

    Handler handleBindResp = new Handler<Bind.Response>() {
        @Override
        public void handle(Bind.Response resp) {
            LOG.info("{}bound:{}", logPrefix, resp.boundPort);
            if (self.getValue0().getPort() == resp.boundPort) {
                bound1 = true;
            } else {
                bound2 = true;
            }

            if (bound1 && bound2) {
                if (self.getValue0().getId() == 0) {
                    DecoratedHeader<DecoratedAddress> requestHeader = new DecoratedHeader(self.getValue0(), partner, Transport.UDP);
                    ContentMsg request = new BasicContentMsg(requestHeader, new NodeMsg.Request(1));
                    LOG.info("{}sending request src:{} dst:{}",
                            new Object[]{logPrefix, requestHeader.getSource(), requestHeader.getDestination()});
                    trigger(request, network);
                }
            }
        }
    };

    Handler handleMsg = new Handler<BasicContentMsg>() {
        @Override
        public void handle(BasicContentMsg msg) {
            if (msg.getContent() instanceof NodeMsg.Request) {
                NodeMsg.Request req = (NodeMsg.Request) msg.getContent();
                LOG.info("{}received req src:{} dst:{} p:{}",
                        new Object[]{logPrefix, msg.getHeader().getSource(), msg.getHeader().getDestination(), req.p});
            } else {
                NodeMsg.Response resp = (NodeMsg.Response) msg.getContent();
                LOG.info("{}received resp src:{} dst:{} p:{}",
                        new Object[]{logPrefix, msg.getHeader().getSource(), msg.getHeader().getDestination(), resp.p});
            }
        }
    };

    ClassMatchedHandler handleRequest
            = new ClassMatchedHandler<NodeMsg.Request, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, NodeMsg.Request>>() {
                @Override
                public void handle(NodeMsg.Request content, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, NodeMsg.Request> container) {
                    LOG.info("{}received request src:{} dst:{} p:{}",
                            new Object[]{logPrefix, container.getSource(), container.getDestination(), content.p});
                    DecoratedHeader<DecoratedAddress> responseHeader = new DecoratedHeader(self.getValue1(), container.getSource(), Transport.UDP);
                    ContentMsg response = new BasicContentMsg(responseHeader, new NodeMsg.Response(content.p + 1));
                    LOG.info("{}sending response src:{} dst:{}",
                            new Object[]{logPrefix, responseHeader.getSource(), responseHeader.getDestination()});
                    trigger(response, network);
                }
            };

    ClassMatchedHandler handleResponse
            = new ClassMatchedHandler<NodeMsg.Response, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, NodeMsg.Response>>() {
                @Override
                public void handle(NodeMsg.Response content, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, NodeMsg.Response> container) {
                    LOG.info("{}received response src:{} dst:{} p:{}",
                            new Object[]{logPrefix, container.getSource(), container.getDestination(), content.p});
                }
            };

    public static class NodeInit extends Init<NodeComp> {

        public final KConfigCache config;

        public NodeInit(KConfigCore config) {
            this.config = new KConfigCache(config);
        }
    }
}

/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.echo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.ktoolbox.echo.serializers.StunEchoSerializerSetup;
import se.sics.p2ptoolbox.util.network.ContentMsg;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.BasicHeader;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;
import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class StunEchoLauncher extends ComponentDefinition {

    public static Type type;
    public static Address self;
    public static Address target;

    private static Logger LOG = LoggerFactory.getLogger(StunEchoLauncher.class);
    private String logPrefix = "";

    private Component networkComp;

    public StunEchoLauncher() {
        LOG.info("{}initiating", logPrefix);

        registerSerializers();
        subscribe(handleStart, control);
        subscribe(handleStop, control);
    }

    private void registerSerializers() {
        int currentId = 128;
        currentId = BasicSerializerSetup.registerBasicSerializers(currentId);
        currentId = StunEchoSerializerSetup.registerSerializers(currentId);
    }

    Handler handleStart = new Handler<Start>() {

        @Override
        public void handle(Start e) {
            LOG.info("{}starting");
            connectNetwork();
            subscribe(handlePing, networkComp.getPositive(Network.class));
            subscribe(handlePong, networkComp.getPositive(Network.class));

            if (type.equals(Type.PING)) {
                DecoratedHeader<DecoratedAddress> requestHeader = new DecoratedHeader(new BasicHeader(self, target, Transport.UDP), null, 0);
                ContentMsg request = new BasicContentMsg(requestHeader, new Ping());
                trigger(request, networkComp.getPositive(Network.class));
            }
        }
    };

    Handler handleStop = new Handler<Stop>() {

        @Override
        public void handle(Stop e) {
            LOG.info("{}stopping");
        }
    };

    private void connectNetwork() {
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName("0.0.0.0");
        } catch (UnknownHostException ex) {
            LOG.error("{}target binding exception");
            System.exit(1);
        }
        Address bind = new DecoratedAddress(new BasicAddress(ip, self.getPort(), -1));
        networkComp = create(NettyNetwork.class, new NettyInit(bind));
        trigger(Start.event, networkComp.control());
    }

    ClassMatchedHandler handlePing
            = new ClassMatchedHandler<Ping, BasicContentMsg<Address, DecoratedHeader<Address>, Ping>>() {
                @Override
                public void handle(Ping content, BasicContentMsg<Address, DecoratedHeader<Address>, Ping> container) {
                    LOG.info("{}received ping from:{}", logPrefix, container.getSource());
                    DecoratedHeader<DecoratedAddress> requestHeader = new DecoratedHeader(new BasicHeader(self, container.getSource(), Transport.UDP), null, 0);
                    ContentMsg request = new BasicContentMsg(requestHeader, new Pong(container.getSource()));
                    trigger(request, networkComp.getPositive(Network.class));
                }
            };

    ClassMatchedHandler handlePong
            = new ClassMatchedHandler<Pong, BasicContentMsg<Address, DecoratedHeader<Address>, Pong>>() {
                @Override
                public void handle(Pong content, BasicContentMsg<Address, DecoratedHeader<Address>, Pong> container) {
                    LOG.info("{}received pong from:{} with pingSrc:{}",
                            new Object[]{logPrefix, container.getSource(), content.pingSrc});
                }
            };

    public static enum Type {

        PING, PONG;
    }

    public static void main(String[] args) {
        if (args.length > 1) {
            if (args[0].equals("-ping")) {
                type = StunEchoLauncher.Type.PING;
                InetAddress ipS = null;
                try {
                    ipS = InetAddress.getByName(args[1]);
                    self = new DecoratedAddress(new BasicAddress(ipS, 34543, 0));
                } catch (UnknownHostException ex) {
                    LOG.error("{}target binding exception");
                    System.exit(1);
                }
                if (args.length > 2) {
                    InetAddress ip = null;
                    try {
                        ip = InetAddress.getByName(args[2]);
                        target = new DecoratedAddress(new BasicAddress(ip, 34544, 1));
                    } catch (UnknownHostException ex) {
                        LOG.error("{}target binding exception");
                        System.exit(1);
                    }
                } else {
                    LOG.error("-ping should have a target");
                    System.exit(1);
                }
            } else if (args[0].equals("-pong")) {
                type = StunEchoLauncher.Type.PONG;
                InetAddress ipS = null;
                try {
                    ipS = InetAddress.getByName(args[1]);
                    self = new DecoratedAddress(new BasicAddress(ipS, 34544, 1));
                } catch (UnknownHostException ex) {
                    LOG.error("{}target binding exception");
                    System.exit(1);
                }
            } else {
                LOG.error("type should be -ping/-pong and self");
                System.exit(1);
            }
        } else {
            LOG.error("-ping/-pong required");
            System.exit(1);
        }

        if (Kompics.isOn()) {
            Kompics.shutdown();
        }
        Kompics.createAndStart(StunEchoLauncher.class, Runtime.getRuntime().availableProcessors(), 20); // Yes 20 is totally arbitrary
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            System.exit(1);
        }
    }
}

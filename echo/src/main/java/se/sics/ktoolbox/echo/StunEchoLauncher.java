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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
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
    public static Address pingTarget;

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
                LOG.info("{} sending PING from:{} to:{}", new Object[]{logPrefix, self, pingTarget});
                DecoratedHeader<DecoratedAddress> requestHeader = new DecoratedHeader(new BasicHeader(self, pingTarget, Transport.UDP), null, 0);
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
        networkComp = create(NettyNetwork.class, new NettyInit(self));
        trigger(Start.event, networkComp.control());
    }

    ClassMatchedHandler handlePing
            = new ClassMatchedHandler<Ping, BasicContentMsg<Address, DecoratedHeader<Address>, Ping>>() {
                @Override
                public void handle(Ping content, BasicContentMsg<Address, DecoratedHeader<Address>, Ping> container) {
                    LOG.info("{}received ping from:{}", logPrefix, container.getSource());
                    DecoratedHeader<DecoratedAddress> requestHeader = new DecoratedHeader(new BasicHeader(self, container.getSource(), Transport.UDP), null, 0);
                    ContentMsg request = new BasicContentMsg(requestHeader, new Pong());
                    trigger(request, networkComp.getPositive(Network.class));
                }
            };

    ClassMatchedHandler handlePong
            = new ClassMatchedHandler<Pong, BasicContentMsg<Address, DecoratedHeader<Address>, Pong>>() {
                @Override
                public void handle(Pong content, BasicContentMsg<Address, DecoratedHeader<Address>, Pong> container) {
                    LOG.info("{}received pong from:{} with pingSrc:{}",
                            new Object[]{logPrefix, container.getSource(), container.getSource()});
                }
            };

    public static enum Type {

        PING, PONG;
    }

    public static void main(String[] args) {
        parseArgs(args);

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

    private static void parseArgs(String[] args) {
        Options options = new Options();
        Option ping = new Option("ping", true, "ping target address");
        Option self = new Option("self", true, "public self ip or own nat address");
        options.addOption(ping);
        options.addOption(self);
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException ex) {
            LOG.error("command line parsing error");
            System.exit(1);
        }

        if (cmd.hasOption(ping.getOpt())) {
            StunEchoLauncher.type = Type.PING;
            InetAddress ip = null;
            try {
                ip = InetAddress.getByName(cmd.getOptionValue(ping.getOpt()));
            } catch (UnknownHostException ex) {
                LOG.error("ping target binding error");
                System.exit(1);
            }
            StunEchoLauncher.pingTarget = new DecoratedAddress(new BasicAddress(ip, 34544, 1)); 
        } else {
            StunEchoLauncher.type = Type.PONG;
        }
        if (cmd.hasOption(self.getOpt())) {
            InetAddress ip = null;
            try {
                ip = InetAddress.getByName(cmd.getOptionValue(self.getOpt()));
            } catch (UnknownHostException ex) {
                LOG.error("self binding error");
                System.exit(1);
            }
            switch (StunEchoLauncher.type) {
                case PING:
                    StunEchoLauncher.self = new DecoratedAddress(new BasicAddress(ip, 34543, 0));
                    break;
                case PONG:
                    StunEchoLauncher.self = new DecoratedAddress(new BasicAddress(ip, 34544, 1));
                    break;
            }
        } else {
            LOG.error("missing self address");
            System.exit(1);
        }
    }
}

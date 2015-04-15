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
package se.sics.p2ptoolbox.croupier.example.system;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import org.javatuples.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.p2ptoolbox.croupier.example.core.ExampleHostComp;
import se.sics.p2ptoolbox.croupier.example.network.ExampleSerializerSetup;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Launcher extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    private static long seed;
    private static DecoratedAddress selfAddress = null;
    private static DecoratedAddress bootstrapAddress = null;
    private static DecoratedAddress aggregatorAddress = null;

    public static void setArgs(long setSeed, Triplet<String, Integer, Integer> self, Triplet<String, Integer, Integer> bootstrap) {
        seed = setSeed;
        try {
            selfAddress = new DecoratedAddress(new BasicAddress(InetAddress.getByName(self.getValue0()), self.getValue1(), self.getValue2()));
            if (bootstrap != null) {
                bootstrapAddress = new DecoratedAddress(new BasicAddress(InetAddress.getByName(bootstrap.getValue0()), bootstrap.getValue1(), bootstrap.getValue2()));
            }
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Component timer;
    private Component network;
    private Component host;

    public Launcher() {
        log.info("initiating...");

        subscribe(handleStart, control);
        subscribe(handleStop, control);

        ExampleSerializerSetup.oneTimeSetup();

        timer = create(JavaTimer.class, Init.NONE);
        network = create(NettyNetwork.class, new NettyInit(selfAddress));
//        connect(network.getNegative(Timer.class), timer.getPositive(Timer.class));

        Set<DecoratedAddress> bootstrapNodes = new HashSet<DecoratedAddress>();
        if (bootstrapAddress != null) {
            bootstrapNodes.add(bootstrapAddress);
        }
        host = create(ExampleHostComp.class, new ExampleHostComp.HostInit(selfAddress, bootstrapNodes, seed, aggregatorAddress));
        connect(host.getNegative(Network.class), network.getPositive(Network.class));
        connect(host.getNegative(Timer.class), timer.getPositive(Timer.class));

    }

    public Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.info("starting...");
        }
    };

    public Handler<Stop> handleStop = new Handler<Stop>() {

        @Override
        public void handle(Stop event) {
            log.info("stopping...");
        }
    };
}

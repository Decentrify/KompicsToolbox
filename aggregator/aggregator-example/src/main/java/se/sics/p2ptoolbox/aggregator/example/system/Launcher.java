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
package se.sics.p2ptoolbox.aggregator.example.system;

import org.javatuples.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.*;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.aggregator.api.msg.Ready;
import se.sics.p2ptoolbox.aggregator.example.core.Application;
import se.sics.p2ptoolbox.aggregator.example.core.ApplicationPort;
import se.sics.p2ptoolbox.aggregator.example.network.ExampleSerializerSetup;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.kompics.timer.java.JavaTimer;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
* @author Alex Ormenisan <aaor@sics.se>
*/
public class Launcher extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    private static long seed = 1234;
    private static DecoratedAddress aggregatorNodeAddress;
    private static DecoratedAddress selfAddress;


    private Component timer;
    private Component peerNetwork;
    private Component applicationNetwork;
    private Component application;
    private Component host;

    public Launcher() {

        doInit();
        ExampleSerializerSetup.oneTimeSetup();
        subscribe(handleStart, control);
        subscribe(readyHandler, application.getPositive(ApplicationPort.class));
    }

    private void doInit(){

        log.info("init");
        timer = create(JavaTimer.class, Init.NONE);
        peerNetwork = create(NettyNetwork.class, new NettyInit(selfAddress));
        applicationNetwork = create(NettyNetwork.class, new NettyInit(aggregatorNodeAddress));

        application = create(Application.class, Init.NONE);

        connect(application.getNegative(Timer.class), timer.getPositive(Timer.class));
        connect(application.getNegative(Network.class), applicationNetwork.getPositive(Network.class));
    }


    public static void setArgs(long setSeed, Triplet<String, Integer, Integer> self, Triplet<String, Integer, Integer> aggregator) {

        seed = setSeed;
        try {

            if(self != null){
                selfAddress = new DecoratedAddress(new BasicAddress(InetAddress.getByName(self.getValue0()), self.getValue1(), self.getValue2()));
            }
            else{

                selfAddress = new DecoratedAddress(new BasicAddress(InetAddress.getLocalHost(), 58022, 0));
            }

            if (aggregator != null) {
                aggregatorNodeAddress = new DecoratedAddress(new BasicAddress(InetAddress.getByName(aggregator.getValue0()), aggregator.getValue1(), aggregator.getValue2()));
            }
            else{
                aggregatorNodeAddress = new DecoratedAddress(new BasicAddress(InetAddress.getLocalHost(), 54321, 0));
            }


        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.warn("Start Handler invoked !!!");
        }
    };

    public Handler<Ready> readyHandler = new Handler<Ready>() {
        @Override
        public void handle(Ready ready) {

            log.info("Received ready from the application.");

            host = create(HostManagerComp.class, new HostManagerComp.HostManagerInit(seed, selfAddress, aggregatorNodeAddress));

            connect(host.getNegative(Network.class), peerNetwork.getPositive(Network.class));
            connect(host.getNegative(Timer.class), timer.getPositive(Timer.class));

            trigger(Start.event, host.getControl());
        }
    };

}

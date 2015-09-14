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
package se.sics.ktoolbox.aggregator.local.example.main;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.ktoolbox.aggregator.local.example.system.HostComp;
import se.sics.ktoolbox.aggregator.local.example.system.HostCompInit;
import se.sics.p2ptoolbox.util.config.SystemConfig;
import se.sics.p2ptoolbox.util.helper.SystemConfigBuilder;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Launcher extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    private SystemConfig systemConfig;
    private Config config;

    private Component timer;
    private Component network;
    private Component host;

    public Launcher() {

        log.info("initiating...");

        subscribe(handleStart, control);
        subscribe(handleStop, control);
    }

    public Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {

            log.info("starting...");
            doStart();
        }
    };

    private void doStart() {

        log.debug("Loading the configuration and initiating the loading of the components.");

        config = ConfigFactory.load("application.conf");

        SystemConfigBuilder builder = new SystemConfigBuilder(config);
        systemConfig = builder.build();

        timer = create(JavaTimer.class, Init.NONE);
        network = create(NettyNetwork.class, new NettyInit(systemConfig.self));
        host = create(HostComp.class, new HostCompInit(systemConfig));

        trigger(Start.event, timer.control());
        trigger(Start.event, network.control());
        trigger(Start.event, host.control());

        connect(host.getNegative(Network.class), network.getPositive(Network.class));
        connect(host.getNegative(Timer.class), timer.getPositive(Timer.class));

        log.debug("Triggered all the components ..");
    }


    public Handler<Stop> handleStop = new Handler<Stop>() {

        @Override
        public void handle(Stop event) {
            log.info("stopping...");
        }
    };
}

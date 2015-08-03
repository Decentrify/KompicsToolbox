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
package se.sics.ktoolbox.cc.heartbeat.example.system;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.caracaldb.Address;
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
import se.sics.ktoolbox.cc.bootstrap.CCBootstrapComp;
import se.sics.ktoolbox.cc.bootstrap.CCBootstrapPort;
import se.sics.ktoolbox.cc.common.config.CaracalClientConfig;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatComp;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatPort;
import se.sics.ktoolbox.cc.heartbeat.example.config.BootstrapNodes;
import se.sics.ktoolbox.cc.heartbeat.example.config.CaracalClientSerializerSetup;
import se.sics.ktoolbox.cc.heartbeat.example.core.ExampleComp;
import se.sics.p2ptoolbox.util.config.SystemConfig;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Launcher extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    private Component timer;
    private Component network;
    private Component ccBootstrap;
    private Component ccHearbeat;
    private Component example;

    public Launcher() {
        log.info("initiating...");

        subscribe(handleStart, control);
        subscribe(handleStop, control);

        CaracalClientSerializerSetup.registerSerializers();

        timer = create(JavaTimer.class, Init.NONE);
        Config config = ConfigFactory.load();
        SystemConfig systemConfig = new SystemConfig(config);
        Address ccSelf = new Address(systemConfig.self.getIp(), systemConfig.self.getPort(), null);
        
        network = create(NettyNetwork.class, new NettyInit(ccSelf));

        CaracalClientConfig ccConfig = new CaracalClientConfig(config);
        ccBootstrap = create(CCBootstrapComp.class, new CCBootstrapComp.CCBootstrapInit(systemConfig, ccConfig, BootstrapNodes.readCaracalBootstrap(config)));
        connect(ccBootstrap.getNegative(Network.class), network.getPositive(Network.class));
        connect(ccBootstrap.getNegative(Timer.class), timer.getPositive(Timer.class));

        ccHearbeat = create(CCHeartbeatComp.class, new CCHeartbeatComp.CCHeartbeatInit(systemConfig, ccConfig));
        connect(ccHearbeat.getNegative(Timer.class), timer.getPositive(Timer.class));
        connect(ccHearbeat.getNegative(CCBootstrapPort.class), ccBootstrap.getPositive(CCBootstrapPort.class));

        example = create(ExampleComp.class, new ExampleComp.ExampleInit(new SystemConfig(config), new byte[]{1, 2, 3}, new byte[]{1, 2, 4}));
        connect(example.getNegative(Timer.class), timer.getPositive(Timer.class));
        connect(example.getNegative(CCHeartbeatPort.class), ccHearbeat.getPositive(CCHeartbeatPort.class));
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

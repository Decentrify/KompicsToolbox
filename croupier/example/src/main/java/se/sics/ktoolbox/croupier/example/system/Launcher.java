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
package se.sics.ktoolbox.croupier.example.system;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.p2ptoolbox.croupier.CroupierSerializerSetup;
import se.sics.ktoolbox.croupier.example.core.ExampleHostComp;
import se.sics.ktoolbox.croupier.example.core.ExampleHostKConfig;
import se.sics.ktoolbox.croupier.example.network.ExampleSerializerSetup;
import se.sics.p2ptoolbox.util.config.KConfigCache;
import se.sics.p2ptoolbox.util.config.KConfigCore;
import se.sics.p2ptoolbox.util.nat.NatedTrait;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.proxy.SystemHookSetup;
import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup;
import se.sics.p2ptoolbox.util.traits.AcceptedTraits;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Launcher extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    private Component timer;
    private Component network;
    private Component host;

    public Launcher() {
        log.info("initiating...");

        Config config = ConfigFactory.load();
        KConfigCore configCore = new KConfigCore(config);
        KConfigCache configCache = new KConfigCache(configCore);
        SystemHookSetup systemHooks = new SystemHookSetup();
        systemSetup(configCore, systemHooks);

        timer = create(JavaTimer.class, Init.NONE);
        network = create(NettyNetwork.class, new NettyInit(configCache.read(ExampleHostKConfig.self).get()));
        host = create(ExampleHostComp.class, new ExampleHostComp.HostInit(configCore));
        connect(host.getNegative(Network.class), network.getPositive(Network.class));
        connect(host.getNegative(Timer.class), timer.getPositive(Timer.class));
    }
    
    private void systemSetup(KConfigCore config, SystemHookSetup systemHooks) {
        //address setup
        ImmutableMap acceptedTraits = ImmutableMap.of(NatedTrait.class, 0);
        DecoratedAddress.setAcceptedTraits(new AcceptedTraits(acceptedTraits));

        //serializers setup
        int serializerId = 128;
        serializerId = BasicSerializerSetup.registerBasicSerializers(serializerId);
        serializerId = CroupierSerializerSetup.registerSerializers(serializerId);
        serializerId = ExampleSerializerSetup.registerSerializers(serializerId);

        if (serializerId > 255) {
            throw new RuntimeException("switch to bigger serializerIds, last serializerId:" + serializerId);
        }

        //hooks setup
        //no hooks needed
    }
}
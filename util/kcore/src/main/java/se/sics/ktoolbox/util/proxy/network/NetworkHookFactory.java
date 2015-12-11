/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * NatTraverser is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.util.proxy.network;

import java.util.HashSet;
import java.util.Set;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Handler;
import se.sics.kompics.Kill;
import se.sics.kompics.Killed;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.UniDirectionalChannel;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.ktoolbox.util.proxy.network.NetworkHook.*;
import se.sics.ktoolbox.util.connect.ConnectionHelperComp;
import se.sics.ktoolbox.util.connect.ConnectionHelperComp.ConnectionHelperInit;
import se.sics.ktoolbox.util.selectors.DestinationPortSelector;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NetworkHookFactory {

    public static Definition getNettyNetwork() {
        return new Definition() {

            @Override
            public SetupResult setup(ComponentProxy proxy, final Parent parent, final SetupInit setupInit) {
                Component[] components = new Component[1];
                Channel[][] channels = new Channel[0][];
                components[0] = proxy.create(NettyNetwork.class, new NettyInit(setupInit.bindAdr));
                return new SetupResult(setupInit.id, components[0].getPositive(Network.class), components, channels);
            }

            @Override
            public void start(ComponentProxy proxy, Parent parent, SetupResult setup, StartInit startInit) {
                if (!startInit.started) {
                    proxy.trigger(Start.event, setup.comp[0].control());
                }
                parent.on(setup.id);
            }

            @Override
            public void tearDown(ComponentProxy proxy, final Parent parent, final SetupResult setup, TearInit hookTear) {
                if (!hookTear.killed) {
                    Handler handleKilled = new Handler<Killed>() {
                        @Override
                        public void handle(Killed event) {
                            parent.off(setup.id);
                        }
                    };
                    proxy.subscribe(handleKilled, setup.comp[0].control());
                    proxy.trigger(Kill.event, setup.comp[0].control());
                } else {
                    parent.off(setup.id);
                }
            }

        };
    }

    public static Definition getNetworkEmulator(final Positive<Network> network) {
        return new Definition() {

            @Override
            public SetupResult setup(ComponentProxy proxy, Parent hookParent, SetupInit hookInit) {
                Component[] components = new Component[1];
                Channel[][] channels = new Channel[1][];

                Set<Class> proxyPorts = new HashSet<>();
                proxyPorts.add(Network.class);
                components[0] = proxy.create(ConnectionHelperComp.class, new ConnectionHelperInit(proxyPorts));

                channels[0] = new Channel[1];
                channels[0][0] = proxy.connect(network, components[0].getNegative(Network.class),
                        new DestinationPortSelector(hookInit.bindAdr.getPort(), false), UniDirectionalChannel.ONE_WAY_POS);
                channels[0][1] = proxy.connect(network, components[0].getNegative(Network.class),
                        UniDirectionalChannel.ONE_WAY_NEG);
                return new SetupResult(hookInit.id, components[0].getPositive(Network.class), components, channels);
            }

            @Override
            public void start(ComponentProxy proxy, Parent hookParent,
                    SetupResult setup, StartInit startInit) {
                if (!startInit.started) {
                    proxy.trigger(Start.event, setup.comp[0].control());
                }
                hookParent.on(setup.id);
            }

            @Override
            public void tearDown(ComponentProxy proxy, Parent hookParent,
                    SetupResult setup, TearInit tearInit) {
                if (!tearInit.killed) {
                    proxy.trigger(Kill.event, setup.comp[0].control());
                }
                hookParent.off(setup.id);
            }
        };
    }
}

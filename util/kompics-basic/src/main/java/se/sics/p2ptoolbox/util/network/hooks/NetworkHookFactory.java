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
package se.sics.p2ptoolbox.util.network.hooks;

import java.util.HashSet;
import java.util.Set;
import se.sics.kompics.Component;
import se.sics.kompics.Kill;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.p2ptoolbox.util.network.hooks.NetworkHook.*;
import se.sics.p2ptoolbox.util.other.ConnectionHelperComp;
import se.sics.p2ptoolbox.util.other.ConnectionHelperComp.ConnectionHelperInit;
import se.sics.p2ptoolbox.util.proxy.ComponentProxy;
import se.sics.p2ptoolbox.util.truefilters.DestinationPortFilter;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NetworkHookFactory {

    public static Definition getNettyNetwork() {
        return new Definition() {

            @Override
            public SetupResult setup(ComponentProxy proxy, final Parent hookParent,
                    final SetupInit setupInit) {
                Component[] comp = new Component[1];
                if (setupInit.alternateBind.isPresent()) {
                    System.setProperty("altBindIf", setupInit.alternateBind.get().getCanonicalHostName());
                }
                comp[0] = proxy.create(NettyNetwork.class, new NettyInit(setupInit.self));
                if (setupInit.alternateBind.isPresent()) {
                    System.clearProperty("altBindIf");
                }
                return new SetupResult(comp);
            }

            @Override
            public void start(ComponentProxy proxy, Parent hookParent,
                    SetupResult setupResult, StartInit startInit) {
                if(!startInit.started) {
                    proxy.trigger(Start.event, setupResult.comp[0].control());
                }
                hookParent.onResult(new NetworkResult(setupResult.comp[0].getPositive(Network.class)));
            }

            @Override
            public void preStop(ComponentProxy proxy, Parent hookParnt,
                    SetupResult setupResult, TearInit hookTear) {
                proxy.trigger(Kill.event, setupResult.comp[0].control());
            }
        };
    }

    public static Definition getNetworkEmulator(final Positive<Network> network) {
        return new Definition() {

            @Override
            public SetupResult setup(ComponentProxy proxy, Parent hookParent,
                    SetupInit hookInit) {
                Component[] comp = new Component[1];
                Set<Class> proxyPorts = new HashSet<>();
                proxyPorts.add(Network.class);
                comp[0] = proxy.create(ConnectionHelperComp.class, new ConnectionHelperInit(proxyPorts));
                proxy.connect(comp[0].getNegative(Network.class), network, new DestinationPortFilter(hookInit.self.getPort(), true));
                return new SetupResult(comp);
            }

            @Override
            public void start(ComponentProxy proxy, Parent hookParent,
                    SetupResult setupResult, StartInit startInit) {
                if(!startInit.started) {
                    proxy.trigger(Start.event, setupResult.comp[0].control());
                }
                hookParent.onResult(new NetworkResult(setupResult.comp[0].getPositive(Network.class)));
            }

            @Override
            public void preStop(ComponentProxy proxy, Parent hookParent,
                    SetupResult setupResult, TearInit hookTear) {
            }
        };
    }
}

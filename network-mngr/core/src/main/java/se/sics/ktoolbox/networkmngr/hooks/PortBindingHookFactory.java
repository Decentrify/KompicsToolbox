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
package se.sics.ktoolbox.networkmngr.hooks;

import com.google.common.base.Optional;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import se.sics.kompics.Component;
import se.sics.ktoolbox.networkmngr.hooks.PortBindingHook.*;
import se.sics.p2ptoolbox.util.proxy.ComponentProxy;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class PortBindingHookFactory {

    public static Definition getPortBinder() {
        return new Definition() {

            @Override
            public SetupResult setup(ComponentProxy proxy, final Parent hookParent,
                    final SetupInit setupInit) {
                return new SetupResult(new Component[0]);
            }

            @Override
            public void start(ComponentProxy proxy, Parent hookParent,
                    SetupResult setupResult, StartInit startInit) {
                Integer boundPort = bind(hookParent, startInit.localIp, startInit.tryPort);
                if (startInit.forceProvidedPort && boundPort != startInit.tryPort) {
                    throw new RuntimeException("could not bind on requested port:" + startInit.tryPort);
                }
                hookParent.onResult(new PortBindingResult(startInit.localIp, startInit.tryPort, boundPort));
            }

            @Override
            public void preStop(ComponentProxy proxy, Parent hookParnt,
                    SetupResult setupResult, TearInit hookTear) {
            }

            private Integer bind(Parent hookParent, InetAddress localIp, Integer tryPort) {
                try (Socket socket = new Socket()) {
                    socket.setReuseAddress(true);
                    socket.bind(new InetSocketAddress(localIp, tryPort));
                    return tryPort;
                } catch (BindException e) {
                    //continue and get a random port
                } catch (Exception e) {
                    throw new RuntimeException("bind problem", e);
                }
                try (Socket socket = new Socket()) {
                    socket.setReuseAddress(true);
                    socket.bind(new InetSocketAddress(localIp, 0));
                    return socket.getLocalPort();
                } catch (Exception e) {
                    throw new RuntimeException("bind problem", e);
                }
            }
        };
    }

    public static Definition getPortBinderEmulator() {
        return new Definition() {

            @Override
            public SetupResult setup(ComponentProxy proxy, Parent hookParent,
                    SetupInit hookInit) {
                return new SetupResult(new Component[0]);
            }

            @Override
            public void start(ComponentProxy proxy, Parent hookParent,
                    SetupResult setupResult, StartInit startInit) {
                hookParent.onResult(new PortBindingResult(startInit.localIp, startInit.tryPort, startInit.tryPort));
            }

            @Override
            public void preStop(ComponentProxy proxy, Parent hookParent,
                    SetupResult setupResult, TearInit hookTear) {
            }
        };
    }
}

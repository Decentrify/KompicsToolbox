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
package se.sics.ktoolbox.ipsolver.newhooks;

import com.google.common.base.Optional;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import se.sics.kompics.Component;
import se.sics.kompics.Handler;
import se.sics.kompics.Start;
import se.sics.ktoolbox.ipsolver.IpSolverComp;
import se.sics.ktoolbox.ipsolver.IpSolverPort;
import se.sics.ktoolbox.ipsolver.msg.GetIp;
import se.sics.ktoolbox.ipsolver.util.IpAddressStatus;
import se.sics.ktoolbox.ipsolver.newhooks.AddressSolverHook.*;
import se.sics.p2ptoolbox.util.proxy.ComponentProxy;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class AddressSolverHookFactory {

    public static Definition getIpSolver() {
        return new Definition() {

            @Override
            public SetupResult setup(ComponentProxy proxy, final Parent hookParent,
                    final SetupInit setupInit) {
                Component[] comp = new Component[1];
                comp[0] = proxy.create(IpSolverComp.class, new IpSolverComp.IpSolverInit());

                Handler handleGetIp = new Handler<GetIp.Resp>() {
                    @Override
                    public void handle(GetIp.Resp resp) {
                        if (!resp.addrs.isEmpty()) {
                            Iterator<IpAddressStatus> it = resp.addrs.iterator();
                            Optional<InetAddress> localIp;
                            Optional<String> sPrefferedLocalIp = hookParent.preferedLocalIp();
                            if (sPrefferedLocalIp.isPresent()) {
                                InetAddress prefferedLocalIp;
                                try {
                                    prefferedLocalIp = InetAddress.getByName(sPrefferedLocalIp.get());
                                } catch (UnknownHostException ex) {
                                    throw new RuntimeException("ip problem");
                                }
                                while (it.hasNext()) {
                                    IpAddressStatus next = it.next();
                                    if (next.getAddr().equals(prefferedLocalIp)) {
                                        hookParent.onResult(new AddressSolverResult(prefferedLocalIp));
                                        return;
                                    }
                                }
                            }
                            throw new RuntimeException("incomplete... todo");
                        } else {
                            throw new RuntimeException("no local interfaces detected");
                        }
                    }
                };
                proxy.subscribe(handleGetIp, comp[0].getPositive(IpSolverPort.class));

                return new SetupResult(comp);
            }

            @Override
            public void start(ComponentProxy proxy, Parent hookParent,
                    SetupResult setupResult, StartInit startInit) {
                if (!startInit.started) {
                    proxy.trigger(Start.event, setupResult.comp[0].control());
                }
                proxy.trigger(new GetIp.Req(hookParent.netInterfaces()), setupResult.comp[0].getPositive(IpSolverPort.class));
            }

            @Override
            public void preStop(ComponentProxy proxy, Parent hookParnt,
                    SetupResult setupResult, TearInit hookTear) {
            }
        };
    }

    public static Definition getIpSolverEmulator() {
        return new Definition() {

            @Override
            public SetupResult setup(ComponentProxy proxy, Parent hookParent,
                    SetupInit hookInit) {
                return new AddressSolverHook.SetupResult(new Component[0]);
            }

            @Override
            public void start(ComponentProxy proxy, Parent hookParent,
                    SetupResult setupResult, StartInit startInit) {
                Optional<String> preferredLocalIp = hookParent.preferedLocalIp();
                if (!preferredLocalIp.isPresent()) {
                    throw new RuntimeException("simulation - missing ip");
                }
                InetAddress localIp;
                try {
                    localIp = InetAddress.getByName(preferredLocalIp.get());
                } catch (UnknownHostException ex) {
                    throw new RuntimeException("simulation - ip host could not be determined");
                }
                hookParent.onResult(new AddressSolverResult(localIp));
            }

            @Override
            public void preStop(ComponentProxy proxy, Parent hookParent,
                    SetupResult setupResult, TearInit hookTear) {
            }
        };
    }
}

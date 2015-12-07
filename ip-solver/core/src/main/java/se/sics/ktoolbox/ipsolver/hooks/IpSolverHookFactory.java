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
package se.sics.ktoolbox.ipsolver.hooks;

import com.google.common.base.Optional;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentProxy;
import se.sics.ktoolbox.ipsolver.IpSolver;
import se.sics.ktoolbox.ipsolver.hooks.IpSolverHook.*;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class IpSolverHookFactory {

    public static Definition getIpSolver() {
        return new Definition() {

            @Override
            public SetupResult setup(ComponentProxy proxy, final Parent hookParent,
                    final SetupInit setupInit) {
                return new SetupResult(new Component[0]);
            }

            @Override
            public void start(ComponentProxy proxy, Parent hookParent,
                    SetupResult setupResult, StartInit startInit) {
                Optional<InetAddress> ip;
                try {
                    ip = IpSolver.getIp(startInit.prefferedIp, startInit.prefferedInterfaces);
                } catch (SocketException ex) {
                    throw new RuntimeException(ex);
                }
                hookParent.onResult(new IpSolverResult(ip));
            }

            @Override
            public void tearDown(ComponentProxy proxy, Parent hookParnt,
                    SetupResult setupResult, TearInit hookTear) {
            }
        };
    }
    
    public static Definition getIpSolverEmulator() {
        return new Definition() {

            @Override
            public SetupResult setup(ComponentProxy proxy, Parent hookParent,
                    SetupInit hookInit) {
                return new IpSolverHook.SetupResult(new Component[0]);
            }

            @Override
            public void start(ComponentProxy proxy, Parent hookParent,
                    SetupResult setupResult, StartInit startInit) {
                if (!startInit.prefferedIp.isPresent()) {
                    throw new RuntimeException("simulation - missing ip");
                }
                InetAddress localIp;
                try {
                    localIp = InetAddress.getByName(startInit.prefferedIp.get());
                } catch (UnknownHostException ex) {
                    throw new RuntimeException("simulation - ip host could not be determined");
                }
                hookParent.onResult(new IpSolverResult(Optional.of(localIp)));
            }

            @Override
            public void tearDown(ComponentProxy proxy, Parent hookParent,
                    SetupResult setupResult, TearInit hookTear) {
            }
        };
    }
}

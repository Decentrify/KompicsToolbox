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
package se.sics.ktoolbox.netmngr.ipsolver.hooks;

import com.google.common.base.Optional;
import java.util.List;
import se.sics.kompics.Component;
import se.sics.ktoolbox.netmngr.ipsolver.IpSolve;
import se.sics.ktoolbox.util.proxy.Hook;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class IpSolverHook {

    public static interface Parent extends Hook.Parent {
        public void onResult(IpSolverResult result);
    }

    public static interface Definition extends Hook.Definition<Parent, SetupInit, SetupResult, StartInit, TearInit> {
    }

    public static class SetupInit implements Hook.SetupInit {
    }

    public static class SetupResult implements Hook.SetupResult {
        public final Component[] comp;

        public SetupResult(Component[] comp) {
            this.comp = comp;
        }
    }

    public static class StartInit extends Hook.StartInit {
        public final Optional<String> prefferedIp;
        public final List<IpSolve.NetworkInterfacesMask> prefferedInterfaces;
        public StartInit(boolean started, Optional<String> prefferedIp, List<IpSolve.NetworkInterfacesMask> prefferedInterfaces) {
            super(started);
            this.prefferedIp = prefferedIp;
            this.prefferedInterfaces = prefferedInterfaces;
        }
    }

    public static class TearInit extends Hook.TearInit {
        public TearInit(boolean killed) {
            super(killed);
        }
    }
}

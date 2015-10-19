/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * KompicsToolbox is free software; you can redistribute it and/or
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
import java.net.InetAddress;
import se.sics.kompics.Component;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Network;
import se.sics.p2ptoolbox.util.proxy.Hook;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NetworkHook {
    public static interface Parent extends Hook.Parent {
        public void onResult(NetworkResult result);
    }

    public static interface Definition extends Hook.Definition<Parent, SetupInit, SetupResult, StartInit, TearInit> {
    }

    public static class SetupInit implements Hook.SetupInit {
        public final Address self;
        public final Optional<InetAddress> alternateBind;
        
        public SetupInit(Address self, Optional<InetAddress> alternateBind) {
            this.self = self;
            this.alternateBind = alternateBind;
        }
    }

    public static class SetupResult implements Hook.SetupResult {

        public final Component[] comp;

        public SetupResult(Component[] comp) {
            this.comp = comp;
        }
    }

    public static class StartInit extends Hook.StartInit {
        public StartInit(boolean started) {
            super(started);
        }
    }

    public static class TearInit implements Hook.TearInit {
    }
}
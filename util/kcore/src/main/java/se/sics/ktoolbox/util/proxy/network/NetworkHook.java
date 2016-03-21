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

package se.sics.ktoolbox.util.proxy.network;

import java.util.UUID;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.proxy.Hook;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NetworkHook {
    public static interface Parent extends Hook.Parent {
        public void on(UUID id);
        public void off(UUID id);
    }

    public static interface Definition extends Hook.Definition<Parent, SetupInit, SetupResult, StartInit, TearInit> {
    }

    public static class SetupInit implements Hook.SetupInit {
        public final UUID id;
        public final KAddress bindAdr;
        
        public SetupInit(UUID id, KAddress bindAdr) {
            this.id = id;
            this.bindAdr = bindAdr;
        }
    }

    public static class SetupResult implements Hook.SetupResult {
        public final UUID id;
        public final Positive<Network> network;
        public final Component[] comp;
        public final Channel[][] channels;

        public SetupResult(UUID id, Positive<Network> network, Component[] comp, Channel[][] channels) {
            this.id = id;
            this.network = network;
            this.comp = comp;
            this.channels = channels;
        }
    }

    public static class StartInit extends Hook.StartInit {
        public StartInit(boolean started) {
            super(started);
        }
    }

    public static class TearInit extends Hook.TearInit {
        public TearInit(boolean killed) {
            super(killed);
        }
    }
}

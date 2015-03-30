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
package se.sics.p2ptoolbox.simulator.core.network.impl;

import java.util.Set;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Msg;
import se.sics.p2ptoolbox.simulator.core.network.NetworkModel;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class DisconnectedNodesNetworkModel implements NetworkModel {

    private int id;
    private final NetworkModel baseNM;
    private final Set<Address> disconnectedNodes;

    public DisconnectedNodesNetworkModel(int id, NetworkModel baseNM, Set<Address> disconnectedNodes) {
        this.id = id;
        this.baseNM = baseNM;
        this.disconnectedNodes = disconnectedNodes;
    }

    @Override
    public long getLatencyMs(Msg message) {
        if (disconnectedNodes.contains(message.getHeader().getSource()) || disconnectedNodes.contains(message.getHeader().getDestination()))  {
            return -1;
        }
        return baseNM.getLatencyMs(message);
    }

    @Override
    public String toString() {
        return "DisconnectedNodes NetworkModel<" + id + ">";
    }
}

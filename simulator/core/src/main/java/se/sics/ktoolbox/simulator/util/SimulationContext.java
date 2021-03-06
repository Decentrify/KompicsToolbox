package se.sics.ktoolbox.simulator.util;

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;
import se.sics.kompics.network.Address;
import se.sics.ktoolbox.util.address.NatAwareAddress;
import se.sics.ktoolbox.util.address.nat.NatType;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimulationContext {

    private final Random rand;
    private final Map<String, Object> otherContext = new HashMap<>();

    public SimulationContext(Random rand) {
        this.rand = rand;
    }

    public Random getRand() {
        return rand;
    }

    /**
     * @param identifier
     * @param obj
     * @return false if registration could not happen. Possible causes: 1. there
     * is already an object registered with that identifier
     */
    public boolean register(String identifier, Object obj) {
        if (otherContext.containsKey(identifier)) {
            return false;
        }
        otherContext.put(identifier, obj);
        return true;
    }

    public Object get(String identifier) {
        return otherContext.get(identifier);
    }
}

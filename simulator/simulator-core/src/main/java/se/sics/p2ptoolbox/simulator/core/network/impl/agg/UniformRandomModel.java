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
package se.sics.p2ptoolbox.simulator.core.network.impl.agg;

import se.sics.p2ptoolbox.simulator.core.network.impl.*;
import java.util.Random;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Msg;
import se.sics.p2ptoolbox.simulator.core.network.NetworkModel;
import se.sics.p2ptoolbox.util.identifiable.IntegerIdentifiable;

/**
 * @author Lars Kroll <lkroll@sics.se>
 */
public class UniformRandomModel implements NetworkModel {

    private final long min;
    private final long max;
    private final long diff;
    private final Random rand;

    public UniformRandomModel(long min, long max) {
        this(min, max, new Random(1));
    }

    public UniformRandomModel(long min, long max, Random rand) {
        this.min = min;
        this.max = max;
        this.diff = max - min;
        this.rand = rand;
    }

    @Override
    public long getLatencyMs(Msg message) {
        Address src = message.getHeader().getSource();
        Address dst = message.getHeader().getDestination();

        if (!(src instanceof IntegerIdentifiable) || !(dst instanceof IntegerIdentifiable)) {
            throw new RuntimeException("used addresses are not identifiable - cannot used DeadLinkNetworkModel with these addresses");
        }
        if (((IntegerIdentifiable) dst).getId() == 0) {
            return 1;
        }
        return min + (long) Math.floor(rand.nextDouble() * diff);
    }

}

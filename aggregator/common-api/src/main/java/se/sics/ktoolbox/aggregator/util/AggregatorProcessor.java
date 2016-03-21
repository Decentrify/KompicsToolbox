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
package se.sics.ktoolbox.aggregator.util;

import com.google.common.base.Optional;

/**
 * Marker interface for the processor responsible for the data conversion at
 * different stages of the aggregation.
 *
 * Created by babbar on 2015-09-04.
 */
public interface AggregatorProcessor<P_IN extends AggregatorPacket, P_OUT extends AggregatorPacket> {
    /**
     * @param previous
     * @param current
     * @return next
     */
    public P_OUT process(Optional<P_OUT> previous, P_IN current);
    public Class<P_OUT> getAggregatedType();
}

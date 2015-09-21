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
package se.sics.ktoolbox.aggregator.server.util;

import se.sics.ktoolbox.aggregator.server.event.AggregatedInfo;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import se.sics.ktoolbox.aggregator.util.PacketInfo;
import se.sics.ktoolbox.aggregator.util.Processor;

/**
 * Design Processor indicates the use of the processor for creating
 * visualizations in the system.
 *
 * @param <PI_I> Packet Information. ( Only to enforce compile time check. )
 * @param <DI_O> Design Information.
 */
public interface DesignProcessor < PI_I extends PacketInfo, DI_O extends DesignInfo > extends Processor {


    /**
     * Process the windows being buffered by the visualization window.
     * After processing return instance of the  processed windows.
     *
     * @param windows : windows are the snapshots of the systems at different intervals.
     *
     * @return collection of processed windows.
     */
    public DesignInfoContainer<DI_O> process(List<AggregatedInfo> windows);


    /**
     * An explicit command indicating the designer to clean the
     * internal state which might still be held in regard to the
     * previous processing.
     *
     */
    public void cleanState();
}

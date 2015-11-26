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
package se.sics.ktoolbox.aggregator.msg;

import java.util.Map;
import java.util.UUID;
import se.sics.ktoolbox.aggregator.event.AggregatorEvent;
import se.sics.ktoolbox.aggregator.util.AggregatorPacket;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NodeWindow implements AggregatorEvent {
    public final UUID id;
    public final Map<Class, AggregatorPacket> window;
    
    public NodeWindow(UUID id, Map<Class, AggregatorPacket> window) {
        this.id = id;
        this.window = window;
    }
    
    @Override
    public UUID getId() {
        return id;
    }
    
    @Override
    public String toString() {
        return "AGG_NODE_WINDOW<" + id + ">";
    }
}

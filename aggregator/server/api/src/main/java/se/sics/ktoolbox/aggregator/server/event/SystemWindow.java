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
package se.sics.ktoolbox.aggregator.server.event;

import com.google.common.collect.Table;
import se.sics.ktoolbox.aggregator.event.AggregatorEvent;
import se.sics.ktoolbox.aggregator.util.AggregatorPacket;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.Identifier;


/**
 * Event from the aggregator indicating the
 * aggregated information from the nodes in the system.
 *
 * Created by babbarshaer on 2015-09-02.
 */
public class SystemWindow implements AggregatorEvent {
    public final Identifier eventId;
    public final Table<Identifier, Class, AggregatorPacket> systemWindow;

    public SystemWindow(Identifier eventId, Table<Identifier, Class, AggregatorPacket> systemWindow){
        this.eventId = eventId;
        this.systemWindow = systemWindow;
    }
    
    public SystemWindow(Table<Identifier, Class, AggregatorPacket> systemWindow){
        this(BasicIdentifiers.eventId(), systemWindow);
    }

    @Override
    public String toString() {
        return getClass() + "<" + eventId + ">";
    }

    @Override
    public Identifier getId() {
        return eventId;
    }
}

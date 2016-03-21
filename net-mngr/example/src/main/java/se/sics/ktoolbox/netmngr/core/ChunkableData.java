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
package se.sics.ktoolbox.netmngr.core;

import java.nio.ByteBuffer;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;
import se.sics.ktoolbox.util.network.basic.ContentPattern;
import se.sics.ktoolbox.util.network.other.Chunkable;
import se.sics.ktoolbox.util.overlays.OverlayEvent;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ChunkableData implements OverlayEvent, Chunkable, ContentPattern<Chunkable> {
    public final Identifier eventId;
    public final Identifier overlayId;
    public final int counter;
    public final ByteBuffer data;
    
    public ChunkableData(Identifier eventId, Identifier overlayId, int counter, ByteBuffer data) {
        this.eventId = eventId;
        this.overlayId = overlayId;
        this.counter = counter;
        this.data = data;
    }
    
    public ChunkableData(Identifier overlayId, int counter, ByteBuffer data) {
        this(UUIDIdentifier.randomId(), overlayId, counter, data);
    }
            
    @Override
    public Identifier overlayId() {
        return overlayId;
    }

    @Override
    public Identifier getId() {
        return eventId;
    }

    @Override
    public String toString() {
        return "ChunkableData<" + overlayId + ", " + eventId + "> ";
    }
    
    public Ack answer() {
        return new Ack(eventId, overlayId, counter);
    }

    @Override
    public Class<Chunkable> extractPattern() {
        return Chunkable.class;
    }
}

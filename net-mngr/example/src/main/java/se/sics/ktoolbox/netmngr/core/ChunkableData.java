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
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.network.basic.ContentPattern;
import se.sics.ktoolbox.util.network.other.Chunkable;
import se.sics.ktoolbox.util.overlays.OverlayEvent;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ChunkableData implements OverlayEvent, Chunkable, ContentPattern<Chunkable> {
    public final Identifier msgId;
    public final OverlayId overlayId;
    public final int counter;
    public final ByteBuffer data;
    
    public ChunkableData(Identifier msgId, OverlayId overlayId, int counter, ByteBuffer data) {
        this.msgId = msgId;
        this.overlayId = overlayId;
        this.counter = counter;
        this.data = data;
    }
    
    public ChunkableData(OverlayId overlayId, int counter, ByteBuffer data) {
        this(BasicIdentifiers.msgId(), overlayId, counter, data);
    }
            
    @Override
    public OverlayId overlayId() {
        return overlayId;
    }

    @Override
    public Identifier getId() {
        return msgId;
    }

    @Override
    public String toString() {
        return "ChunkableData<" + overlayId + ", " + msgId + "> ";
    }
    
    public Ack answer() {
        return new Ack(msgId, overlayId, counter);
    }

    @Override
    public Class<Chunkable> extractPattern() {
        return Chunkable.class;
    }
}

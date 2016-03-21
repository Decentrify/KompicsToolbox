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

package se.sics.ktoolbox.netmngr.chunk.util;

import se.sics.ktoolbox.netmngr.chunk.Chunk;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class IncompleteChunkTracker {
    public final Map<Integer, Chunk> chunks;
    public final int nrChunks;
    
    public IncompleteChunkTracker(int lastChunk) {
        this.chunks = new HashMap<>();
        this.nrChunks = lastChunk + 1;
    }
    
    public void add(Chunk chunk) {
        chunks.put(chunk.chunkNr, chunk);
    }
    
    public boolean isComplete() {
        return chunks.size() == nrChunks;
    }
    
    public byte[] getMsg() {
        ByteBuf buf = Unpooled.buffer();
        for(Chunk chunk : chunks.values()) {
            buf.writeBytes(chunk.content);
        }
        byte[] result = new byte[buf.readableBytes()];
        buf.readBytes(result);
        return result;
    }
}

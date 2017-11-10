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

package se.sics.ktoolbox.netmngr.chunk;

import se.sics.kompics.util.Identifier;

/**
 * author Alex Ormenisan <aaor@sics.se>
 */
public class Chunk {
    public final Identifier originId;
    public final int chunkNr;
    public final int lastChunk;
    public final byte[] content;

    public Chunk(Identifier OriginId, int chunkNr, int lastChunk, byte[] content) {
        this.originId = OriginId;
        this.chunkNr = chunkNr;
        this.lastChunk = lastChunk;
        this.content = content;
        if(lastChunk > 127) {
            throw new RuntimeException("if your messages are bigger than 128 UDP chunks - you are doing something wrong");
        }
    }
    
    @Override
    public String toString() {
        return "Chunk<"+ chunkNr + ", "+ originId + ">";
    }
}
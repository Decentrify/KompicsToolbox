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

package se.sics.p2ptoolbox.chunkmanager.util;

import java.util.Arrays;
import java.util.UUID;

/**
 * author Alex Ormenisan <aaor@sics.se>
 */
public class Chunk {
    public final UUID messageId;
    public final int chunkNr;
    public final int lastChunk;
    public final byte[] content;

    public Chunk(UUID messageId, int chunkNr, int lastChunk, byte[] content) {
        this.messageId = messageId;
        this.chunkNr = chunkNr;
        this.lastChunk = lastChunk;
        this.content = content;
        if(lastChunk > 127) {
            throw new RuntimeException("if your messages is bigger than 128 UDP chunks - you are doing something wrong");
        }
    }
    
    @Override
    public String toString() {
        return "Chunk<" + messageId + ", "+ chunkNr + ">";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + (this.messageId != null ? this.messageId.hashCode() : 0);
        hash = 47 * hash + this.chunkNr;
        hash = 47 * hash + this.lastChunk;
        hash = 47 * hash + Arrays.hashCode(this.content);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Chunk other = (Chunk) obj;
        if (this.messageId != other.messageId && (this.messageId == null || !this.messageId.equals(other.messageId))) {
            return false;
        }
        if (this.chunkNr != other.chunkNr) {
            return false;
        }
        if (this.lastChunk != other.lastChunk) {
            return false;
        }
        if (!Arrays.equals(this.content, other.content)) {
            return false;
        }
        return true;
    }
}
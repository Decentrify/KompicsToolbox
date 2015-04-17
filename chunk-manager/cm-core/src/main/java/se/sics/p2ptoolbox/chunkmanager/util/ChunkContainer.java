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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by alidar on 10/21/14.
 */
public class ChunkContainer {

    private UUID messageID;
    private int totalExpectedChunks;
    private HashMap<Integer, Chunk> chunks = new HashMap<Integer, Chunk>();
    private UUID timeoutId;

    public ChunkContainer(UUID messageID, int totalExpectedChunks) {

        this.messageID = messageID;
        this.totalExpectedChunks = totalExpectedChunks;
    }

    private boolean isDuplicateChunk(Chunk chunk) {

        Chunk oldChunk = chunks.get(chunk.getChunkID());
        return oldChunk != null;
    }

    public void addChunk(Chunk chunk) throws Exception {

        if(isDuplicateChunk(chunk))
            return;

        if(chunk.getChunkID() >= totalExpectedChunks)
            throw new Exception("Received invalid chunk. " +
                    "The chunkID is greater than the ID of the last expected chunk");

        chunks.put(chunk.getChunkID(), chunk);
    }

    public boolean isComplete()
    {
        if (totalExpectedChunks == chunks.size())
            return true;

        return false;
    }

    public ByteBuf getCombinedBytesOfChunks() throws IOException {

        if(!isComplete())
            return null;

        ByteBuf byteBuf = Unpooled.buffer();

        for(int i = 0; i < totalExpectedChunks; i++) {
            Chunk chunk =  chunks.get(i);

            byteBuf.writeBytes(chunk.getBytes());
        }

        return byteBuf;
    }


    public UUID getMessageID() {
        return messageID;
    }

    public int getTotalExpectedChunks() {
        return totalExpectedChunks;
    }

    public HashMap<Integer, Chunk> getChunks() {
        return chunks;
    }

    public UUID getTimeoutId() {
        return timeoutId;
    }

    public void setTimeoutId(UUID timeoutId) {
        this.timeoutId = timeoutId;
    }
}

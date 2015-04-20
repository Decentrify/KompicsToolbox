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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CompleteChunkTracker {

    public final Map<Integer, Chunk> chunks;
    public final int lastChunk;

    public CompleteChunkTracker(UUID messageId, ByteBuf content, int datagramContentSize) {
        this.lastChunk = (content.readableBytes() % datagramContentSize == 0 ? content.readableBytes() / datagramContentSize - 1 : content.readableBytes() / datagramContentSize);
        this.chunks = new HashMap<Integer, Chunk>();
        int chunkNr = 0;
        while (content.readableBytes() > 0) {
            int readSize = (content.readableBytes() > datagramContentSize ? datagramContentSize : content.readableBytes());
            byte[] chunkContent = new byte[readSize];
            content.readBytes(chunkContent);
            chunks.put(chunkNr++, new Chunk(messageId, chunkNr, lastChunk, chunkContent));
        }
    }
}    

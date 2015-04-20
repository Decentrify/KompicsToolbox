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

import com.google.common.base.Optional;
import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ChunkSerializer implements Serializer {

    private final int id;

    public ChunkSerializer(int id) {
        this.id = id;
    }

    @Override
    public int identifier() {
        return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        Chunk obj = (Chunk) o;
        Serializers.lookupSerializer(UUID.class).toBinary(obj.messageId, buf);
        buf.writeByte(obj.chunkNr);
        buf.writeByte(obj.lastChunk);
        byte[] portBytes = Ints.toByteArray(obj.content.length);
        buf.writeByte(portBytes[2]);
        buf.writeByte(portBytes[3]);
        buf.writeBytes(obj.content);
    }

    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        UUID messageId = (UUID)Serializers.lookupSerializer(UUID.class).fromBinary(buf, hint);
        int chunkNr = buf.readByte();
        int lastChunk = buf.readByte();
        int contentSize = Ints.fromBytes((byte) 0, (byte) 0, buf.readByte(), buf.readByte());
        byte[] content = new byte[contentSize];
        buf.readBytes(content);
        return new Chunk(messageId, chunkNr, lastChunk, content);
    }
}
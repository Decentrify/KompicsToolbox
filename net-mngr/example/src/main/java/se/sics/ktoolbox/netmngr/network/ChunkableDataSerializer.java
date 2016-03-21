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
package se.sics.ktoolbox.netmngr.network;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.netmngr.core.ChunkableData;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ChunkableDataSerializer implements Serializer {
    public final int id;

    public ChunkableDataSerializer(int id) {
        this.id = id;
    }

    @Override
    public int identifier() {
        return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        ChunkableData obj = (ChunkableData) o;
        Serializers.toBinary(obj.eventId, buf);
        Serializers.toBinary(obj.overlayId, buf);
        buf.writeInt(obj.counter);
        buf.writeInt(obj.data.array().length);
        buf.writeBytes(obj.data);
    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        Identifier eventId = (Identifier) Serializers.fromBinary(buf, hint);
        Identifier overlayId = (Identifier) Serializers.fromBinary(buf, hint);
        int counter = buf.readInt();
        int dataSize = buf.readInt();
        byte[] data = new byte[dataSize];
        buf.readBytes(data);
        return new ChunkableData(eventId, overlayId, counter, ByteBuffer.wrap(data));
    }
}

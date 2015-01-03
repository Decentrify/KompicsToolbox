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

package se.sics.p2ptoolbox.serialization.serializer;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import se.sics.p2ptoolbox.serialization.SerializationContext;
import se.sics.p2ptoolbox.serialization.Serializer;
import se.sics.p2ptoolbox.serialization.Serializer.SerializerException;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class UUIDSerializer implements Serializer<UUID> {

    @Override
    public ByteBuf encode(SerializationContext context, ByteBuf buf, UUID obj) throws SerializerException {
        buf.writeLong(obj.getMostSignificantBits());
        buf.writeLong(obj.getLeastSignificantBits());
        return buf;
    }

    @Override
    public UUID decode(SerializationContext context, ByteBuf buf) throws SerializerException {
        Long uuidMSB = buf.readLong();
        Long uuidLSB = buf.readLong();
        return new UUID(uuidMSB, uuidLSB);
    }

    @Override
    public int getSize(SerializationContext context, UUID obj) throws SerializerException {
        return 2*Long.SIZE/8;
    }
}

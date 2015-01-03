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
import se.sics.p2ptoolbox.serialization.SerializationContext;
import se.sics.p2ptoolbox.serialization.Serializer;
import se.sics.p2ptoolbox.serialization.Serializer.SerializerException;
import se.sics.p2ptoolbox.serialization.msg.OverlayHeaderField;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class OverlayHeaderFieldSerializer implements Serializer<OverlayHeaderField> {

    @Override
    public ByteBuf encode(SerializationContext context, ByteBuf buf, OverlayHeaderField obj) throws SerializerException, SerializationContext.MissingException {
        buf.writeInt(obj.overlayId);
        return buf;
    }

    @Override
    public OverlayHeaderField decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
        int overlayId = buf.readInt();
        return new OverlayHeaderField(overlayId);
    }

    @Override
    public int getSize(SerializationContext context, OverlayHeaderField obj) throws SerializerException, SerializationContext.MissingException {
        int size = Integer.SIZE / 8; //overlayId
        return size;
    }
}
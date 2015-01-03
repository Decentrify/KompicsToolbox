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
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.util.UserTypesDecoderFactory;
import se.sics.gvod.net.util.UserTypesEncoderFactory;
import se.sics.p2ptoolbox.serialization.SerializationContext;
import se.sics.p2ptoolbox.serialization.Serializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class VodAddressSerializer implements Serializer<VodAddress> {

    @Override
    public ByteBuf encode(SerializationContext context, ByteBuf buf, VodAddress obj) throws SerializerException {
        try {
            UserTypesEncoderFactory.writeVodAddress(buf, obj);
            return buf;
        } catch (MessageEncodingException ex) {
            throw new SerializerException(ex);
        }
    }

    @Override
    public VodAddress decode(SerializationContext context, ByteBuf buf) throws SerializerException {
        try {
            return UserTypesDecoderFactory.readVodAddress(buf);
        } catch (MessageDecodingException ex) {
            throw new SerializerException(ex);
        }
    }

    @Override
    public int getSize(SerializationContext context, VodAddress obj) throws SerializerException {
        int size = 0;
        size += UserTypesEncoderFactory.ADDRESS_LEN; // address
        size += Integer.SIZE/8; // overlayId
        size += Byte.SIZE/8; //natPolicy
        size += (obj.getParents().isEmpty() ? 2 : 2 + obj.getParents().size() * UserTypesEncoderFactory.ADDRESS_LEN);
        return size;
    }
    
}

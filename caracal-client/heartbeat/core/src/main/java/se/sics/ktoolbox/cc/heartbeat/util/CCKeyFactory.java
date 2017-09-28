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
package se.sics.ktoolbox.cc.heartbeat.util;

import com.google.common.base.Optional;
import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.ByteBuffer;
import java.util.Arrays;
import se.sics.caracaldb.Key;
import se.sics.caracaldb.KeyRange;
import se.sics.kompics.id.Identifier;
import se.sics.kompics.network.netty.serialization.Serializers;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CCKeyFactory {

    public static Key getHeartbeatKey(byte[] schemaPrefix, Identifier overlayId, int slot) {
        Key.KeyBuilder key = new Key.KeyBuilder(schemaPrefix);
        ByteBuf buf = Unpooled.buffer();
        Serializers.toBinary(overlayId, buf);
        byte[] oId = Arrays.copyOfRange(buf.array(), 0, buf.readableBytes());
        key.append(new byte[]{(byte) oId.length});
        key.append(oId);
        key.append(Ints.toByteArray(slot));
        return key.get();
    }

    public static KeyRange getHeartbeatRange(byte[] schemaPrefix, Identifier overlayId) {
        Key.KeyBuilder prefix = new Key.KeyBuilder(schemaPrefix);
        ByteBuf buf = Unpooled.buffer();
        Serializers.toBinary(overlayId, buf);
        byte[] oId = Arrays.copyOfRange(buf.array(), 0, buf.readableBytes());
        prefix.append(new byte[]{(byte) oId.length});
        prefix.append(oId);
        return KeyRange.prefix(prefix.get());
    }

    public static Identifier extractOverlay(byte[] schemaPrefix, Key key) {
        ByteBuffer bbKey = key.getWrapper();
        bbKey.position(schemaPrefix.length);
        int overlaySize = bbKey.get();
        byte[] overlayId = new byte[overlaySize];
        bbKey.get(overlayId);
        ByteBuf buf = Unpooled.wrappedBuffer(overlayId);
        return (Identifier)Serializers.fromBinary(buf, Optional.absent());
    }
}

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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CCValueFactory {

    public static byte[] getHeartbeatValue(DecoratedAddress src) {
        ByteBuf buf = Unpooled.buffer();
        Serializers.lookupSerializer(DecoratedAddress.class).toBinary(src, buf);
        return Arrays.copyOf(buf.array(), buf.readableBytes());
    }

    public static Set<DecoratedAddress> extractHeartbeatSrc(Collection<byte[]> values) {
        Set<DecoratedAddress> result = new HashSet<DecoratedAddress>();
        Serializer serializer = Serializers.lookupSerializer(DecoratedAddress.class);
        for (byte[] value : values) {
            ByteBuf buf = Unpooled.wrappedBuffer(value);
            result.add((DecoratedAddress)serializer.fromBinary(buf, Optional.absent()));
        }
        return result;
    }
}

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
package se.sics.ktoolbox.util.network.basic;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import se.sics.kompics.network.netty.serialization.DatagramSerializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.util.network.KHeader;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BasicContentMsgSerializer implements DatagramSerializer {

    private final int id;

    public BasicContentMsgSerializer(int id) {
        this.id = id;
    }

    @Override
    public int identifier() {
        return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        BasicContentMsg obj = (BasicContentMsg) o;
        Serializers.toBinary(obj.getHeader(), buf);
        Serializers.toBinary(obj.getContent(), buf);
    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        return fromBinary(buf, (DatagramPacket)null);
    }

    @Override
    public Object fromBinary(ByteBuf buf, DatagramPacket datagram) {
        KHeader header = (KHeader)Serializers.fromBinary(buf, datagram);
        Object content = Serializers.fromBinary(buf, Optional.absent());
        return new BasicContentMsg(header, content);
    }
}

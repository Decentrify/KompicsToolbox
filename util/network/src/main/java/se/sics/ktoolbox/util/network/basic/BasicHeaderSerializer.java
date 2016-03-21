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
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.netty.serialization.DatagramSerializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BasicHeaderSerializer implements DatagramSerializer {

    private final int id;

    public BasicHeaderSerializer(int id) {
        this.id = id;
    }

    @Override
    public int identifier() {
        return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        BasicHeader obj = (BasicHeader) o;

        Serializers.toBinary(obj.getSource(), buf);
        Serializers.toBinary(obj.getDestination(), buf);

        switch (obj.getProtocol()) {
            case UDP:
                buf.writeByte(0);
                break;
            case TCP:
                buf.writeByte(1);
                break;
            case UDT:
                buf.writeByte(2);
                break;
            case LEDBAT:
                buf.writeByte(3);
                break;
            case MULTICAST_UDP:
                buf.writeByte(4);
                break;
            default:
                throw new RuntimeException("unknown transport protocol");
        }
    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        return fromBinary(buf, (DatagramPacket) null);
    }

    @Override
    public Object fromBinary(ByteBuf buf, DatagramPacket datagram) {
        KAddress src;
        KAddress dst;
        if (datagram != null) {
            src = (KAddress) Serializers.fromBinary(buf, Optional.fromNullable((Object) datagram.sender()));
            dst = (KAddress) Serializers.fromBinary(buf, Optional.fromNullable((Object) datagram.recipient()));
        } else {
            src = (KAddress) Serializers.fromBinary(buf, Optional.absent());
            dst = (KAddress) Serializers.fromBinary(buf, Optional.absent());
        }
        
        byte protocolByte = buf.readByte();
        Transport protocol;
        switch (protocolByte) {
            case 0:
                protocol = Transport.UDP;
                break;
            case 1:
                protocol = Transport.TCP;
                break;
            case 2:
                protocol = Transport.UDT;
                break;
            case 3:
                protocol = Transport.LEDBAT;
                break;
            case 4:
                protocol = Transport.MULTICAST_UDP;
                break;
            default:
                throw new RuntimeException("unknown transport protocol");
        }
        return new BasicHeader(src, dst, protocol);
    }
}

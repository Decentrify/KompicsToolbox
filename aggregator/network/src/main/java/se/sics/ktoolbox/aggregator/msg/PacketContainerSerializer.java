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
package se.sics.ktoolbox.aggregator.msg;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.aggregator.util.PacketInfo;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.UUID;

/**
 * Serializer for the packet container mainly containing the
 * packet information.
 * Created by babbar on 2015-09-07.
 */
public class PacketContainerSerializer implements Serializer {

    private int id;

    public PacketContainerSerializer(int id){
        this.id = id;
    }

    @Override
    public int identifier() {
        return this.id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        PacketContainer container = (PacketContainer)o;
        Serializers.lookupSerializer(UUID.class).toBinary(container.uuid, buf);
        Serializers.lookupSerializer(DecoratedAddress.class).toBinary(container.sourceAddress, buf);
        Serializers.toBinary(container.packetInfo, buf);
    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        UUID uuid = (UUID) Serializers.lookupSerializer(UUID.class).fromBinary(buf, hint);
        DecoratedAddress selfAddress = (DecoratedAddress)Serializers.lookupSerializer(DecoratedAddress.class).fromBinary(buf, hint);
        PacketInfo packetInfo = (PacketInfo)Serializers.fromBinary(buf, hint);
        return new PacketContainer(uuid, selfAddress, packetInfo);
    }
}

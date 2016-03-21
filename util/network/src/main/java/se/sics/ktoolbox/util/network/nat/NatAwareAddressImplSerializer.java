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
package se.sics.ktoolbox.util.network.nat;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.util.network.basic.BasicAddress;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NatAwareAddressImplSerializer implements Serializer {

    private final int id;

    public NatAwareAddressImplSerializer(int id) {
        this.id = id;
    }

    @Override
    public int identifier() {
        return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        NatAwareAddressImpl obj = (NatAwareAddressImpl) o;
        Serializers.lookupSerializer(NatType.class).toBinary(obj.natType, buf);
        
        Serializer basicAddressSerializer = Serializers.lookupSerializer(BasicAddress.class);
        basicAddressSerializer.toBinary(obj.publicAdr, buf);
        
        buf.writeByte(obj.parents.size());
        for (BasicAddress parent : obj.parents) {
            basicAddressSerializer.toBinary(parent, buf);
        }
    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        NatType natType = (NatType) Serializers.lookupSerializer(NatType.class).fromBinary(buf, hint);
        
        Serializer basicAddressSerializer = Serializers.lookupSerializer(BasicAddress.class);
        BasicAddress publicAdr = (BasicAddress) basicAddressSerializer.fromBinary(buf, hint);
        
        List<BasicAddress> parents = new ArrayList<>();
        int nrParents = buf.readByte();
        for (int i = 0; i < nrParents; i++) {
            parents.add((BasicAddress) basicAddressSerializer.fromBinary(buf, hint));
        }
        BasicAddress privateAdr = null;
        if(hint.isPresent() && hint.get() instanceof InetSocketAddress) {
            InetSocketAddress adr = (InetSocketAddress)hint.get();
            privateAdr = new BasicAddress(adr.getAddress(), adr.getPort(), publicAdr.getId());
            if(privateAdr.equals(publicAdr)) {
                privateAdr = null;
            }
        } 
        return new NatAwareAddressImpl(privateAdr, publicAdr, natType, parents);
    }
}

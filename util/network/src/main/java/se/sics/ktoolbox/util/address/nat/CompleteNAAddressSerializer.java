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
package se.sics.ktoolbox.util.address.nat;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.util.address.basic.BasicAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CompleteNAAddressSerializer implements Serializer {
    private final int id;

    public CompleteNAAddressSerializer(int id) {
        this.id = id;
    }
    
    @Override
    public int identifier() {
        return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        CompleteNAAddress obj = (CompleteNAAddress)o;
        Serializer basicAddressSerializer = Serializers.lookupSerializer(BasicAddress.class);
        basicAddressSerializer.toBinary(obj.baseAdr.baseAdr, buf);
        Serializers.lookupSerializer(NatType.class).toBinary(obj.baseAdr.natType, buf);
        buf.writeByte(obj.parents.size());
        for(BasicAddress parent : obj.parents) {
            basicAddressSerializer.toBinary(parent, buf);
        }
    }

    @Override
    public CompleteNAAddress fromBinary(ByteBuf buf, Optional<Object> hint) {
        Serializer basicAddressSerializer = Serializers.lookupSerializer(BasicAddress.class);
        BasicAddress baseAdr = (BasicAddress)basicAddressSerializer.fromBinary(buf, hint);
        NatType natType = (NatType)Serializers.lookupSerializer(NatType.class).fromBinary(buf, hint);
        List<BasicAddress> parents = new ArrayList<>();
        int nrParents = buf.readByte();
        for(int i = 0; i < nrParents; i++) {
            parents.add((BasicAddress)basicAddressSerializer.fromBinary(buf, hint));
        }
        return new CompleteNAAddress(new StrippedNAAddress(baseAdr, natType), parents);
    }
}

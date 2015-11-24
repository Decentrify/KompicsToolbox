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
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.util.address.resolution.AddressResolutionHelper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class StrippedNAAddressSerializer implements Serializer {
    private final int id;

    public StrippedNAAddressSerializer(int id) {
        this.id = id;
    }
    
    @Override
    public int identifier() {
        return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        StrippedNAAddress strippedAdr = (StrippedNAAddress)o;
        CompleteNAAddress completeAdr = (CompleteNAAddress)AddressResolutionHelper.resolve(strippedAdr);
        Serializers.lookupSerializer(CompleteNAAddress.class).toBinary(completeAdr, buf);
    }

    @Override
    public StrippedNAAddress fromBinary(ByteBuf buf, Optional<Object> hint) {
        CompleteNAAddress completeAdr = (CompleteNAAddress)Serializers.lookupSerializer(CompleteNAAddress.class).fromBinary(buf, hint);
        AddressResolutionHelper.setIndirect(completeAdr);
        return completeAdr.strip();
    }
}

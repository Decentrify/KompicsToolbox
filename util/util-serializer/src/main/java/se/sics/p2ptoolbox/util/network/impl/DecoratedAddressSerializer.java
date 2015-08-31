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
package se.sics.p2ptoolbox.util.network.impl;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import org.javatuples.Pair;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.util.traits.AcceptedTraits;
import se.sics.p2ptoolbox.util.traits.Trait;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class DecoratedAddressSerializer implements Serializer {

    private final int id;

    public DecoratedAddressSerializer(int id) {
        this.id = id;
    }

    @Override
    public int identifier() {
        return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        DecoratedAddress obj = (DecoratedAddress) o;
        Serializers.lookupSerializer(BasicAddress.class).toBinary(obj.getBase(), buf);
        toBinary(o, buf, obj.getTraits());
    }

    //TODO Alex - try to see if it can be optimized
    private void toBinary(Object o, ByteBuf buf, Trait[] traits) {
        //we do not know how many traits we currently have, 
        //so we save space and write the size at the end
        int writerIndex = buf.writerIndex();
        buf.writeByte(0);
        int nrTraits = 0;
        AcceptedTraits at = DecoratedAddress.getAcceptedTraits();
        for (Trait trait : traits) {
            if (trait != null) {
                byte traitId = at.getTraitInfo(trait.getClass()).id;
                buf.writeByte(traitId);
                Serializers.lookupSerializer(trait.getClass()).toBinary(o, buf);
                nrTraits++;
            }
        }
        if (nrTraits > 10) {
            /**
             * TODO Alex - total random - 10... but more than 10 traits would
             * probably make the address too big for any practical use
             */
            throw new RuntimeException("Decorated address traits logic - too many");
        }
        int lastWriterIndex = buf.writerIndex();
        buf.writerIndex(writerIndex);
        buf.writeByte((byte) nrTraits);
        buf.writerIndex(lastWriterIndex);
    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        Serializer basicAdrSerializer = Serializers.lookupSerializer(BasicAddress.class);
        BasicAddress base = (BasicAddress) basicAdrSerializer.fromBinary(buf, hint);
        Trait[] traits = traitsFromBinary(buf, hint);
        
        return new DecoratedAddress(base, traits);
    }

    //TODO Alex - try to see if it can be optimized
    private Trait[] traitsFromBinary(ByteBuf buf, Optional<Object> hint) {
        AcceptedTraits at = DecoratedAddress.getAcceptedTraits();
        Trait[] traits = new Trait[at.size()];
        int nrTraits = buf.readByte();
        for (int i = 0; i < nrTraits; i++) {
            byte traitId = buf.readByte();
            Map.Entry<Class<? extends Trait>, Pair<Integer, Byte>> tInfo = at.getTraitInfo(traitId);
            Serializer traitSerializer = Serializers.lookupSerializer(tInfo.getKey());
            traits[tInfo.getValue().getValue0()] = (Trait) traitSerializer.fromBinary(buf, hint);
        }
        return traits;
    }
}

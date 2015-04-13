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
import java.util.HashSet;
import java.util.Set;
import org.javatuples.Pair;
import org.junit.Assert;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.util.BitBuffer;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.traits.Nated;

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
        Serializer basicAdrSerializer = Serializers.lookupSerializer(BasicAddress.class);
        basicAdrSerializer.toBinary(obj.getBase(), buf);

        BitBuffer flags = BitBuffer.create();
        //traits - uses flag 0 for the Nated Trait
        if (obj.hasTrait(Nated.class)) {
            flags.write(Pair.with(0, true));
        } else {
            flags.write(Pair.with(0, false));
        }

        buf.writeBytes(flags.finalise());

        if (obj.hasTrait(Nated.class)) {
            Assert.assertTrue("too many parents", obj.getParents().size() < 128);
            buf.writeByte(obj.getParents().size());
            for (DecoratedAddress adr : obj.getParents()) {
                this.toBinary(adr, buf);
            }
        }
    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        Serializer basicAdrSerializer = Serializers.lookupSerializer(BasicAddress.class);
        BasicAddress base = (BasicAddress)basicAdrSerializer.fromBinary(buf, hint);
        
        byte[] bFlags = new byte[1];
        buf.readBytes(bFlags);
        boolean[] flags = BitBuffer.extract(1, bFlags);
        if(flags[0]) {
            int parentsSize = buf.readByte();
            Set<DecoratedAddress> parents = new HashSet<DecoratedAddress>();
            for(int i = 0; i < parentsSize; i++) {
                parents.add((DecoratedAddress)this.fromBinary(buf, hint));
            }
            return new DecoratedAddress(base, parents);
        }  
        return new DecoratedAddress(base);
    }
}

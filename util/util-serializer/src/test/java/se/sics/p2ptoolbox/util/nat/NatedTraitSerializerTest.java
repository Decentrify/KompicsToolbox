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
package se.sics.p2ptoolbox.util.nat;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import org.javatuples.Pair;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup;
import se.sics.p2ptoolbox.util.traits.AcceptedTraits;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NatedTraitSerializerTest {

    @BeforeClass
    public static void setup() {
        int serializerId = 128;
        BasicSerializerSetup.registerBasicSerializers(serializerId);
        ImmutableMap acceptedTraits = ImmutableMap.of(NatedTrait.class, 0);
        DecoratedAddress.setAcceptedTraits(new AcceptedTraits(acceptedTraits));
    }

    @Test
    public void testTraits() throws UnknownHostException {
        Serializer natedTraitSerializer = Serializers.lookupSerializer(NatedTrait.class);

        NatedTrait original, copy;
        ByteBuf buf;

        buf = Unpooled.buffer();
        original = NatedTrait.open();
        natedTraitSerializer.toBinary(original, buf);
        copy = (NatedTrait) natedTraitSerializer.fromBinary(Unpooled.wrappedBuffer(buf.array()), Optional.absent());
        Assert.assertEquals(original, copy);

        buf = Unpooled.buffer();
        original = NatedTrait.firewall();
        natedTraitSerializer.toBinary(original, buf);
        copy = (NatedTrait) natedTraitSerializer.fromBinary(Unpooled.wrappedBuffer(buf.array()), Optional.absent());
        Assert.assertEquals(original, copy);

        buf = Unpooled.buffer();
        original = NatedTrait.udpBlocked();
        natedTraitSerializer.toBinary(original, buf);
        copy = (NatedTrait) natedTraitSerializer.fromBinary(Unpooled.wrappedBuffer(buf.array()), Optional.absent());
        Assert.assertEquals(original, copy);

        buf = Unpooled.buffer();
        original = NatedTrait.upnp();
        natedTraitSerializer.toBinary(original, buf);
        copy = (NatedTrait) natedTraitSerializer.fromBinary(Unpooled.wrappedBuffer(buf.array()), Optional.absent());
        Assert.assertEquals(original, copy);

        buf = Unpooled.buffer();
        original = NatedTrait.nated(Nat.MappingPolicy.HOST_DEPENDENT, Nat.AllocationPolicy.PORT_CONTIGUITY, 0, Nat.FilteringPolicy.HOST_DEPENDENT, 10000, new ArrayList<DecoratedAddress>());
        natedTraitSerializer.toBinary(original, buf);
        copy = (NatedTrait) natedTraitSerializer.fromBinary(Unpooled.wrappedBuffer(buf.array()), Optional.absent());
        Assert.assertEquals(original, copy);

        buf = Unpooled.buffer();
        ArrayList<DecoratedAddress> parents = new ArrayList<DecoratedAddress>();
        DecoratedAddress parent;
        parent = new DecoratedAddress(new BasicAddress(InetAddress.getLocalHost(), 10000, 1));
        parent.addTrait(NatedTrait.open());
        parents.add(parent);
        parent = new DecoratedAddress(new BasicAddress(InetAddress.getLocalHost(), 10001, 2));
        parent.addTrait(NatedTrait.open());
        parents.add(parent);
        original = NatedTrait.nated(Nat.MappingPolicy.HOST_DEPENDENT, Nat.AllocationPolicy.PORT_CONTIGUITY, 0, Nat.FilteringPolicy.HOST_DEPENDENT, 10000, parents);
        natedTraitSerializer.toBinary(original, buf);
        copy = (NatedTrait) natedTraitSerializer.fromBinary(Unpooled.wrappedBuffer(buf.array()), Optional.absent());
        Assert.assertEquals(original, copy);
    }
}

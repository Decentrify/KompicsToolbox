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
package se.sics.ktoolbox.util.address.nat;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.util.address.basic.BasicAddress;
import se.sics.ktoolbox.util.address.resolution.AddressResolutionHelper;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CompleteNAAddressSerializerTest {

    @BeforeClass
    public static void setup() {
        int serializerId = 128;
        serializerId = BasicSerializerSetup.registerBasicSerializers(serializerId);
        
        AddressResolutionHelper.reset();
        AddressResolutionHelper.useNatAwareAddresses();
    }

    @Test
    public void testOpenAddress() throws UnknownHostException {
        Serializer serializer = Serializers.lookupSerializer(CompleteNAAddress.class);
        CompleteNAAddress original, copy;
        ByteBuf buf;

        BasicAddress baseAdr = new BasicAddress(InetAddress.getLocalHost(), 10000, 1);
        original = CompleteNAAddress.open(baseAdr);
        buf = Unpooled.buffer();
        serializer.toBinary(original, buf);
        
        ByteBuf copyBuf = Unpooled.buffer();
        buf.getBytes(0, copyBuf, buf.readableBytes());
        copy = (CompleteNAAddress) serializer.fromBinary(copyBuf, Optional.absent());
        compareCompleteAddresses(original, copy);
        Assert.assertEquals(0, copyBuf.readableBytes());
    }

    @Test
    public void testNatedAddress() throws UnknownHostException {
        Serializer serializer = Serializers.lookupSerializer(CompleteNAAddress.class);
        CompleteNAAddress original, copy;
        ByteBuf buf;

        BasicAddress baseAdr = new BasicAddress(InetAddress.getLocalHost(), 10000, 2);
        NatType natType = NatType.nated(Nat.MappingPolicy.HOST_DEPENDENT, Nat.AllocationPolicy.PORT_CONTIGUITY, 1,
                Nat.FilteringPolicy.HOST_DEPENDENT, 10000);
        ArrayList<BasicAddress> parents = new ArrayList<>();
        BasicAddress parent;
        parent = new BasicAddress(InetAddress.getLocalHost(), 10000, 1);
        parents.add(parent);
        parent = new BasicAddress(InetAddress.getLocalHost(), 10001, 2);
        parents.add(parent);
        original = new CompleteNAAddress(baseAdr, natType, parents);
        buf = Unpooled.buffer();
        serializer.toBinary(original, buf);
        
        ByteBuf copyBuf = Unpooled.buffer();
        buf.getBytes(0, copyBuf, buf.readableBytes());
        copy = (CompleteNAAddress) serializer.fromBinary(copyBuf, Optional.absent());
        compareCompleteAddresses(original, copy);
        Assert.assertEquals(0, copyBuf.readableBytes());
    }
    
    public void compareCompleteAddresses(CompleteNAAddress adr1, CompleteNAAddress adr2) {
        Assert.assertEquals(adr1.getBaseAdr(), adr2.getBaseAdr());
        Assert.assertEquals(adr1.getNatType(), adr2.getNatType());
        Assert.assertEquals(adr1.getParents(), adr2.getParents());
    }
}

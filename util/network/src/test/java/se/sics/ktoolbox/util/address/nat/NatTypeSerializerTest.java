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
import java.net.UnknownHostException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NatTypeSerializerTest {

    @BeforeClass
    public static void setup() {
        int serializerId = 128;
        serializerId = BasicSerializerSetup.registerBasicSerializers(serializerId);
    }

    @Test
    public void testTraits() throws UnknownHostException {
        Serializer natTypeSerializer = Serializers.lookupSerializer(NatType.class);

        NatType original, copy;
        ByteBuf buf, copyBuf;

        buf = Unpooled.buffer();
        original = NatType.open();
        natTypeSerializer.toBinary(original, buf);

        copyBuf = Unpooled.buffer();
        buf.getBytes(0, copyBuf, buf.readableBytes());
        copy = (NatType) natTypeSerializer.fromBinary(copyBuf, Optional.absent());

        Assert.assertEquals(original, copy);
        Assert.assertEquals(0, copyBuf.readableBytes());

        //****************
        buf = Unpooled.buffer();
        original = NatType.firewall();
        natTypeSerializer.toBinary(original, buf);

        copyBuf = Unpooled.buffer();
        buf.getBytes(0, copyBuf, buf.readableBytes());
        copy = (NatType) natTypeSerializer.fromBinary(copyBuf, Optional.absent());

        Assert.assertEquals(original, copy);
        Assert.assertEquals(0, copyBuf.readableBytes());

        //*****************
        buf = Unpooled.buffer();
        original = NatType.udpBlocked();
        natTypeSerializer.toBinary(original, buf);

        copyBuf = Unpooled.buffer();
        buf.getBytes(0, copyBuf, buf.readableBytes());
        copy = (NatType) natTypeSerializer.fromBinary(copyBuf, Optional.absent());

        Assert.assertEquals(original, copy);
        Assert.assertEquals(0, copyBuf.readableBytes());

        //*****************
        buf = Unpooled.buffer();
        original = NatType.upnp();
        natTypeSerializer.toBinary(original, buf);

        copyBuf = Unpooled.buffer();
        buf.getBytes(0, copyBuf, buf.readableBytes());
        copy = (NatType) natTypeSerializer.fromBinary(copyBuf, Optional.absent());

        Assert.assertEquals(original, copy);
        Assert.assertEquals(0, copyBuf.readableBytes());
        
        //*****************
        buf = Unpooled.buffer();
        original = NatType.nated(Nat.MappingPolicy.HOST_DEPENDENT, Nat.AllocationPolicy.PORT_CONTIGUITY, 0, Nat.FilteringPolicy.HOST_DEPENDENT, 10000);
        natTypeSerializer.toBinary(original, buf);
        
        copyBuf = Unpooled.buffer();
        buf.getBytes(0, copyBuf, buf.readableBytes());
        copy = (NatType) natTypeSerializer.fromBinary(copyBuf, Optional.absent());
        
        Assert.assertEquals(original, copy);
        Assert.assertEquals(0, copyBuf.readableBytes());
        
        //******************
        buf = Unpooled.buffer();
        original = NatType.nated(Nat.MappingPolicy.HOST_DEPENDENT, Nat.AllocationPolicy.PORT_CONTIGUITY, 0, Nat.FilteringPolicy.HOST_DEPENDENT, 10000);
        natTypeSerializer.toBinary(original, buf);
        
        copyBuf = Unpooled.buffer();
        buf.getBytes(0, copyBuf, buf.readableBytes());
        copy = (NatType) natTypeSerializer.fromBinary(copyBuf, Optional.absent());
        
        Assert.assertEquals(original, copy);
        Assert.assertEquals(0, copyBuf.readableBytes());
        
        //******************
        buf = Unpooled.buffer();
        original = NatType.unknown();
        natTypeSerializer.toBinary(original, buf);
        
        copyBuf = Unpooled.buffer();
        buf.getBytes(0, copyBuf, buf.readableBytes());
        copy = (NatType) natTypeSerializer.fromBinary(copyBuf, Optional.absent());
        
        Assert.assertEquals(original, copy);
        Assert.assertEquals(0, copyBuf.readableBytes());
    }
}

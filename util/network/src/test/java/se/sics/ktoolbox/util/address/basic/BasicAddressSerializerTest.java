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
package se.sics.ktoolbox.util.address.basic;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.util.address.basic.BasicAddress;
import se.sics.ktoolbox.util.address.resolution.AddressResolutionHelper;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BasicAddressSerializerTest {
    @BeforeClass
    public static void setup() {
        int serializerId = 128;
        serializerId = BasicSerializerSetup.registerBasicSerializers(serializerId);
        
        AddressResolutionHelper.reset();
        AddressResolutionHelper.useNatAwareAddresses();
    }
    
    @Test
    public void testBasicAddress() throws UnknownHostException {
        Serializer serializer = Serializers.lookupSerializer(BasicAddress.class);
        BasicAddress original, copy;
        ByteBuf buf;

        original = new BasicAddress(InetAddress.getLocalHost(), 10000, 1);
        buf = Unpooled.buffer();
        serializer.toBinary(original, buf);
        
        ByteBuf copyBuf = Unpooled.buffer();
        buf.getBytes(0, copyBuf, buf.readableBytes());
        copy = (BasicAddress) serializer.fromBinary(copyBuf, Optional.absent());
        
        Assert.assertEquals(original, copy);
        Assert.assertEquals(0, copyBuf.readableBytes());
    }
}

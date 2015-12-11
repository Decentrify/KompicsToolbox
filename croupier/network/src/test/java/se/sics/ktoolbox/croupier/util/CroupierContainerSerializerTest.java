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
package se.sics.ktoolbox.croupier.util;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.kompics.simutil.identifiable.impl.IntIdentifier;
import se.sics.kompics.simutil.msg.impl.BasicAddress;
import se.sics.ktoolbox.croupier.CroupierSerializerSetup;
import se.sics.ktoolbox.util.address.nat.NatAwareAddressImpl;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CroupierContainerSerializerTest {

    @BeforeClass
    public static void setup() {
        int serializerId = 128;
        serializerId = BasicSerializerSetup.registerBasicSerializers(serializerId);
        serializerId = CroupierSerializerSetup.registerSerializers(serializerId);

        TestHelper.TestContent1Serializer testSerializer = new TestHelper.TestContent1Serializer(serializerId++);
        Serializers.register(testSerializer, "testContent1Serializer");
        Serializers.register(TestHelper.TestContent1.class, "testContent1Serializer");
    }

    @Test
    public void testCroupierContainer() {
        Serializer serializer = Serializers.lookupSerializer(CroupierContainer.class);
        CroupierContainer original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        InetAddress localHost;
        try {
            localHost = InetAddress.getByName("localhost");
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }

        NatAwareAddressImpl simpleAdr1 = NatAwareAddressImpl.open(new BasicAddress(localHost, 10000, new IntIdentifier(1)));
        original = new CroupierContainer(simpleAdr1, new TestHelper.TestContent1(1));
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);

        serializedCopy = Unpooled.buffer();
        serializedOriginal.getBytes(0, serializedCopy, serializedOriginal.readableBytes());
        copy = (CroupierContainer) serializer.fromBinary(serializedCopy, Optional.absent());

        Assert.assertEquals(original.age, copy.age);
        Assert.assertEquals(original.content, copy.content);
        Assert.assertEquals(original.src.getPrivateAdr(), copy.src.getPrivateAdr());
        Assert.assertEquals(original.src.getPublicAdr(), copy.src.getPublicAdr());
        Assert.assertEquals(original.src.getNatType(), copy.src.getNatType());
        Assert.assertTrue(Objects.equals(original.src.getParents(), copy.src.getParents()));
        Assert.assertEquals(0, serializedCopy.readableBytes());
    }
}

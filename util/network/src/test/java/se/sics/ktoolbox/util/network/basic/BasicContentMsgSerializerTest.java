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
package se.sics.ktoolbox.util.network.basic;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayIdFactory;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayRegistry;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BasicContentMsgSerializerTest {

    @BeforeClass
    public static void setup() {
        OverlayRegistry.initiate(new OverlayId.BasicTypeFactory((byte)0), new OverlayId.BasicTypeComparator());
        IdentifierRegistryV2.registerBaseDefaults1(64);
        int serializerId = 128;
        serializerId = BasicSerializerSetup.registerBasicSerializers(serializerId);
        TestSerializer testSerializer = new TestSerializer(serializerId++);
        Serializers.register(testSerializer, "testSerializer");
        Serializers.register(TestContent.class, "testSerializer");
    }

    @Test
    public void testBasicContentMsg() {
        Serializer serializer = Serializers.lookupSerializer(BasicContentMsg.class);
        BasicContentMsg original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        InetAddress localHost;
        try {
            localHost = InetAddress.getByName("localhost");
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }

        IdentifierFactory nodeIdFactory = IdentifierRegistryV2.instance(BasicIdentifiers.Values.NODE, java.util.Optional.of(1234l));
        BasicAddress basicAdr1 = new BasicAddress(localHost, 10000, nodeIdFactory.randomId());
        BasicAddress basicAdr2 = new BasicAddress(localHost, 10000, nodeIdFactory.randomId());
        BasicAddress basicAdr3 = new BasicAddress(localHost, 10000, nodeIdFactory.randomId());
        BasicAddress basicAdr4 = new BasicAddress(localHost, 10000, nodeIdFactory.randomId());

        byte ownerId = 1;
        IdentifierFactory baseOverlayIdFactory = IdentifierRegistryV2.instance(BasicIdentifiers.Values.OVERLAY, java.util.Optional.of(1234l));
        OverlayIdFactory overlayIdFactory = new OverlayIdFactory(baseOverlayIdFactory, OverlayId.BasicTypes.CROUPIER, ownerId);
        DecoratedHeader header = new DecoratedHeader(new BasicHeader(basicAdr1, basicAdr4, Transport.UDP), overlayIdFactory.randomId());
        TestContent content = new TestContent(5);

        original = new BasicContentMsg(header, content);
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);

        serializedCopy = Unpooled.buffer();
        serializedOriginal.getBytes(0, serializedCopy, serializedOriginal.readableBytes());
        copy = (BasicContentMsg) serializer.fromBinary(serializedCopy, Optional.absent());

        Assert.assertEquals(original.getHeader().getSource(), copy.getHeader().getSource());
        Assert.assertEquals(original.getHeader().getDestination(), copy.getHeader().getDestination());
        Assert.assertEquals(original.getHeader().getProtocol(), copy.getHeader().getProtocol());
        Assert.assertEquals(original.getContent(), copy.getContent());
        Assert.assertEquals(0, serializedCopy.readableBytes());
    }

    public static class TestContent {

        public final int val;

        public TestContent(int val) {
            this.val = val;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 59 * hash + this.val;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TestContent other = (TestContent) obj;
            if (this.val != other.val) {
                return false;
            }
            return true;
        }
    }

    public static class TestSerializer implements Serializer {

        public final int id;

        public TestSerializer(int id) {
            this.id = id;
        }

        @Override
        public int identifier() {
            return id;
        }

        @Override
        public void toBinary(Object o, ByteBuf buf) {
            TestContent obj = (TestContent) o;
            buf.writeInt(obj.val);
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            return new TestContent(buf.readInt());
        }
    }
}

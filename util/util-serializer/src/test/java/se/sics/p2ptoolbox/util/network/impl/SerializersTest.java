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
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup;
import se.sics.p2ptoolbox.util.traits.AcceptedTraits;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SerializersTest {

    private static InetAddress localHost;
    private DecoratedAddress simpleAdr1, simpleAdr2, simpleAdr3, simpleAdr4;

    @BeforeClass
    public static void oneTimeSetup() {
        try {
            localHost = InetAddress.getByName("localhost");
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
        
        int currentId = 128;
        
        currentId = BasicSerializerSetup.registerBasicSerializers(currentId);
        TestSerializer testSerializer = new TestSerializer(currentId++);
        Serializers.register(testSerializer, "testSerializer");
        Serializers.register(TestContent.class, "testSerializer");
    }

    @Before
    public void setup() {
        simpleAdr1 = new DecoratedAddress(new BasicAddress(localHost, 10000, 1));
        simpleAdr2 = new DecoratedAddress(new BasicAddress(localHost, 10000, 2));
        simpleAdr3 = new DecoratedAddress(new BasicAddress(localHost, 10000, 3));
        simpleAdr4 = new DecoratedAddress(new BasicAddress(localHost, 10000, 4));
    }

    @Test
    public void testUUIDSerializer() {
        Serializer serializer = Serializers.lookupSerializer(UUID.class);
        UUID original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        original = UUID.randomUUID();
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);
        serializedCopy = Unpooled.wrappedBuffer(serializedOriginal.array());
        copy = (UUID) serializer.fromBinary(serializedCopy, Optional.absent());

        Assert.assertEquals(original, copy);
    }

    @Test
    public void testBasicAddressSerializer() throws UnknownHostException {
        Serializer serializer = Serializers.lookupSerializer(BasicAddress.class);
        BasicAddress original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        InetAddress ia = InetAddress.getByName("localhost");

        original = new BasicAddress(ia, 10000, 1);
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);
        serializedCopy = Unpooled.wrappedBuffer(serializedOriginal.array());
        copy = (BasicAddress) serializer.fromBinary(serializedCopy, Optional.absent());

        Assert.assertEquals(original, copy);
    }

    @Test
    public void testDecoratedAddressSerializer1() throws UnknownHostException {
        Serializer serializer = Serializers.lookupSerializer(DecoratedAddress.class);
        DecoratedAddress copy;
        ByteBuf serializedOriginal, serializedCopy;

        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(simpleAdr1, serializedOriginal);
        serializedCopy = Unpooled.wrappedBuffer(serializedOriginal.array());
        copy = (DecoratedAddress) serializer.fromBinary(serializedCopy, Optional.absent());

        Assert.assertEquals(simpleAdr1, copy);
    }
    
     @Test
    public void testDecoratedAddressSerializer2() throws UnknownHostException {
        Serializer serializer = Serializers.lookupSerializer(DecoratedAddress.class);
        DecoratedAddress copy1, copy2;
        ByteBuf serializedOriginal, serializedCopy;

        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(simpleAdr1, serializedOriginal);
        serializer.toBinary(simpleAdr2, serializedOriginal);
        serializedCopy = Unpooled.wrappedBuffer(serializedOriginal.array());
        copy1 = (DecoratedAddress) serializer.fromBinary(serializedCopy, Optional.absent());
        copy2 = (DecoratedAddress) serializer.fromBinary(serializedCopy, Optional.absent());
        
        Assert.assertEquals(simpleAdr1, copy1);
        Assert.assertEquals(simpleAdr2, copy2);
    }

    @Test
    public void testBasicHeaderSerialize1() throws UnknownHostException {
        Serializer serializer = Serializers.lookupSerializer(BasicHeader.class);
        BasicHeader<DecoratedAddress> original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        original = new BasicHeader(simpleAdr1, simpleAdr2, Transport.UDP);
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);
        serializedCopy = Unpooled.wrappedBuffer(serializedOriginal.array());
        copy = (BasicHeader<DecoratedAddress>) serializer.fromBinary(serializedCopy, Optional.absent());

        Assert.assertEquals(original, copy);
    }

    @Test
    public void testRouteSerializer() throws UnknownHostException {
        Serializer serializer = Serializers.lookupSerializer(Route.class);
        Route<DecoratedAddress> original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        List<DecoratedAddress> route = new ArrayList<DecoratedAddress>();
        route.add(simpleAdr1);
        route.add(simpleAdr2);
        original = new Route(route);
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);
        serializedCopy = Unpooled.wrappedBuffer(serializedOriginal.array());
        copy = (Route<DecoratedAddress>) serializer.fromBinary(serializedCopy, Optional.absent());
        Assert.assertEquals(original, copy);
    }

    @Test
    public void testDecoratedHeaderSerializer() throws UnknownHostException {
        Serializer serializer = Serializers.lookupSerializer(DecoratedHeader.class);
        DecoratedHeader<DecoratedAddress> original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        List<DecoratedAddress> route = new ArrayList<DecoratedAddress>();
        route.add(simpleAdr2);
        route.add(simpleAdr3);
        original = new DecoratedHeader(new BasicHeader(simpleAdr1, simpleAdr4, Transport.UDP), new Route(route), 10);
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);
        serializedCopy = Unpooled.wrappedBuffer(serializedOriginal.array());
        copy = (DecoratedHeader<DecoratedAddress>) serializer.fromBinary(serializedCopy, Optional.absent());
//        System.out.println(original);
//        System.out.println(copy);
        Assert.assertEquals(original, copy);
    }

    @Test
    public void testBasicContentMsg() {
        Serializer serializer = Serializers.lookupSerializer(BasicContentMsg.class);
        BasicContentMsg original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        List<DecoratedAddress> route = new ArrayList<DecoratedAddress>();
        route.add(simpleAdr2);
        route.add(simpleAdr3);
        DecoratedHeader header = new DecoratedHeader(new BasicHeader(simpleAdr1, simpleAdr4, Transport.UDP), new Route(route), 10);
        TestContent content = new TestContent(5);
        original = new BasicContentMsg(header, content);
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);
        serializedCopy = Unpooled.wrappedBuffer(serializedOriginal.array());
        copy = (BasicContentMsg) serializer.fromBinary(serializedCopy, Optional.absent());
        Assert.assertEquals(original, copy);
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

        public int identifier() {
            return id;
        }

        public void toBinary(Object o, ByteBuf buf) {
            TestContent obj = (TestContent) o;
            buf.writeInt(obj.val);
        }

        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            return new TestContent(buf.readInt());
        }

    }
}

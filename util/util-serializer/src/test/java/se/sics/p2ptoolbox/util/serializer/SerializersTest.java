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
package se.sics.p2ptoolbox.util.serializer;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsgSerializer;
import se.sics.p2ptoolbox.util.network.impl.BasicHeader;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;
import se.sics.p2ptoolbox.util.network.impl.Route;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SerializersTest {

    private static InetAddress localHost;

    {
        BasicSerializerSetup.registerBasicSerializers(0);
        try {
            localHost = InetAddress.getByName("localhost");
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
    }

    private DecoratedAddress simpleAdr1, simpleAdr2, simpleAdr3, simpleAdr4, simpleAdr5;
    private DecoratedAddress natedAdr1, natedAdr2;

    @BeforeClass
    public static void oneTimeSetup() {
        TestSerializer testSerializer = new TestSerializer(255);
        Serializers.register(testSerializer, "testSerializer");
        Serializers.register(TestContent.class, "testSerializer");
    }

    @Before
    public void setup() {
        simpleAdr1 = new DecoratedAddress(localHost, 10000, 1);
        simpleAdr2 = new DecoratedAddress(localHost, 10000, 2);
        simpleAdr3 = new DecoratedAddress(localHost, 10000, 3);
        simpleAdr4 = new DecoratedAddress(localHost, 10000, 4);
        simpleAdr5 = new DecoratedAddress(localHost, 10000, 5);
        Set<DecoratedAddress> parents1 = new HashSet<DecoratedAddress>();
        parents1.add(simpleAdr1);
        parents1.add(simpleAdr2);
        natedAdr1 = new DecoratedAddress(new BasicAddress(localHost, 20000, 1), parents1);
        Set<DecoratedAddress> parents2 = new HashSet<DecoratedAddress>();
        parents2.add(simpleAdr3);
        parents2.add(simpleAdr4);
        parents2.add(simpleAdr5);
        natedAdr2 = new DecoratedAddress(new BasicAddress(localHost, 20000, 2), parents2);
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
    public void testDecoratedAddressSerializer() throws UnknownHostException {
        Serializer serializer = Serializers.lookupSerializer(DecoratedAddress.class);
        DecoratedAddress copy;
        ByteBuf serializedOriginal, serializedCopy;

        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(natedAdr1, serializedOriginal);
        serializedCopy = Unpooled.wrappedBuffer(serializedOriginal.array());
        copy = (DecoratedAddress) serializer.fromBinary(serializedCopy, Optional.absent());

        Assert.assertEquals(natedAdr1, copy);
    }

    @Test
    public void testBasicHeaderSerialize1() throws UnknownHostException {
        Serializer serializer = Serializers.lookupSerializer(BasicHeader.class);
        BasicHeader<DecoratedAddress> original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        original = new BasicHeader(simpleAdr1, natedAdr1, Transport.UDP);
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
        route.add(natedAdr1);
        route.add(simpleAdr2);
        original = new Route(route);
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);
        serializedCopy = Unpooled.wrappedBuffer(serializedOriginal.array());
        copy = (Route<DecoratedAddress>) serializer.fromBinary(serializedCopy, Optional.absent());
//        System.out.println(original);
//        System.out.println(copy);
        Assert.assertEquals(original, copy);
    }

    @Test
    public void testDecoratedHeaderSerializer() throws UnknownHostException {
        Serializer serializer = Serializers.lookupSerializer(DecoratedHeader.class);
        DecoratedHeader<DecoratedAddress> original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        List<DecoratedAddress> route = new ArrayList<DecoratedAddress>();
        route.add(simpleAdr1);
        route.add(simpleAdr2);
        route.add(natedAdr1);
        original = new DecoratedHeader(new BasicHeader(simpleAdr3, natedAdr2, Transport.UDP), new Route(route), 10);
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
        route.add(simpleAdr1);
        route.add(simpleAdr2);
        route.add(natedAdr1);
        DecoratedHeader header = new DecoratedHeader(new BasicHeader(simpleAdr3, natedAdr2, Transport.UDP), new Route(route), 10);
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

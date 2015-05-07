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
package se.sics.p2ptoolbox.gradient;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.croupier.CroupierSerializerSetup;
import se.sics.p2ptoolbox.gradient.msg.GradientShuffle;
import se.sics.p2ptoolbox.gradient.util.GradientContainer;
import se.sics.p2ptoolbox.gradient.util.GradientLocalView;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SerializersTest {

    private static InetAddress localHost;

    {
        try {
            localHost = InetAddress.getByName("localhost");
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
    }

    private DecoratedAddress simpleAdr1, simpleAdr2, simpleAdr3, simpleAdr4, simpleAdr5;
    private DecoratedAddress natedAdr1, natedAdr2;
    private GradientContainer container1, container2, container3;

    @BeforeClass
    public static void oneTimeSetup() {
        int currentId = 128;
        BasicSerializerSetup.registerBasicSerializers(currentId);
        currentId = currentId + BasicSerializerSetup.serializerIds;
        currentId = CroupierSerializerSetup.registerSerializers(currentId);
        currentId = GradientSerializerSetup.registerSerializers(currentId);
        TestSerializer testSerializer = new TestSerializer(currentId++);
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

        container1 = new GradientContainer(simpleAdr1, new TestContent(1), 0, 1);
        container2 = new GradientContainer(simpleAdr2, new TestContent(2), 1, 2);
        container3 = new GradientContainer(natedAdr1, new TestContent(3), 2, 3);
    }

    @Test
    public void testGradientContainer() {
        Serializer serializer = Serializers.lookupSerializer(GradientContainer.class);
        GradientContainer original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        original = container1;
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);
        serializedCopy = Unpooled.wrappedBuffer(serializedOriginal.array());
        copy = (GradientContainer) serializer.fromBinary(serializedCopy, Optional.absent());

        Assert.assertEquals(original, copy);
    }
    
    @Test
    public void testGradientLocalView() {
        Serializer serializer = Serializers.lookupSerializer(GradientLocalView.class);
        GradientLocalView original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        original = new GradientLocalView(1, 1);
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);
        serializedCopy = Unpooled.wrappedBuffer(serializedOriginal.array());
        copy = (GradientLocalView) serializer.fromBinary(serializedCopy, Optional.absent());

        Assert.assertEquals(original, copy);
    }

    @Test
    public void testGradientShuffleRequest() {
        Serializer serializer = Serializers.lookupSerializer(GradientShuffle.Request.class);
        GradientShuffle.Request original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        GradientContainer selfGC = container3;
        Set<GradientContainer> exchangeNodes = new HashSet<GradientContainer>();
        exchangeNodes.add(container1);
        exchangeNodes.add(container2);
        original = new GradientShuffle.Request(UUID.randomUUID(), selfGC, exchangeNodes);
        
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);
        serializedCopy = Unpooled.wrappedBuffer(serializedOriginal.array());
        copy = (GradientShuffle.Request) serializer.fromBinary(serializedCopy, Optional.absent());

        Assert.assertEquals(original, copy);
    }
    
    @Test
    public void testGradientShuffleResponse() {
        Serializer serializer = Serializers.lookupSerializer(GradientShuffle.Response.class);
        GradientShuffle.Response original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        GradientContainer selfGC = container3;
        Set<GradientContainer> exchangeNodes = new HashSet<GradientContainer>();
        exchangeNodes.add(container1);
        exchangeNodes.add(container2);
        original = new GradientShuffle.Response(UUID.randomUUID(), selfGC, exchangeNodes);
        
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);
        serializedCopy = Unpooled.wrappedBuffer(serializedOriginal.array());
        copy = (GradientShuffle.Response) serializer.fromBinary(serializedCopy, Optional.absent());

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

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
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.javatuples.Pair;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.croupier.CroupierSerializerSetup;
import se.sics.ktoolbox.croupier.msg.CroupierShuffle;
import se.sics.ktoolbox.croupier.util.TestHelper.TestContent1;
import se.sics.ktoolbox.util.address.basic.BasicAddress;
import se.sics.ktoolbox.util.address.nat.Nat;
import se.sics.ktoolbox.util.address.nat.NatAwareAddressImpl;
import se.sics.ktoolbox.util.address.nat.NatType;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;
import se.sics.ktoolbox.util.update.view.View;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CroupierShuffleSerializerTest {

    @BeforeClass
    public static void setup() {
        int serializerId = 128;
        serializerId = BasicSerializerSetup.registerBasicSerializers(serializerId);
        serializerId = CroupierSerializerSetup.registerSerializers(serializerId);

        TestHelper.TestContent1Serializer testSerializer = new TestHelper.TestContent1Serializer(serializerId++);
        Serializers.register(testSerializer, "testContent1Serializer");
        Serializers.register(TestHelper.TestContent1.class, "testContent1Serializer");
    }

    private Pair<Map, Map> getShuffle() {
        InetAddress localHost;
        try {
            localHost = InetAddress.getByName("localhost");
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }

        NatAwareAddressImpl simpleAdr1 = NatAwareAddressImpl.open(new BasicAddress(localHost, 10000, 1));
        NatAwareAddressImpl simpleAdr2 = NatAwareAddressImpl.open(new BasicAddress(localHost, 10000, 2));
        NatType nat1 = NatType.nated(Nat.MappingPolicy.ENDPOINT_INDEPENDENT, Nat.AllocationPolicy.PORT_CONTIGUITY, 1,
                Nat.FilteringPolicy.ENDPOINT_INDEPENDENT, 10000);
        NatAwareAddressImpl natedAdr1 = new NatAwareAddressImpl(new BasicAddress(localHost, 20000, 2), nat1,
                localHost, 30000, new ArrayList<BasicAddress>());

        CroupierContainer container1 = new CroupierContainer(simpleAdr1, new TestContent1(1));
        CroupierContainer container2 = new CroupierContainer(simpleAdr2, new TestContent1(2));
        CroupierContainer container3 = new CroupierContainer(natedAdr1, new TestContent1(3));

        Map publicNodes = new HashMap();
        publicNodes.put(container1.src, container1);
        publicNodes.put(container2.src, container2);
        Map privateNodes = new HashMap();
        privateNodes.put(container3.src, container3);
        return Pair.with(publicNodes, privateNodes);
    }

    private void compareShuffle(CroupierShuffle.Basic original, CroupierShuffle.Basic copy) {
        Assert.assertEquals(original.id, copy.id);
        Assert.assertEquals(original.selfView, copy.selfView);
        Assert.assertEquals(original.publicNodes, copy.publicNodes);
        Assert.assertEquals(original.privateNodes, copy.privateNodes);
    }

    @Test
    public void testCroupierShuffleRequestEmptySelfView() {
        Serializer serializer = Serializers.lookupSerializer(CroupierShuffle.Request.class);
        CroupierShuffle.Request original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        Pair<Map, Map> shuffle = getShuffle();
        Optional<View> selfView = Optional.absent();
        original = new CroupierShuffle.Request(UUID.randomUUID(), selfView, shuffle.getValue0(), shuffle.getValue1());

        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);

        serializedCopy = Unpooled.buffer();
        serializedOriginal.getBytes(0, serializedCopy, serializedOriginal.readableBytes());
        copy = (CroupierShuffle.Request) serializer.fromBinary(serializedCopy, Optional.absent());

        compareShuffle(original, copy);
        Assert.assertEquals(0, serializedCopy.readableBytes());
    }

    @Test
    public void testCroupierShuffleRequestWithSelfView() {
        Serializer serializer = Serializers.lookupSerializer(CroupierShuffle.Request.class);
        CroupierShuffle.Request original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        Pair<Map, Map> shuffle = getShuffle();
        View selfView = new TestContent1(10);
        original = new CroupierShuffle.Request(UUID.randomUUID(), Optional.of(selfView), shuffle.getValue0(), shuffle.getValue1());

        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);

        serializedCopy = Unpooled.buffer();
        serializedOriginal.getBytes(0, serializedCopy, serializedOriginal.readableBytes());
        copy = (CroupierShuffle.Request) serializer.fromBinary(serializedCopy, Optional.absent());

        compareShuffle(original, copy);
        Assert.assertEquals(0, serializedCopy.readableBytes());
    }

    @Test
    public void testCroupierShuffleResponseWithSelfView() {
        Serializer serializer = Serializers.lookupSerializer(CroupierShuffle.Response.class);
        CroupierShuffle.Response original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        Pair<Map, Map> shuffle = getShuffle();
        View selfView = new TestContent1(10);
        original = new CroupierShuffle.Response(UUID.randomUUID(), Optional.of(selfView), shuffle.getValue0(), shuffle.getValue1());

        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);

        serializedCopy = Unpooled.buffer();
        serializedOriginal.getBytes(0, serializedCopy, serializedOriginal.readableBytes());
        copy = (CroupierShuffle.Response) serializer.fromBinary(serializedCopy, Optional.absent());

        compareShuffle(original, copy);
        Assert.assertEquals(0, serializedCopy.readableBytes());
    }
}
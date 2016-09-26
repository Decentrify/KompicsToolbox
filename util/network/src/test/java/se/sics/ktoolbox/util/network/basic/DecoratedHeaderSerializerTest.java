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
import se.sics.ktoolbox.util.identifiable.IdentifierRegistry;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayIdFactory;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayRegistry;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class DecoratedHeaderSerializerTest {

    @BeforeClass
    public static void setup() {
        OverlayRegistry.initiate(new OverlayId.BasicTypeFactory((byte)0), new OverlayId.BasicTypeComparator());
        BasicIdentifiers.registerDefaults(1234l);
        int serializerId = 128;
        serializerId = BasicSerializerSetup.registerBasicSerializers(serializerId);
    }

    @Test
    public void test1() throws UnknownHostException {
        Serializer serializer = Serializers.lookupSerializer(DecoratedHeader.class);
        DecoratedHeader<BasicAddress> original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        InetAddress localHost;
        try {
            localHost = InetAddress.getByName("localhost");
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
        
        IdentifierFactory nodeIdFactory = IdentifierRegistry.lookup(BasicIdentifiers.Values.NODE.toString());
        BasicAddress basicAdr1 = new BasicAddress(localHost, 10000, nodeIdFactory.randomId());
        BasicAddress basicAdr2 = new BasicAddress(localHost, 10000, nodeIdFactory.randomId());

        byte ownerId = 1;
        IdentifierFactory baseOverlayIdFactory = IdentifierRegistry.lookup(BasicIdentifiers.Values.OVERLAY.toString());
        OverlayIdFactory overlayIdFactory = new OverlayIdFactory(baseOverlayIdFactory, OverlayId.BasicTypes.CROUPIER, ownerId);
        original = new DecoratedHeader(new BasicHeader(basicAdr1, basicAdr2, Transport.UDP), overlayIdFactory.randomId());
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);

        serializedCopy = Unpooled.buffer();
        serializedOriginal.getBytes(0, serializedCopy, serializedOriginal.readableBytes());
        copy = (DecoratedHeader) serializer.fromBinary(serializedCopy, Optional.absent());

        Assert.assertEquals(original.getSource(), copy.getSource());
        Assert.assertEquals(original.getDestination(), copy.getDestination());
        Assert.assertEquals(original.getProtocol(), copy.getProtocol());
        Assert.assertEquals(original.getOverlayId(), copy.getOverlayId());
        
        Assert.assertEquals(0, serializedCopy.readableBytes());
    }
}

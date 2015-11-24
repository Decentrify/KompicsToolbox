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
package se.sics.ktoolbox.util.msg;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
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
public class RouteSerializerTest {

    @BeforeClass
    public static void setup() {
        int serializerId = 128;
        serializerId = BasicSerializerSetup.registerBasicSerializers(serializerId);
        
        AddressResolutionHelper.reset();
        AddressResolutionHelper.useNatAwareAddresses();
    }

    @Test
    public void testRouteSerializer() throws UnknownHostException {
        Serializer serializer = Serializers.lookupSerializer(Route.class);
        Route<BasicAddress> original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        InetAddress localHost;
        try {
            localHost = InetAddress.getByName("localhost");
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
        
        List<BasicAddress> route = new ArrayList<>();
        route.add(new BasicAddress(localHost, 10000, 1));
        route.add(new BasicAddress(localHost, 10000, 2));
        original = new Route(route);
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);
        
        serializedCopy = Unpooled.buffer();
        serializedOriginal.getBytes(0, serializedCopy, serializedOriginal.readableBytes());
        copy = (Route) serializer.fromBinary(serializedCopy, Optional.absent());

        Assert.assertEquals(original, copy);
        Assert.assertEquals(0, serializedCopy.readableBytes());
    }
}

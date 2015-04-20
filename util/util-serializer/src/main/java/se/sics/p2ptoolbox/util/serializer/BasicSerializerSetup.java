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

import se.sics.p2ptoolbox.util.network.impl.BasicHeaderSerializer;
import se.sics.p2ptoolbox.util.network.impl.BasicAddressSerializer;
import org.junit.Assert;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsgSerializer;
import se.sics.p2ptoolbox.util.network.impl.BasicHeader;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddressSerializer;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeaderSerializer;
import se.sics.p2ptoolbox.util.network.impl.Route;
import se.sics.p2ptoolbox.util.network.impl.RouteSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BasicSerializerSetup {

    public static final int serializerIds = 6;

    public static enum BasicSerializers {
        BasicAddress(BasicAddress.class),
        DecoratedAddress(DecoratedAddress.class),
        Route(Route.class),
        BasicHeader(BasicHeader.class),
        DecoratedHeader(DecoratedHeader.class),
        BasicContentMsg(BasicContentMsg.class);

        public final Class serializedClass;

        BasicSerializers(Class serializedClass) {
            this.serializedClass = serializedClass;
        }
    }

    public static void checkSetup() {
        for (BasicSerializers bs : BasicSerializers.values()) {
            if (Serializers.lookupSerializer(bs.serializedClass) == null) {
                throw new RuntimeException("No serializer for " + bs.serializedClass);
            }
        }
    }

    public static int registerBasicSerializers(int startingId) {
        if(startingId < 128) {
            throw new RuntimeException("start your serializer ids at 128");
        }
        int currentId = startingId;

        BasicAddressSerializer basicAddressSerializer = new BasicAddressSerializer(currentId++);
        Serializers.register(basicAddressSerializer, "basicAddressSerializer");
        Serializers.register(BasicAddress.class, "basicAddressSerializer");

        DecoratedAddressSerializer decoratedAddressSerializer = new DecoratedAddressSerializer(currentId++);
        Serializers.register(decoratedAddressSerializer, "decoratedAddressSerializer");
        Serializers.register(DecoratedAddress.class, "decoratedAddressSerializer");

        BasicHeaderSerializer basicHeaderSerializer = new BasicHeaderSerializer(currentId++);
        Serializers.register(basicHeaderSerializer, "basicHeaderSerializer");
        Serializers.register(BasicHeader.class, "basicHeaderSerializer");

        RouteSerializer routeSerializer = new RouteSerializer(currentId++);
        Serializers.register(routeSerializer, "routeSerializer");
        Serializers.register(Route.class, "routeSerializer");

        DecoratedHeaderSerializer decoratedHeaderSerializer = new DecoratedHeaderSerializer(currentId++);
        Serializers.register(decoratedHeaderSerializer, "decoratedHeaderSerializer");
        Serializers.register(DecoratedHeader.class, "decoratedHeaderSerializer");

        BasicContentMsgSerializer basicContentMsgSerializer = new BasicContentMsgSerializer(currentId++);
        Serializers.register(basicContentMsgSerializer, "basicContentMsgSerializer");
        Serializers.register(BasicContentMsg.class, "basicContentMsgSerializer");

        Assert.assertEquals(serializerIds + startingId, currentId);
        return currentId;
    }
}

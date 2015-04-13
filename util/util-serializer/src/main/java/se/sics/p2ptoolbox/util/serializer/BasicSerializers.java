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
import java.util.UUID;
import org.junit.Assert;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.BasicHeader;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddressSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BasicSerializers {

    public static final int serializerIds = 4;

    public static void registerBasicSerializers(int startingId) {
        int currentId = 0;
        UUIDSerializer uuidSerializer = new UUIDSerializer(startingId + currentId++);
        Serializers.register(uuidSerializer, "uuidSerializer");
        Serializers.register(UUID.class, "uuidSerializer");
        
        BasicAddressSerializer basicAddressSerializer = new BasicAddressSerializer(startingId + currentId++);
        Serializers.register(basicAddressSerializer, "basicAddressSerializer");
        Serializers.register(BasicAddress.class, "basicAddressSerializer");
        
        DecoratedAddressSerializer decoratedAddressSerializer = new DecoratedAddressSerializer(startingId + currentId++);
        Serializers.register(decoratedAddressSerializer, "decoratedAddressSerializer");
        Serializers.register(DecoratedAddress.class, "decoratedAddressSerializer");
        
        BasicHeaderSerializer basicHeaderSerializer = new BasicHeaderSerializer(startingId + currentId++);
        Serializers.register(basicHeaderSerializer, "basicHeaderSerializer");
        Serializers.register(BasicHeader.class, "basicHeaderSerializer");
        
        Assert.assertEquals(serializerIds, currentId);
    }
}

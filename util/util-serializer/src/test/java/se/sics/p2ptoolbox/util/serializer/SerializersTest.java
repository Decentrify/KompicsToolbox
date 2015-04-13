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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.BasicHeader;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SerializersTest {

    {
        BasicSerializers.registerBasicSerializers(0);
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
        copy = (UUID)serializer.fromBinary(serializedCopy, Optional.absent());
        
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
        copy = (BasicAddress)serializer.fromBinary(serializedCopy, Optional.absent());
        
        Assert.assertEquals(original, copy);
    }
    
    @Test
    public void testDecoratedAddressSerializer1() throws UnknownHostException {
        Serializer serializer = Serializers.lookupSerializer(DecoratedAddress.class);
        DecoratedAddress original, copy;
        ByteBuf serializedOriginal, serializedCopy;
        
        InetAddress ia = InetAddress.getByName("localhost");

        original = new DecoratedAddress(ia, 10000, 1);
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);
        serializedCopy = Unpooled.wrappedBuffer(serializedOriginal.array());
        copy = (DecoratedAddress)serializer.fromBinary(serializedCopy, Optional.absent());
        
        Assert.assertEquals(original, copy);
    }
    
    @Test
    public void testDecoratedAddressSerializer() throws UnknownHostException {
        Serializer serializer = Serializers.lookupSerializer(DecoratedAddress.class);
        DecoratedAddress original, copy;
        ByteBuf serializedOriginal, serializedCopy;
        
        InetAddress ia = InetAddress.getByName("localhost");
        
        Set<DecoratedAddress> parents = new HashSet<DecoratedAddress>();
        parents.add(new DecoratedAddress(ia, 1234, 2));
        
        original = new DecoratedAddress(ia, 10000, 1);
        original = DecoratedAddress.addNatedTrait(original, parents);
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);
        serializedCopy = Unpooled.wrappedBuffer(serializedOriginal.array());
        copy = (DecoratedAddress)serializer.fromBinary(serializedCopy, Optional.absent());
        
        Assert.assertEquals(original, copy);
    }
    
    @Test
    public void testBasicHeaderSerialize1() throws UnknownHostException {
        Serializer serializer = Serializers.lookupSerializer(BasicHeader.class);
        BasicHeader<DecoratedAddress> original, copy;
        ByteBuf serializedOriginal, serializedCopy;
        
        InetAddress ia = InetAddress.getByName("localhost");
        
        Set<DecoratedAddress> parents = new HashSet<DecoratedAddress>();
        parents.add(new DecoratedAddress(ia, 2222, 1));
        parents.add(new DecoratedAddress(ia, 2222, 2));
        parents.add(new DecoratedAddress(ia, 2222, 3));
        
        DecoratedAddress src = new DecoratedAddress(new BasicAddress(ia, 10000, 1), parents);
        DecoratedAddress dst = new DecoratedAddress(ia, 10000, 2);
        original = new BasicHeader(src, dst, Transport.UDP);
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);
        serializedCopy = Unpooled.wrappedBuffer(serializedOriginal.array());
        copy = (BasicHeader<DecoratedAddress>)serializer.fromBinary(serializedCopy, Optional.absent());
        
        Assert.assertEquals(original, copy);
    }
}

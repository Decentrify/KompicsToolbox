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
package se.sics.p2ptoolbox.chunkmanager;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.chunkmanager.util.Chunk;
import se.sics.p2ptoolbox.chunkmanager.util.ChunkPrefixHelper;
import se.sics.p2ptoolbox.util.nat.NatedTrait;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup;
import se.sics.p2ptoolbox.util.traits.AcceptedTraits;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SerializersTest {

    @BeforeClass
    public static void oneTimeSetup() {
        int currentId = 128;
        BasicSerializerSetup.registerBasicSerializers(currentId);
        currentId = currentId + BasicSerializerSetup.serializerIds;
        currentId = ChunkManagerSerializerSetup.registerSerializers(currentId);
        
        ImmutableMap acceptedTraits = ImmutableMap.of(NatedTrait.class, 0);
        DecoratedAddress.setAcceptedTraits(new AcceptedTraits(acceptedTraits));
    }

    @Before
    public void setup() {
    }

    @Test
    public void testChunk() {
        Serializer serializer = Serializers.lookupSerializer(Chunk.class);
        Chunk original, copy;
        ByteBuf serializedOriginal, serializedCopy;

        original = new Chunk(UUID.randomUUID(), 1, 4, new byte[]{1,2,3});
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);
        serializedCopy = Unpooled.wrappedBuffer(serializedOriginal.array());
        copy = (Chunk) serializer.fromBinary(serializedCopy, Optional.absent());

        Assert.assertEquals(original, copy);
    }
    
    @Test
    public void testChunkPrefixHardcode() {
        Serializer serializer = Serializers.lookupSerializer(Chunk.class);
        Chunk original, copy;
        ByteBuf serializedOriginal;

        byte[] content = new byte[]{1,2,3,4,5,6,7,8,9,10};
        original = new Chunk(UUID.randomUUID(), 1, 4, content);
        serializedOriginal = Unpooled.buffer();
        serializer.toBinary(original, serializedOriginal);
        
        Assert.assertEquals(ChunkPrefixHelper.getChunkPrefixSize() + content.length , serializedOriginal.readableBytes());
    }
}
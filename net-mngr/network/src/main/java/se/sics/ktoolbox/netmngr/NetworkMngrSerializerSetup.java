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

package se.sics.ktoolbox.netmngr;

import org.junit.Assert;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.netmngr.chunk.Chunk;
import se.sics.ktoolbox.netmngr.chunk.ChunkSerializer;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class NetworkMngrSerializerSetup {
    public static int serializerIds = 1;
    
    public static enum NetworkMngrSerializers {
        Chunk(Chunk.class, "netChunkSerializer");
        
        public final Class serializedClass;
        public final String serializerName;

        private NetworkMngrSerializers(Class serializedClass, String serializerName) {
            this.serializedClass = serializedClass;
            this.serializerName = serializerName;
        }
    }
    
    public static boolean checkSetup() {
        for (NetworkMngrSerializers cs : NetworkMngrSerializers.values()) {
            if (Serializers.lookupSerializer(cs.serializedClass) == null) {
                return false;
            }
        }
        return BasicSerializerSetup.checkSetup();
    }
    
    public static int registerSerializers(int startingId) {
        int currentId = startingId;
        
        ChunkSerializer chunkSerializer = new ChunkSerializer(currentId++);
        Serializers.register(chunkSerializer, NetworkMngrSerializers.Chunk.serializerName);
        Serializers.register(NetworkMngrSerializers.Chunk.serializedClass, NetworkMngrSerializers.Chunk.serializerName);
        
        Assert.assertEquals(serializerIds, currentId - startingId);
        return currentId;
    }
}

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
package se.sics.ktoolbox.netmngr.network;

import org.junit.Assert;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.netmngr.core.Ack;
import se.sics.ktoolbox.netmngr.core.ChunkableData;
import se.sics.ktoolbox.netmngr.core.Data;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TestSerializerSetup {

    public static int serializerIds = 3;

    public static enum TestSerializers {

        TestData(Data.class, "testDataSerializer"),
        TestCunkableData(ChunkableData.class, "testChunkableDataSerializer"),
        TestAck(Ack.class, "testAckSerializer");

        public final Class serializedClass;
        public final String serializerName;

        private TestSerializers(Class serializedClass, String serializerName) {
            this.serializedClass = serializedClass;
            this.serializerName = serializerName;
        }
    }

    public static boolean checkSetup() {
        for (TestSerializers cs : TestSerializers.values()) {
            if (Serializers.lookupSerializer(cs.serializedClass) == null) {
                return false;
            }
        }
        return true;
    }

    public static int registerSerializers(int startingId) {
        int currentId = startingId;

        DataSerializer dataSerializer = new DataSerializer(currentId++);
        Serializers.register(dataSerializer, TestSerializers.TestData.serializerName);
        Serializers.register(TestSerializers.TestData.serializedClass, TestSerializers.TestData.serializerName);
        
        ChunkableDataSerializer chunkableDataSerializer = new ChunkableDataSerializer(currentId++);
        Serializers.register(chunkableDataSerializer, TestSerializers.TestCunkableData.serializerName);
        Serializers.register(TestSerializers.TestCunkableData.serializedClass, TestSerializers.TestCunkableData.serializerName);

        AckSerializer ackSerializer = new AckSerializer(currentId++);
        Serializers.register(ackSerializer, TestSerializers.TestAck.serializerName);
        Serializers.register(TestSerializers.TestAck.serializedClass, TestSerializers.TestAck.serializerName);

        Assert.assertEquals(serializerIds, currentId - startingId);
        return currentId;
    }
}

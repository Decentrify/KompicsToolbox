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
package se.sics.ktoolbox.fd;

import org.junit.Assert;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.fd.msg.Heartbeat;
import se.sics.ktoolbox.fd.msg.HeartbeatSerializer;
import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FDSerializerSetup {
     public static int serializerIds = 1;
    
    public static enum FDSerializers {
        Heartbeat(Heartbeat.class, "heartbeatSerializer");
        
        public final Class serializedClass;
        public final String serializerName;

        private FDSerializers(Class serializedClass, String serializerName) {
            this.serializedClass = serializedClass;
            this.serializerName = serializerName;
        }
    }
    
    public static void checkSetup() {
        for (FDSerializers fds : FDSerializers.values()) {
            if (Serializers.lookupSerializer(fds.serializedClass) == null) {
                throw new RuntimeException("No serializer for " + fds.serializedClass);
            }
        }
        BasicSerializerSetup.checkSetup();
    }
    
    public static int registerSerializers(int startingId) {
        int currentId = startingId;
        
        HeartbeatSerializer heartbeatSerializer = new HeartbeatSerializer(currentId++);
        Serializers.register(heartbeatSerializer, FDSerializers.Heartbeat.serializerName);
        Serializers.register(FDSerializers.Heartbeat.serializedClass, FDSerializers.Heartbeat.serializerName);

        Assert.assertEquals(serializerIds, currentId - startingId);
        return currentId;
    }
}

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
import se.sics.ktoolbox.fd.msg.EPFDPing;
import se.sics.ktoolbox.fd.msg.EPFDPingSerializer;
import se.sics.ktoolbox.fd.msg.EPFDPong;
import se.sics.ktoolbox.fd.msg.EPFDPongSerializer;
import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class EPFDSerializerSetup {
     public static int serializerIds = 2;
    
    public static enum EPFDSerializers {
        EPFDPing(EPFDPing.class, "epfdPingSerializer"), 
        EPFDPong(EPFDPong.class, "epfdPongSerializer");
        
        public final Class serializedClass;
        public final String serializerName;

        private EPFDSerializers(Class serializedClass, String serializerName) {
            this.serializedClass = serializedClass;
            this.serializerName = serializerName;
        }
    }
    
    public static void checkSetup() {
        for (EPFDSerializers fds : EPFDSerializers.values()) {
            if (Serializers.lookupSerializer(fds.serializedClass) == null) {
                throw new RuntimeException("No serializer for " + fds.serializedClass);
            }
        }
        BasicSerializerSetup.checkSetup();
    }
    
    public static int registerSerializers(int startingId) {
        int currentId = startingId;
        
        EPFDPingSerializer epfdPingSerializer = new EPFDPingSerializer(currentId++);
        Serializers.register(epfdPingSerializer, EPFDSerializers.EPFDPing.serializerName);
        Serializers.register(EPFDSerializers.EPFDPing.serializedClass, EPFDSerializers.EPFDPing.serializerName);
        
        EPFDPongSerializer epfdPongSerializer = new EPFDPongSerializer(currentId++);
        Serializers.register(epfdPongSerializer, EPFDSerializers.EPFDPong.serializerName);
        Serializers.register(EPFDSerializers.EPFDPong.serializedClass, EPFDSerializers.EPFDPong.serializerName);

        Assert.assertEquals(serializerIds, currentId - startingId);
        return currentId;
    }
}

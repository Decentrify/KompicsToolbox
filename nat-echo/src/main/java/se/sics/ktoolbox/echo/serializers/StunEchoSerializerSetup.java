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

package se.sics.ktoolbox.echo.serializers;

import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.echo.Ping;
import se.sics.ktoolbox.echo.Pong;
import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class StunEchoSerializerSetup {
    public static int serializerIds = 2;
    
    public static enum StunEchoSerializers {
        Ping(Ping.class, "pingSerializer"),
        Pong(Pong.class, "pongSerializer");
        
        public final Class serializedClass;
        public final String serializerName;

        private StunEchoSerializers(Class serializedClass, String serializerName) {
            this.serializedClass = serializedClass;
            this.serializerName = serializerName;
        }
    }
    
    public static void checkSetup() {
        for (StunEchoSerializers gs : StunEchoSerializers.values()) {
            if (Serializers.lookupSerializer(gs.serializedClass) == null) {
                throw new RuntimeException("No serializer for " + gs.serializedClass);
            }
        }
        BasicSerializerSetup.checkSetup();
    }
    
    public static int registerSerializers(int startingId) {
        int currentId = startingId;
        
        PingSerializer pingSerializer = new PingSerializer(currentId++);
        Serializers.register(pingSerializer, StunEchoSerializers.Ping.serializerName);
        Serializers.register(StunEchoSerializers.Ping.serializedClass, StunEchoSerializers.Ping.serializerName);
        
        PongSerializer pongSerializer = new PongSerializer(currentId++);
        Serializers.register(pongSerializer, StunEchoSerializers.Pong.serializerName);
        Serializers.register(StunEchoSerializers.Pong.serializedClass, StunEchoSerializers.Pong.serializerName);
        
        assert startingId + serializerIds == currentId;
        return currentId;
    }
}

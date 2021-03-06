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
package se.sics.ktoolbox.util.proxy.example.serializer;

import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.util.proxy.example.core.ExampleEvent;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ExampleSerializerSetup {
    public static int serializerIds = 2;
    
    public static enum ExampleSerializers {
        ExampleXSerializer(ExampleEvent.X.class, "exampleXSerializer"),
        ExampleYSerializer(ExampleEvent.Y.class, "exampleYSerializer");
        
        public final Class serializedClass;
        public final String serializerName;

        private ExampleSerializers(Class serializedClass, String serializerName) {
            this.serializedClass = serializedClass;
            this.serializerName = serializerName;
        }
    }
    
    public static void checkSetup() {
        for (ExampleSerializers cs : ExampleSerializers.values()) {
            if (Serializers.lookupSerializer(cs.serializedClass) == null) {
                throw new RuntimeException("No serializer for " + cs.serializedClass);
            }
        }
    }
    
    public static int registerSerializers(int startingId) {
        int currentId = startingId;
        
        ExampleEventSerializer.X exampleXSerializer = new ExampleEventSerializer.X(currentId++);
        Serializers.register(exampleXSerializer, ExampleSerializers.ExampleXSerializer.serializerName);
        Serializers.register(ExampleSerializers.ExampleXSerializer.serializedClass, ExampleSerializers.ExampleXSerializer.serializerName);
        
        ExampleEventSerializer.Y exampleYSerializer = new ExampleEventSerializer.Y(currentId++);
        Serializers.register(exampleYSerializer, ExampleSerializers.ExampleYSerializer.serializerName);
        Serializers.register(ExampleSerializers.ExampleYSerializer.serializedClass, ExampleSerializers.ExampleYSerializer.serializerName);
        
        assert serializerIds == currentId - startingId;
        return currentId;
    }
}

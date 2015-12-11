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
package se.sics.ktoolbox.aggregator;

import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.aggregator.msg.NodeWindow;
import se.sics.ktoolbox.aggregator.msg.NodeWindowSerializer;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;

/**
 * Setup for the serializers required to communicate with the global aggregator.
 * Created by babbar on 2015-09-09.
 */
public class AggregatorSerializerSetup {

    public static enum AggregatorSerializers {

        nodeWindow(NodeWindow.class, "aggregatorNodeWindow");

        public final Class serializedClass;
        public final String serializerName;

        private AggregatorSerializers(Class serializedClass, String serializerName){
            this.serializedClass = serializedClass;
            this.serializerName = serializerName;
        }
    }

    public static void checkSetup() {

        for (AggregatorSerializers cs : AggregatorSerializers.values()) {
            if (Serializers.lookupSerializer(cs.serializedClass) == null) {
                throw new RuntimeException("No serializer for " + cs.serializedClass);
            }
        }
        BasicSerializerSetup.checkSetup();
    }

    public static int registerSerializers(int startId){

        int currentId = startId;

        NodeWindowSerializer aggregatorNodeWindow = new NodeWindowSerializer(currentId++);
        Serializers.register(aggregatorNodeWindow, AggregatorSerializers.nodeWindow.serializerName);
        Serializers.register(AggregatorSerializers.nodeWindow.serializedClass, AggregatorSerializers.nodeWindow.serializerName);

        return currentId;
    }

}

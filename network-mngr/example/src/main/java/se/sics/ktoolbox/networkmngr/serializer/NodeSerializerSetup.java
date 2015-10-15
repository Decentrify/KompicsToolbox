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
package se.sics.ktoolbox.networkmngr.serializer;

import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.networkmngr.msg.NodeMsg;
import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup.BasicSerializers;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NodeSerializerSetup {

    public static final int serializerIds = 2;

    public static enum NodeSerializers {

        Request(NodeMsg.Request.class, "nodeRequestSerializer"),
        Response(NodeMsg.Response.class, "nodeResponseSerializer");

        public final Class serializedClass;
        public final String serializerName;

        NodeSerializers(Class serializedClass, String serializerName) {
            this.serializedClass = serializedClass;
            this.serializerName = serializerName;
        }
    }

    public static void checkSetup() {
        for (BasicSerializers bs : BasicSerializers.values()) {
            if (Serializers.lookupSerializer(bs.serializedClass) == null) {
                throw new RuntimeException("No serializer for " + bs.serializedClass);
            }
        }
    }

    public static int registerSerializers(int startingId) {
        if (startingId < 128) {
            throw new RuntimeException("start your serializer ids at 128");
        }
        int currentId = startingId;

        NodeMsgSerializer.Request nodeRequestSerializer = new NodeMsgSerializer.Request(currentId++);
        Serializers.register(nodeRequestSerializer, NodeSerializers.Request.serializerName);
        Serializers.register(NodeSerializers.Request.serializedClass, NodeSerializers.Request.serializerName);

        NodeMsgSerializer.Response nodeResponseSerializer = new NodeMsgSerializer.Response(currentId++);
        Serializers.register(nodeResponseSerializer, NodeSerializers.Response.serializerName);
        Serializers.register(NodeSerializers.Response.serializedClass, NodeSerializers.Response.serializerName);

        assert startingId + serializerIds == currentId;

        return currentId;
    }
}

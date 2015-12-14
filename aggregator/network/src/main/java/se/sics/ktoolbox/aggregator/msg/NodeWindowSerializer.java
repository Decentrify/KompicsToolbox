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
package se.sics.ktoolbox.aggregator.msg;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.aggregator.util.AggregatorPacket;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 * Serializer for the packet container mainly containing the packet information.
 * Created by babbar on 2015-09-07.
 */
public class NodeWindowSerializer implements Serializer {

    private final int id;

    public NodeWindowSerializer(int id) {
        this.id = id;
    }

    @Override
    public int identifier() {
        return this.id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        NodeWindow window = (NodeWindow) o;
        Serializers.toBinary(window.id, buf);
        buf.writeInt(window.window.size());
        for (Map.Entry<Class, AggregatorPacket> e : window.window.entrySet()) {
            buf.writeInt(e.getKey().getName().length());
            buf.writeBytes(e.getKey().getName().getBytes());
            Serializers.toBinary(e.getValue(), buf);
        }
    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        Identifier windowId = (Identifier) Serializers.fromBinary(buf, hint);
        int windowSize = buf.readInt();
        Map<Class, AggregatorPacket> window = new HashMap<>();
        while (windowSize > 0) {
            windowSize--;
            int nameSize = buf.readInt();
            byte[] bName = new byte[nameSize];
            Class c;
            try {
                c = Class.forName(new String(bName));
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
            AggregatorPacket ap = (AggregatorPacket) Serializers.fromBinary(buf, hint);
            window.put(c, ap);
        }
        return new NodeWindow(windowId, window);
    }
}

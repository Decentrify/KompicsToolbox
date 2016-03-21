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

package se.sics.ktoolbox.gradient.msg;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.javatuples.Quartet;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.gradient.util.GradientContainer;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class GradientShuffleSerializer {
    public abstract static class Basic implements Serializer {
        private final int id;
        
        Basic(int id) {
            this.id = id;
        }
        
        @Override
        public int identifier() {
            return id;
        }

        @Override
        public void toBinary(Object o, ByteBuf buf) {
            GradientShuffle.Basic obj = (GradientShuffle.Basic)o;
            Serializers.toBinary(obj.getId(), buf);
            Serializers.toBinary(obj.overlayId(), buf);
            
            Serializers.lookupSerializer(GradientContainer.class).toBinary(obj.selfGC, buf);
            buf.writeByte(obj.exchangeGC.size());
            for(GradientContainer cc : obj.exchangeGC) {
                Serializers.lookupSerializer(GradientContainer.class).toBinary(cc, buf);
            }
        }

        public Quartet<Identifier, Identifier, GradientContainer, List<GradientContainer>> fromBinaryBase(ByteBuf buf, Optional<Object> hint) {
            Identifier eventId = (Identifier)Serializers.fromBinary(buf, hint);
            Identifier overlayId = (Identifier)Serializers.fromBinary(buf, hint);
            
            GradientContainer selfGC = (GradientContainer)Serializers.lookupSerializer(GradientContainer.class).fromBinary(buf, hint);
            int exchangedNodesSize = buf.readByte();
            List<GradientContainer> exchangedNodes = new ArrayList<>(); 
            while(exchangedNodesSize > 0) {
                GradientContainer cc = (GradientContainer)Serializers.lookupSerializer(GradientContainer.class).fromBinary(buf, hint);
                exchangedNodes.add(cc);
                exchangedNodesSize--;
            }
            
            return Quartet.with(eventId, overlayId, selfGC, exchangedNodes);
        }
    }
    
    public static class Request extends Basic {

        public Request(int id) {
            super(id);
        }
        
        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            Quartet<Identifier, Identifier, GradientContainer, List<GradientContainer>> contents = fromBinaryBase(buf, hint);
            return new GradientShuffle.Request(contents.getValue0(), contents.getValue1(), 
                    contents.getValue2(), contents.getValue3());
        }
    }
    
    public static class Response extends Basic {

        public Response(int id) {
            super(id);
        }
        
        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            Quartet<Identifier, Identifier, GradientContainer, List<GradientContainer>> contents = fromBinaryBase(buf, hint);
            return new GradientShuffle.Response(contents.getValue0(), contents.getValue1(), 
                    contents.getValue2(), contents.getValue3());
        }
    }
}

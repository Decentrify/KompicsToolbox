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

package se.sics.p2ptoolbox.gradient.msg;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.javatuples.Triplet;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.gradient.util.GradientContainer;

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
            Serializers.lookupSerializer(UUID.class).toBinary(obj.getId(), buf);
            
            Serializers.lookupSerializer(GradientContainer.class).toBinary(obj.selfGC, buf);
            
            buf.writeByte(obj.exchangeNodes.size());
            for(GradientContainer cc : obj.exchangeNodes) {
                Serializers.lookupSerializer(GradientContainer.class).toBinary(cc, buf);
            }
        }

        public Triplet<UUID, GradientContainer, Set<GradientContainer>> fromBinaryBase(ByteBuf buf, Optional<Object> hint) {
            UUID id = (UUID)Serializers.lookupSerializer(UUID.class).fromBinary(buf, hint);
            
            GradientContainer selfGC = (GradientContainer)Serializers.lookupSerializer(GradientContainer.class).fromBinary(buf, hint);
            
            int exchangedNodesSize = buf.readByte();
            Set<GradientContainer> exchangedNodes = new HashSet<GradientContainer>(); 
            while(exchangedNodesSize > 0) {
                GradientContainer cc = (GradientContainer)Serializers.lookupSerializer(GradientContainer.class).fromBinary(buf, hint);
                exchangedNodes.add(cc);
                exchangedNodesSize--;
            }
            
            return Triplet.with(id, selfGC, exchangedNodes);
        }
    }
    
    public static class Request extends Basic {

        public Request(int id) {
            super(id);
        }
        
        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            Triplet<UUID, GradientContainer, Set<GradientContainer>> contents = fromBinaryBase(buf, hint);
            return new GradientShuffle.Request(contents.getValue0(), contents.getValue1(), contents.getValue2());
        }
    }
    
    public static class Response extends Basic {

        public Response(int id) {
            super(id);
        }
        
        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            Triplet<UUID, GradientContainer, Set<GradientContainer>> contents = fromBinaryBase(buf, hint);
            return new GradientShuffle.Response(contents.getValue0(), contents.getValue1(), contents.getValue2());
        }
    }
}
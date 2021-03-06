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
package se.sics.ktoolbox.croupier.msg;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import org.javatuples.Quintet;
import se.sics.kompics.util.Identifier;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.croupier.util.CroupierContainer;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.update.View;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierShuffleSerializer {

    public abstract static class Basic implements Serializer {

        private final int id;
        private final Class msgIdType;

        Basic(int id) {
            this.id = id;
            this.msgIdType = BasicIdentifiers.msgIdType();
        }

        @Override
        public int identifier() {
            return id;
        }

        @Override
        public void toBinary(Object o, ByteBuf buf) {
            CroupierShuffle.Basic obj = (CroupierShuffle.Basic) o;
            Serializers.lookupSerializer(msgIdType).toBinary(obj.msgId, buf);
            Serializers.lookupSerializer(OverlayId.class).toBinary(obj.overlayId, buf);

            if(obj.selfView.isPresent()) {
                buf.writeBoolean(true);
                Serializers.toBinary(obj.selfView.get(), buf);
            } else {
                buf.writeBoolean(false);
            }
            buf.writeByte(obj.publicNodes.size());
            for (CroupierContainer cc : obj.publicNodes.values()) {
                Serializers.lookupSerializer(CroupierContainer.class).toBinary(cc, buf);
            }

            buf.writeByte(obj.privateNodes.size());
            for (CroupierContainer cc : obj.privateNodes.values()) {
                Serializers.lookupSerializer(CroupierContainer.class).toBinary(cc, buf);
            }
        }

        public Quintet<Identifier, OverlayId, Optional<View>, Map, Map> fromBinaryBase(ByteBuf buf, Optional<Object> hint) {
            Identifier msgId = (Identifier)Serializers.lookupSerializer(msgIdType).fromBinary(buf, hint);
            OverlayId overlayId = (OverlayId)Serializers.lookupSerializer(OverlayId.class).fromBinary(buf, hint);
            
            Optional<View> selfView;
            if(buf.readBoolean()) {
                selfView = Optional.of((View)Serializers.fromBinary(buf, hint));
            } else {
                selfView = Optional.absent();
            }
            int publicNodesSize = buf.readByte();
            Map publicNodes = new HashMap();
            for (int i = 0; i < publicNodesSize; i++) {
                CroupierContainer cc = (CroupierContainer) Serializers.lookupSerializer(CroupierContainer.class).fromBinary(buf, hint);
                publicNodes.put(cc.getSource().getId(), cc);
            }

            int privateNodesSize = buf.readByte();
            Map privateNodes = new HashMap();
            for (int i = 0; i < privateNodesSize; i++) {
                CroupierContainer cc = (CroupierContainer) Serializers.lookupSerializer(CroupierContainer.class).fromBinary(buf, hint);
                privateNodes.put(cc.getSource().getId(), cc);
            }
            return Quintet.with(msgId, overlayId, selfView, publicNodes, privateNodes);
        }
    }

    public static class Request extends Basic {

        public Request(int id) {
            super(id);
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            Quintet<Identifier, OverlayId, Optional<View>, Map, Map> contents = fromBinaryBase(buf, hint);
            return new CroupierShuffle.Request(contents.getValue0(), contents.getValue1(), 
                    contents.getValue2(), contents.getValue3(), contents.getValue4());
        }
    }

    public static class Response extends Basic {

        public Response(int id) {
            super(id);
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            Quintet<Identifier, OverlayId, Optional<View>, Map, Map> contents = fromBinaryBase(buf, hint);
            return new CroupierShuffle.Response(contents.getValue0(), contents.getValue1(), 
                    contents.getValue2(), contents.getValue3(), contents.getValue4());
        }
    }
}

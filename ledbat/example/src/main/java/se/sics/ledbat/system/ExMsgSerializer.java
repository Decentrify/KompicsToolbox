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
package se.sics.ledbat.system;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ExMsgSerializer {

    public static class Request implements Serializer {

        private final int id;

        public Request(int id) {
            this.id = id;
        }

        @Override
        public int identifier() {
            return id;
        }

        @Override
        public void toBinary(Object o, ByteBuf buf) {
            ExMsg.Request obj = (ExMsg.Request) o;
            Serializers.lookupSerializer(UUID.class).toBinary(obj.eventId, buf);
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            UUID eventId = (UUID) Serializers.lookupSerializer(UUID.class).fromBinary(buf, hint);
            return new ExMsg.Request(eventId);
        }
    }

    public static class Response implements Serializer {

        private final int id;

        public Response(int id) {
            this.id = id;
        }

        @Override
        public int identifier() {
            return id;
        }

        @Override
        public void toBinary(Object o, ByteBuf buf) {
            ExMsg.Response obj = (ExMsg.Response) o;
            Serializers.lookupSerializer(UUID.class).toBinary(obj.eventId, buf);
            buf.writeInt(obj.payload.length);
            buf.writeBytes(obj.payload);
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            UUID eventId = (UUID) Serializers.lookupSerializer(UUID.class).fromBinary(buf, hint);
            int payloadSize = buf.readInt();
            byte[] payload = new byte[payloadSize];
            buf.readBytes(payload);
            return new ExMsg.Response(eventId, payload);
        }
    }
}

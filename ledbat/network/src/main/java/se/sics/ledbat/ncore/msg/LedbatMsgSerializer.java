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
package se.sics.ledbat.ncore.msg;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.util.overlays.OverlayEvent;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LedbatMsgSerializer {
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
            LedbatMsg.Request obj = (LedbatMsg.Request)o;
            buf.writeLong(obj.leecherAppReqSendT);
            Serializers.toBinary(obj.content, buf);
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            long leecherAppReqSendT = buf.readLong();
            OverlayEvent payload = (OverlayEvent)Serializers.fromBinary(buf, hint);
            return new LedbatMsg.Request(payload, leecherAppReqSendT);
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
            LedbatMsg.Response obj = (LedbatMsg.Response)o;
            buf.writeLong(obj.leecherAppReqSendT);
            buf.writeLong(System.currentTimeMillis());
            Serializers.toBinary(obj.content, buf);
        }

        @Override
        public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
            long leecherAppReqSendT = buf.readLong();
            long seederNetRespSendT = buf.readLong();
            OverlayEvent payload = (OverlayEvent)Serializers.fromBinary(buf, hint);
            return new LedbatMsg.Response(payload, leecherAppReqSendT, seederNetRespSendT, System.currentTimeMillis());
        }
    }
}

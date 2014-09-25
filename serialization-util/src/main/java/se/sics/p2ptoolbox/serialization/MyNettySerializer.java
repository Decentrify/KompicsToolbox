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
package se.sics.p2ptoolbox.serialization;

import se.sics.p2ptoolbox.serialization.api.Serializer;
import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.common.msgs.DirectMsgNettyFactory;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.net.msgs.DirectMsg;
import se.sics.p2ptoolbox.serialization.api.BaseCategories;
import se.sics.p2ptoolbox.serialization.api.Payload;
import se.sics.p2ptoolbox.serialization.api.SerializationContext;
import se.sics.p2ptoolbox.serialization.api.Serializer.SerializerException;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class MyNettySerializer {

    private static SerializationContext context;

    public static void setSerializationContext(SerializationContext setContext) {
        context = setContext;
    }

    public static class Request extends DirectMsgNettyFactory.Request {

        @Override
        protected DirectMsg process(ByteBuf buffer) throws MessageDecodingException {
            try {
                byte opCode = buffer.readByte();
                Class<? extends Payload> serializedClass = (Class<? extends Payload>)context.getSerializedClass(BaseCategories.PAYLOAD, opCode);
                Serializer<? extends Payload> currentSerializer = context.getSerializer(serializedClass);
                Payload payload = currentSerializer.decode(context, buffer);
                return new MyNettyMsg.Request<Payload>(vodSrc, vodDest, payload);
            } catch (SerializerException ex) {
                throw new MessageDecodingException(ex);
            }
        }
        
        public DirectMsgNetty.Request myDecode(ByteBuf buffer) throws MessageDecodingException {
            return (DirectMsgNetty.Request)decode(buffer);
        } 
    }

    public static class Response extends DirectMsgNettyFactory.Response {
        @Override
        protected DirectMsg process(ByteBuf buffer) throws MessageDecodingException {
            try {
                byte opCode = buffer.readByte();
                Class<? extends Payload> serializedClass = (Class<? extends Payload>) context.getSerializedClass(BaseCategories.PAYLOAD, opCode);
                Serializer<? extends Payload> currentSerializer = context.getSerializer(serializedClass);
                Payload payload = currentSerializer.decode(context, buffer);
                return new MyNettyMsg.Response<Payload>(vodSrc, vodDest, payload);
            } catch (SerializerException ex) {
                throw new MessageDecodingException(ex);
            }
        }
        
        public DirectMsgNetty.Response myDecode(ByteBuf buffer) throws MessageDecodingException {
            return (DirectMsgNetty.Response)decode(buffer);
        } 
    }

    public static class OneWay extends DirectMsgNettyFactory.Oneway {
        @Override
        protected DirectMsg process(ByteBuf buffer) throws MessageDecodingException {
            try {
                byte opCode = buffer.readByte();
                Class<? extends Payload> serializedClass = (Class<? extends Payload>) context.getSerializedClass(BaseCategories.PAYLOAD, opCode);
                Serializer<? extends Payload> currentSerializer = context.getSerializer(serializedClass);
                Payload payload = currentSerializer.decode(context, buffer);
                return new MyNettyMsg.OneWay<Payload>(vodSrc, vodDest, payload);
            } catch (SerializerException ex) {
                throw new MessageDecodingException(ex);
            }
        }
        
        public DirectMsgNetty.Oneway myDecode(ByteBuf buffer) throws MessageDecodingException {
            return (DirectMsgNetty.Oneway)decode(buffer);
        }
    }
}

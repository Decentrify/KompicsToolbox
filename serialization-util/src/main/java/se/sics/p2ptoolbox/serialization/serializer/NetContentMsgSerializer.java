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
package se.sics.p2ptoolbox.serialization.serializer;

import io.netty.buffer.ByteBuf;
import se.sics.p2ptoolbox.serialization.SerializationContext;
import se.sics.p2ptoolbox.serialization.Serializer;
import se.sics.p2ptoolbox.serialization.Serializer.SerializerException;
import se.sics.p2ptoolbox.serialization.msg.NetContentMsg;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class NetContentMsgSerializer {

    public static abstract class Request<E extends NetContentMsg.Request> extends NetMsgSerializer.AbsRequest<E> {

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, E obj) throws SerializerException, SerializationContext.MissingException {
            encodeAbsRequest(context, buf, obj);
            Serializer serializer = context.getSerializer(obj.content.getClass());
            serializer.encode(context, buf, obj.content);
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, E obj) throws SerializerException, SerializationContext.MissingException {
            int size = sizeAbsRequest(context, obj);
            Serializer serializer = context.getSerializer(obj.content.getClass());
            size += serializer.getSize(context, obj.content);
            return size;
        }
    }
    
    public static abstract class Response<E extends NetContentMsg.Response> extends NetMsgSerializer.AbsResponse<E> {

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, E obj) throws SerializerException, SerializationContext.MissingException {
            encodeAbsResponse(context, buf, obj);
            Serializer serializer = context.getSerializer(obj.content.getClass());
            serializer.encode(context, buf, obj.content);
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, E obj) throws SerializerException, SerializationContext.MissingException {
            int size = sizeAbsResponse(context, obj);
            Serializer serializer = context.getSerializer(obj.content.getClass());
            size += serializer.getSize(context, obj.content);
            return size;
        }
    }
    
    public static abstract class OneWay<E extends NetContentMsg.OneWay> extends NetMsgSerializer.AbsOneWay<E> {

        @Override
        public ByteBuf encode(SerializationContext context, ByteBuf buf, E obj) throws SerializerException, SerializationContext.MissingException {
            encodeAbsOneWay(context, buf, obj);
            Serializer serializer = context.getSerializer(obj.content.getClass());
            serializer.encode(context, buf, obj.content);
            return buf;
        }

        @Override
        public int getSize(SerializationContext context, E obj) throws SerializerException, SerializationContext.MissingException {
            int size = sizeAbsOneWay(context, obj);
            Serializer serializer = context.getSerializer(obj.content.getClass());
            size += serializer.getSize(context, obj.content);
            return size;
        }
    }
}

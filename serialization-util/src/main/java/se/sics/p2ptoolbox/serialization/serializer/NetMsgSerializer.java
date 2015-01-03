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
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.javatuples.Pair;
import se.sics.gvod.address.Address;
import se.sics.p2ptoolbox.serialization.SerializationContext;
import se.sics.p2ptoolbox.serialization.Serializer;
import se.sics.p2ptoolbox.serialization.msg.HeaderField;
import se.sics.p2ptoolbox.serialization.msg.NetMsg;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class NetMsgSerializer {

    private static ByteBuf encodeHeader(SerializationContext context, ByteBuf buf, Map<String, HeaderField> header) throws SerializationContext.MissingException, Serializer.SerializerException {
        if (header == null) {
            buf.writeInt(-1);
            return buf;
        }
        buf.writeInt(header.size());
        for (Map.Entry<String, HeaderField> e : header.entrySet()) {
            byte[] bHeaderName = e.getKey().getBytes();
            buf.writeInt(bHeaderName.length);
            buf.writeBytes(bHeaderName);
            Pair<Byte, Byte> headerCode = context.getCode(e.getValue().getClass());
            buf.writeByte(headerCode.getValue0());
            buf.writeByte(headerCode.getValue1());
            Serializer serializer = context.getSerializer(e.getValue().getClass());
            serializer.encode(context, buf, e.getValue());
        }
        return buf;
    }

    private static Map<String, HeaderField> decodeHeader(SerializationContext context, ByteBuf buf) throws SerializationContext.MissingException, Serializer.SerializerException {
        int headerSize = buf.readInt();
        if (headerSize == -1) {
            return null;
        }
        Map<String, HeaderField> header = new HashMap<String, HeaderField>();
        for (int i = 0; i < headerSize; i++) {
            int hnSize = buf.readInt();
            byte[] bHeaderName = new byte[hnSize];
            buf.readBytes(bHeaderName);
            byte aliasCode = buf.readByte();
            byte multiplexCode = buf.readByte();
            Serializer<HeaderField> serializer = context.getSerializer(HeaderField.class, aliasCode, multiplexCode);
            HeaderField headerField = serializer.decode(context, buf);
            header.put(new String(bHeaderName), headerField);
        }
        return header;
    }

    private static int sizeHeader(SerializationContext context, Map<String, HeaderField> header) throws SerializationContext.MissingException, Serializer.SerializerException {
        int size = 0;
        size += Integer.SIZE / 8; //size
        if (header != null) {
            for (Map.Entry<String, HeaderField> e : header.entrySet()) {
                size += Integer.SIZE / 8; //name byte size
                size += e.getKey().getBytes().length;
                size += 2 * Byte.SIZE / 8; //alias + multiplex code
                Serializer serializer = context.getSerializer(e.getValue().getClass());
                size += serializer.getSize(context, e.getValue());
            }
        }
        return size;
    }

    public static abstract class AbsRequest<E extends NetMsg.Request> implements Serializer<E> {

        protected static Address dummyHack;
        static {
            try {
                dummyHack = new Address(Inet4Address.getLocalHost(), 11111, -1);
            } catch (UnknownHostException ex) {
                throw new RuntimeException(ex);
            }
        }

        protected Pair<UUID, Map<String, HeaderField>> decodeAbsRequest(SerializationContext context, ByteBuf buf) throws Serializer.SerializerException, SerializationContext.MissingException {
            UUID id = context.getSerializer(UUID.class).decode(context, buf);
            Map<String, HeaderField> header = decodeHeader(context, buf);
            return Pair.with(id, header);
        }

        protected ByteBuf encodeAbsRequest(SerializationContext context, ByteBuf buf, E obj) throws Serializer.SerializerException, SerializationContext.MissingException {
            context.getSerializer(UUID.class).encode(context, buf, obj.id);
            encodeHeader(context, buf, obj.header);
            return buf;
        }

        protected int sizeAbsRequest(SerializationContext context, E obj) throws SerializerException, SerializationContext.MissingException {
            int size = 0;
            size += context.getSerializer(UUID.class).getSize(context, obj.id);
            size += sizeHeader(context, obj.header);
            return size;
        }
    }

    public static abstract class AbsResponse<E extends NetMsg.Response> implements Serializer<E> {
        protected static Address dummyHack;
        static {
            try {
                dummyHack = new Address(Inet4Address.getLocalHost(), 11111, -1);
            } catch (UnknownHostException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        protected Pair<UUID, Map<String, HeaderField>> decodeAbsResponse(SerializationContext context, ByteBuf buf) throws Serializer.SerializerException, SerializationContext.MissingException {
            UUID id = context.getSerializer(UUID.class).decode(context, buf);
            Map<String, HeaderField> header = decodeHeader(context, buf);
            return Pair.with(id, header);
        }

        protected ByteBuf encodeAbsResponse(SerializationContext context, ByteBuf buf, E obj) throws Serializer.SerializerException, SerializationContext.MissingException {
            context.getSerializer(UUID.class).encode(context, buf, obj.id);
            encodeHeader(context, buf, obj.header);
            return buf;
        }

        protected int sizeAbsResponse(SerializationContext context, E obj) throws SerializerException, SerializationContext.MissingException {
            int size = 0;
            size += context.getSerializer(UUID.class).getSize(context, obj.id);
            size += sizeHeader(context, obj.header);
            return size;
        }
    }

    public static abstract class AbsOneWay<E extends NetMsg.OneWay> implements Serializer<E> {
        protected static Address dummyHack;
        static {
            try {
                dummyHack = new Address(Inet4Address.getLocalHost(), 11111, -1);
            } catch (UnknownHostException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        protected Pair<UUID, Map<String, HeaderField>> decodeAbsOneWay(SerializationContext context, ByteBuf buf) throws Serializer.SerializerException, SerializationContext.MissingException {
            UUID id = context.getSerializer(UUID.class).decode(context, buf);
            Map<String, HeaderField> header = decodeHeader(context, buf);
            return Pair.with(id, header);
        }

        protected ByteBuf encodeAbsOneWay(SerializationContext context, ByteBuf buf, E obj) throws Serializer.SerializerException, SerializationContext.MissingException {
            context.getSerializer(UUID.class).encode(context, buf, obj.id);
            encodeHeader(context, buf, obj.header);
            return buf;
        }

        protected int sizeAbsOneWay(SerializationContext context, E obj) throws SerializerException, SerializationContext.MissingException {
            int size = 0;
            size += context.getSerializer(UUID.class).getSize(context, obj.id);
            size += sizeHeader(context, obj.header);
            return size;
        }
    }
}

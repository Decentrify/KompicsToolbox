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
package se.sics.p2ptoolbox.nettytest.msgserializer;

import io.netty.buffer.ByteBuf;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import se.sics.p2ptoolbox.nettytest.msg.MsgA;
import se.sics.p2ptoolbox.serialization.api.BaseCategories;
import se.sics.p2ptoolbox.serialization.api.SerializationContext;
import se.sics.p2ptoolbox.serialization.api.SerializationContext.CatMarker;
import se.sics.p2ptoolbox.serialization.api.Serializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class MsgASerializer {

    public static class Request implements Serializer<MsgA.Request> {

        private final static CatMarker category = BaseCategories.PAYLOAD;

        @Override

        public void encode(SerializationContext context, ByteBuf buf, MsgA.Request req) throws SerializerException {
            buf.writeByte(context.getOpcode(category, MsgA.Request.class));
            context.getSerializer(UUID.class).encode(context, buf, req.id);
            buf.writeInt(req.a);
        }

        @Override
        public MsgA.Request decode(SerializationContext context, ByteBuf buf) throws SerializerException {
            UUID id = context.getSerializer(UUID.class).decode(context, buf);
            int a = buf.readInt();

            return new MsgA.Request(id, a);
        }

        @Override
        public int getSize(SerializationContext context, MsgA.Request req) throws SerializerException {
            int size = 0;
            size += 1; //opcode
            size += context.getSerializer(UUID.class).getSize(context, req.id);
            size += 4; //overlayId;
            return size;
        }
    }

    public static class Response implements Serializer<MsgA.Response> {

        private final static CatMarker category = BaseCategories.PAYLOAD;

        @Override
        public void encode(SerializationContext context, ByteBuf buf, MsgA.Response resp) throws SerializerException {
            buf.writeByte(context.getOpcode(category, MsgA.Response.class));
            context.getSerializer(UUID.class).encode(context, buf, resp.id);

            buf.writeInt(resp.b.size());
            for (String val : resp.b) {
                try {
                    byte[] bval = val.getBytes("UTF-8");
                    buf.writeInt(bval.length);
                    buf.writeBytes(bval);
                } catch (UnsupportedEncodingException ex) {
                    throw new SerializerException(ex);
                }
            }
        }

        @Override
        public MsgA.Response decode(SerializationContext context, ByteBuf buf) throws SerializerException {
            UUID id = context.getSerializer(UUID.class).decode(context, buf);
            Set<String> b = new HashSet<String>();
            int bsize = buf.readInt();

            for (int i = 0; i < bsize; i++) {
                try {
                    int valSize = buf.readInt();
                    byte[] valByte = new byte[valSize];
                    buf.readBytes(valByte);
                    b.add(new String(valByte, "UTF-8"));
                } catch (UnsupportedEncodingException ex) {
                    throw new SerializerException(ex);
                }
            }
            
            return new MsgA.Response(id, b);
        }

        @Override
        public int getSize(SerializationContext context, MsgA.Response resp) throws SerializerException {
            int size = 0;
            size += 1; //opcode
            size += context.getSerializer(UUID.class).getSize(context, resp.id);
            size += 4; //bsize
            for (String val : resp.b) {
                try {
                    size += 4; // byte length
                    size += val.getBytes("UTF-8").length;
                } catch (UnsupportedEncodingException ex) {
                    throw new SerializerException(ex);
                }
            }
            return size;
        }
    }
}

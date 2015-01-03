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
package se.sics.p2ptoolbox.serialization.msg;

import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.UUID;
import org.javatuples.Pair;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.net.VodAddress;
import se.sics.p2ptoolbox.serialization.SerializationContext;
import se.sics.p2ptoolbox.serialization.Serializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class NetContentMsg {

    public static abstract class Request<E extends Object> extends NetMsg.Request {

        public final E content;

        public Request(VodAddress src, VodAddress dest, UUID id, E content) {
            super(src, dest, id);
            this.content = content;
        }

        public Request(VodAddress src, VodAddress dest, UUID id, Map<String, HeaderField> header, E content) {
            super(src, dest, id, header);
            this.content = content;
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            try {
                ByteBuf buf = createChannelBufferWithHeader();
                Pair<Byte, Byte> code = context.getCode(this.getClass());
                buf.writeByte(code.getValue0());
                buf.writeByte(code.getValue1());
                Serializer serializer = context.getSerializer(this.getClass());
                serializer.encode(context, buf, this);
                return buf;
            } catch (SerializationContext.MissingException ex) {
                throw new MessageEncodingException("missing serializer for " + this.getClass());
            } catch (Serializer.SerializerException ex) {
                throw new MessageEncodingException("cannot properly serializer " + this.getClass() + " check serializer");
            }
        }

        @Override
        public int getSize() {
            try {
                int size = super.getSize();
                size += 2 * Byte.SIZE / 8; //alias code + multiplex code
                Serializer serializer = context.getSerializer(this.getClass());
                size += serializer.getSize(context, this);
                return size;
            } catch (SerializationContext.MissingException ex) {
                throw new RuntimeException(ex);
            } catch (Serializer.SerializerException ex) {
                throw new RuntimeException(ex);
            }
        }

    }

    public static abstract class Response<E extends Object> extends NetMsg.Response {

        public final E content;

        public Response(VodAddress src, VodAddress dest, UUID id, E content) {
            super(src, dest, id);
            this.content = content;
        }

        public Response(VodAddress src, VodAddress dest, UUID id, Map<String, HeaderField> header, E content) {
            super(src, dest, id, header);
            this.content = content;
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            try {
                ByteBuf buf = createChannelBufferWithHeader();
                Pair<Byte, Byte> code = context.getCode(this.getClass());
                buf.writeByte(code.getValue0());
                buf.writeByte(code.getValue1());
                Serializer serializer = context.getSerializer(this.getClass());
                serializer.encode(context, buf, this);
                return buf;
            } catch (SerializationContext.MissingException ex) {
                throw new MessageEncodingException("missing serializer for " + this.getClass());
            } catch (Serializer.SerializerException ex) {
                throw new MessageEncodingException("cannot properly serializer " + this.getClass() + " check serializer");
            }
        }

        @Override
        public int getSize() {
            try {
                int size = super.getSize();
                size += 2 * Byte.SIZE / 8; //alias code + multiplex code
                Serializer serializer = context.getSerializer(this.getClass());
                size += serializer.getSize(context, this);
                return size;
            } catch (SerializationContext.MissingException ex) {
                throw new RuntimeException(ex);
            } catch (Serializer.SerializerException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static abstract class OneWay<E extends Object> extends NetMsg.OneWay {

        public final E content;

        public OneWay(VodAddress src, VodAddress dest, UUID id, E content) {
            super(src, dest, id);
            this.content = content;
        }

        public OneWay(VodAddress src, VodAddress dest, UUID id, Map<String, HeaderField> header, E content) {
            super(src, dest, id, header);
            this.content = content;
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            try {
                ByteBuf buf = createChannelBufferWithHeader();
                Pair<Byte, Byte> code = context.getCode(this.getClass());
                buf.writeByte(code.getValue0());
                buf.writeByte(code.getValue1());
                Serializer serializer = context.getSerializer(this.getClass());
                serializer.encode(context, buf, this);
                return buf;
            } catch (SerializationContext.MissingException ex) {
                throw new MessageEncodingException("missing serializer for " + this.getClass());
            } catch (Serializer.SerializerException ex) {
                throw new MessageEncodingException("cannot properly serializer " + this.getClass() + " check serializer");
            }
        }

        @Override
        public int getSize() {
            try {
                int size = super.getSize();
                size += 2 * Byte.SIZE / 8; //alias code + multiplex code
                Serializer serializer = context.getSerializer(this.getClass());
                size += serializer.getSize(context, this);
                return size;
            } catch (SerializationContext.MissingException ex) {
                throw new RuntimeException(ex);
            } catch (Serializer.SerializerException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}

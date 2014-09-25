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

import se.sics.p2ptoolbox.serialization.api.Payload;
import se.sics.p2ptoolbox.serialization.api.Serializer;
import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.p2ptoolbox.serialization.api.BaseCategories;
import se.sics.p2ptoolbox.serialization.api.SerializationContext;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class MyNettyMsg {
    private static SerializationContext context;
    
    public static void setContext(SerializationContext setContext) {
        context = setContext;
    }
    
    public static class Request<E extends Payload> extends DirectMsgNetty.Request {
        
        public final E payload;
        
        public Request(VodAddress src, VodAddress dest, E payload) {
            super(src, dest);

            //TODO ALEX fix later
            setTimeoutId(se.sics.gvod.timer.UUID.nextUUID());
            //fix
            
            this.payload = payload;
        }
        
        @Override 
        public String toString() {
            return payload.toString() + " src " + vodSrc.getPeerAddress().toString() + " dest " + vodDest.getPeerAddress().toString();
        }
        
        @Override
        public int getSize() {
            try {
                Serializer serializer = context.getSerializer(payload.getClass());
                return getHeaderSize() + serializer.getSize(context, payload);
            } catch (Serializer.SerializerException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            Serializer serializer = context.getSerializer(payload.getClass());
            try {
                serializer.encode(context, buffer, payload);
            } catch (Serializer.SerializerException ex) {
                throw new MessageEncodingException("serialization exception");
            }
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return context.getOpcode(BaseCategories.NETWORK, this.getClass());
        }

        @Override
        public RewriteableMsg copy() {
            return new Request(vodSrc, vodDest, payload);
        }
        
    }
    
    public static class Response<E extends Payload> extends DirectMsgNetty.Response {
        public final E payload;
        
        public Response(VodAddress src, VodAddress dest, E payload) {
            super(src, dest);
            
            //TODO ALEX fix later
            setTimeoutId(se.sics.gvod.timer.UUID.nextUUID());
            //fix
            
            this.payload = payload;
        }
        
        @Override 
        public String toString() {
            return payload.toString() + " src " + vodSrc.getPeerAddress().toString() + " dest " + vodDest.getPeerAddress().toString();
        }
        
        @Override
        public int getSize() {
            try {
                Serializer serializer = context.getSerializer(payload.getClass());
                return getHeaderSize() + serializer.getSize(context, payload);
            } catch (Serializer.SerializerException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public Response copy() {
            return new Response<E>(vodSrc, vodDest, payload);
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            Serializer serializer = context.getSerializer(payload.getClass());
            try {
                serializer.encode(context, buffer, payload);
            } catch (Serializer.SerializerException ex) {
                throw new MessageEncodingException("serialization exception");
            }
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return context.getOpcode(BaseCategories.NETWORK, this.getClass());
        }
    }
    
    public static class OneWay<E extends Payload> extends DirectMsgNetty.Oneway {
        public final E payload;
        
        public OneWay(VodAddress src, VodAddress dest, E payload) {
            super(src, dest);
            
            this.payload = payload;
        }
        
        @Override 
        public String toString() {
            return payload.toString() + " src " + vodSrc.getPeerAddress().toString() + " dest " + vodDest.getPeerAddress().toString();
        }
        
        @Override
        public int getSize() {
            try {
                Serializer serializer = context.getSerializer(payload.getClass());
                return getHeaderSize() + serializer.getSize(context, payload);
            } catch (Serializer.SerializerException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public OneWay copy() {
            return new OneWay<E>(vodSrc, vodDest, payload);
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            Serializer serializer = context.getSerializer(payload.getClass());
            try {
                serializer.encode(context, buffer, payload);
            } catch (Serializer.SerializerException ex) {
                throw new MessageEncodingException("serialization exception");
            }
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return context.getOpcode(BaseCategories.NETWORK, this.getClass());
        }
    }
}

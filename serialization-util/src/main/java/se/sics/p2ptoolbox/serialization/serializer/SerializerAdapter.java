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
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.common.msgs.DirectMsgNettyFactory;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.net.msgs.DirectMsg;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.p2ptoolbox.serialization.SerializationContext;
import se.sics.p2ptoolbox.serialization.Serializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SerializerAdapter {
    
    public static void setContext(SerializationContext setContext) {
        Request.context = setContext;
        Response.context = setContext;
        OneWay.context = setContext;
    }

    public static boolean hasContext() {
        return Request.context != null && Response.context != null && OneWay.context != null;
    }
    
    public static class Request extends DirectMsgNettyFactory.Request {
        protected static SerializationContext context;
        
        @Override
        protected DirectMsg process(ByteBuf buf) throws MessageDecodingException {
            try {
                Byte aliasCode = buf.readByte();
                Byte multiplexCode = buf.readByte();
                Serializer serializer = context.getSerializer(DirectMsgNetty.Request.class, aliasCode, multiplexCode);
                Object o = serializer.decode(context, buf);
                if(!(o instanceof DirectMsg)) {
                    throw new MessageDecodingException();
                }
                DirectMsg msg = (DirectMsg)o;
                msg.rewriteDestination(dest);
                msg.rewritePublicSource(src);
                return msg;
            } catch (Serializer.SerializerException ex) {
                throw new RuntimeException(ex);
            } catch (SerializationContext.MissingException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        public RewriteableMsg decodeMsg(ByteBuf buffer) throws MessageDecodingException {
                return decode(buffer);
        }
    }
    
    public static class Response extends DirectMsgNettyFactory.Response {
        
        protected static SerializationContext context;
        
        @Override
        protected DirectMsg process(ByteBuf buf) throws MessageDecodingException {
            try {
                Byte aliasCode = buf.readByte();
                Byte multiplexCode = buf.readByte();
                
                Serializer serializer = context.getSerializer(DirectMsgNetty.Response.class, aliasCode, multiplexCode);
                Object o = serializer.decode(context, buf);
                if(!(o instanceof DirectMsg)) {
                    throw new MessageDecodingException();
                }
                DirectMsg msg = (DirectMsg)o;
                msg.rewriteDestination(dest);
                msg.rewritePublicSource(src);
                return msg;
            } catch (Serializer.SerializerException ex) {
                throw new RuntimeException(ex);
            } catch (SerializationContext.MissingException ex) {
                throw new RuntimeException(ex);
            }
        }

        public RewriteableMsg decodeMsg(ByteBuf buffer) throws MessageDecodingException {
                return decode(buffer);
        }
    }
    
    public static class OneWay extends DirectMsgNettyFactory.Oneway {
        protected static SerializationContext context;
        
        @Override
        protected DirectMsg process(ByteBuf buf) throws MessageDecodingException {
            try {
                Byte aliasCode = buf.readByte();
                Byte multiplexCode = buf.readByte();
                
                Serializer serializer = context.getSerializer(DirectMsgNetty.Oneway.class, aliasCode, multiplexCode);
                Object o = serializer.decode(context, buf);
                if(!(o instanceof DirectMsg)) {
                    throw new MessageDecodingException();
                }
                DirectMsg msg = (DirectMsg)o;
                msg.rewriteDestination(dest);
                msg.rewritePublicSource(src);
                return msg;
            } catch (Serializer.SerializerException ex) {
                throw new RuntimeException(ex);
            } catch (SerializationContext.MissingException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        public RewriteableMsg decodeMsg(ByteBuf buffer) throws MessageDecodingException {
                return decode(buffer);
        }
    }
}

/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Croupier is free software; you can redistribute it and/or
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

package se.sics.p2ptoolbox.croupier.core.net;

import io.netty.buffer.ByteBuf;
import se.sics.p2ptoolbox.croupier.core.msg.ShuffleNet;
import se.sics.p2ptoolbox.serialization.SerializationContext;
import se.sics.p2ptoolbox.serialization.serializer.NetContentMsgSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ShuffleNetSerializer {
    public static class Request extends NetContentMsgSerializer.Request<ShuffleNet.Request> {

        public ShuffleNet.Request decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
    
    public static class Response extends NetContentMsgSerializer.Response<ShuffleNet.Response> {

        public ShuffleNet.Response decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
}

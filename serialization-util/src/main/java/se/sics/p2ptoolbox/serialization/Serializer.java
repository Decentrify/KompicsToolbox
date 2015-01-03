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

package se.sics.p2ptoolbox.serialization;

import io.netty.buffer.ByteBuf;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public interface Serializer<E extends Object> {

    public ByteBuf encode(SerializationContext context, ByteBuf buf, E obj) throws SerializerException, SerializationContext.MissingException;

    public E decode(SerializationContext context, ByteBuf buf) throws SerializerException, SerializationContext.MissingException;
    
    public int getSize(SerializationContext context, E obj) throws SerializerException, SerializationContext.MissingException;

    public static class SerializerException extends Exception {
        public SerializerException(String msg) {
            super(msg);
        }
        
        public SerializerException(Throwable cause) {
            super(cause);
        }
        
        public SerializerException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}
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
package se.sics.p2ptoolbox.serialization.api;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public interface SerializationContext {
    public <E extends Object> SerializationContext registerSerializer(Class<E> serializedClass, Serializer<E> classSerializer) throws DuplicateException;
    public SerializationContext registerCategory(CatMarker marker, byte category) throws DuplicateException;
    public SerializationContext registerOpcode(CatMarker marker, byte opcode, Class<?> serializedClass) throws DuplicateException;
    
    public <E extends Object> Serializer<E> getSerializer(Class<E> serializedClass);
    public <E extends Object> Class<? extends E> getSerializedClass(CatMarker marker, byte opCode);
    public Byte getOpcode(CatMarker marker, Class<?> serializedClass);
    
    public static class DuplicateException extends Exception {
        public DuplicateException() {
            super();
        }
    }
    
    public static interface CatMarker {
    }
}

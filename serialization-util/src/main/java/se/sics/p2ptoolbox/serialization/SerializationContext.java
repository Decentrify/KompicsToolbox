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

import java.util.Set;
import org.javatuples.Pair;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public interface SerializationContext {
    public <E extends Object> SerializationContext registerSerializer(Class<E> serializedClass, Serializer<E> classSerializer) throws DuplicateException;
    public SerializationContext registerAlias(Class aliasedClass, String alias, Byte aliasCode) throws DuplicateException;
    public SerializationContext multiplexAlias(String alias, Class multiplexClass, Byte multiplexCode) throws DuplicateException, MissingException;
    
    public boolean containsAliases(Set<String> aliases);
    public Byte getAliasCode(String alias) throws MissingException;
    public <E extends Object> Serializer<E> getSerializer(Class<E> serializedClass) throws MissingException;
    public Serializer getSerializer(Class aliasedClass, byte aliasCode, byte multiplexCode) throws MissingException;
    public Pair<Byte, Byte> getCode(Class serializedClass) throws MissingException;
            
    public static class DuplicateException extends Exception {
        public DuplicateException() {
            super();
        }
    }
    
    public static class MissingException extends Exception {
        public MissingException() {
            super();
        }
        
        public MissingException(String msg) {
            super(msg);
        }
    }
}
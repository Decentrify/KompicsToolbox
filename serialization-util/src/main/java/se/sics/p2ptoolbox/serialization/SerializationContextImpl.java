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

import se.sics.p2ptoolbox.serialization.api.SerializationContext;
import se.sics.p2ptoolbox.serialization.api.Serializer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.javatuples.Pair;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SerializationContextImpl<E extends Object> implements SerializationContext<E> {

    private static final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private final Map<Pair<Byte, Byte>, Serializer<E>> serializers;
    private final Map<Class<E>, Pair<Byte, Byte>> classMapping;

    private SerializationContextImpl() {
        this.serializers = new HashMap<Pair<Byte, Byte>, Serializer<E>>();
        this.classMapping = new HashMap<Class<E>, Pair<Byte, Byte>>();
    }

    @Override
    public void register(byte sCategory, byte sCode, Class<E> serializedClass, Serializer<E> classSerializer) throws DuplicateException {
        rwLock.writeLock().lock();
        try {
            Pair<Byte, Byte> serializerId = Pair.with(sCategory, sCode);
            if (serializers.containsKey(serializerId)) {
                throw new DuplicateException();
            }
            serializers.put(serializerId, classSerializer);
            classMapping.put(serializedClass, serializerId);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public Serializer<E> getSerializer(byte sCategory, byte sCode) {
        rwLock.readLock().lock();
        try {
            return serializers.get(Pair.with(sCategory, sCode));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Serializer<E> getSerializer(Class<E> serializedClass) {
        rwLock.readLock().lock();
        try {
            Pair<Byte, Byte> serializerId = classMapping.get(serializedClass);
            return serializers.get(serializerId);
        } finally {
            rwLock.readLock().unlock();
        }
    }
}

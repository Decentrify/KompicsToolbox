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
public class SerializationContextImpl implements SerializationContext {

    private final ReentrantReadWriteLock rwLock;

    private final Map<Class<?>, Serializer<?>> serializers;
    private final Map<Pair<Byte, Byte>, Class<?>> classMapping;
    private final Map<CatMarker, Byte> categoryMapping;
    byte nextCat;

    public SerializationContextImpl() {
        this.rwLock = new ReentrantReadWriteLock();

        this.serializers = new HashMap<Class<?>, Serializer<?>>();
        this.classMapping = new HashMap<Pair<Byte, Byte>, Class<?>>();
        this.categoryMapping = new HashMap<CatMarker, Byte>();
        this.nextCat = 0x00;
    }

    @Override
    public <E extends Object> SerializationContextImpl registerSerializer(Class<E> serializedClass, Serializer<E> classSerializer) throws DuplicateException {
        rwLock.writeLock().lock();
        try {
            if (serializers.containsKey(serializedClass)) {
                throw new DuplicateException();
            }
            serializers.put(serializedClass, classSerializer);
            return this;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public SerializationContext registerCategory(CatMarker marker, byte category) throws DuplicateException {
        rwLock.writeLock().lock();
        try {
            if (categoryMapping.containsKey(marker)) {
                throw new DuplicateException();
            }
            categoryMapping.put(marker, category);
            return this;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public SerializationContext registerOpcode(CatMarker marker, byte opcode, Class<?> serializedClass) throws DuplicateException {
        rwLock.writeLock().lock();
        try {
            byte category = categoryMapping.get(marker);
            Pair<Byte, Byte> id = Pair.with(category, opcode);
            if (classMapping.containsKey(id) || classMapping.containsValue(serializedClass)) {
                throw new DuplicateException();
            }
            classMapping.put(id, serializedClass);
            return this;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public <E extends Object> Serializer<E> getSerializer(Class<E> serializedClass) {
        rwLock.readLock().lock();
        try {
            return (Serializer<E>) serializers.get(serializedClass);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public <E extends Object> Class<? extends E> getSerializedClass(CatMarker marker, byte opcode) {
        rwLock.readLock().lock();
        try {
            Pair<Byte, Byte> id = Pair.with(categoryMapping.get(marker), opcode);
            return (Class<? extends E>) classMapping.get(id);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Byte getOpcode(CatMarker marker, Class<?> serializedClass) {
        rwLock.readLock().lock();
        try {
            byte category = categoryMapping.get(marker);
            for (Map.Entry<Pair<Byte, Byte>, Class<?>> e : classMapping.entrySet()) {
                if (e.getKey().getValue0() == category && serializedClass.equals(e.getValue())) {
                    return e.getKey().getValue1();
                }
            }
            return null;
        } finally {
            rwLock.readLock().unlock();
        }
    }
}

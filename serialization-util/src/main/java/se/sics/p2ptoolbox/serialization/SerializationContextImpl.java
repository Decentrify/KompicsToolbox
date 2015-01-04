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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import se.sics.p2ptoolbox.serialization.SerializationContext.MissingException;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SerializationContextImpl implements SerializationContext {

    private final ReentrantReadWriteLock rwLock;

    private final Map<Class, Serializer> serializers; //serializedClass, classSerializer>
    private final Map<String, Pair<Class, Byte>> aliases; //<aliasName, <aliasClass, aliasCode>>
    private final Map<Class, Triplet<Class, Byte, Byte>> classToBCode; //<multiplexClass, <aliasClass, aliasCode, multiplexCode>>
    private final Map<Triplet<Class, Byte, Byte>, Class> bcodeToClass; //<<aliasClass, aliasCode, multiplexCode>, multiplexClass>

    public SerializationContextImpl() {
        this.rwLock = new ReentrantReadWriteLock();
        this.serializers = new HashMap<Class, Serializer>();
        this.aliases = new HashMap<String, Pair<Class, Byte>>();
        this.classToBCode = new HashMap<Class, Triplet<Class, Byte, Byte>>();
        this.bcodeToClass = new HashMap<Triplet<Class, Byte, Byte>, Class>();
    }

    @Override
    public <E> SerializationContext registerSerializer(Class<E> serializedClass, Serializer<E> classSerializer) throws DuplicateException {
        rwLock.writeLock().lock();
        try {
            if (serializers.containsKey(serializedClass) || serializers.containsValue(classSerializer)) {
                throw new DuplicateException();
            }
            serializers.put(serializedClass, classSerializer);
            return this;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public SerializationContext registerAlias(Class aliasedClass, String alias, Byte aliasCode) throws DuplicateException {
        rwLock.writeLock().lock();
        try {
            if (aliases.containsKey(alias)) {
                throw new DuplicateException();
            }

            aliases.put(alias, Pair.with(aliasedClass, aliasCode));
            return this;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public SerializationContext multiplexAlias(String alias, Class multiplexClass, Byte multiplexCode) throws DuplicateException, MissingException {
        rwLock.writeLock().lock();
        try {
            Pair<Class, Byte> aliasInfo = aliases.get(alias);
            if (aliasInfo == null) {
                throw new MissingException();
            }
            if (classToBCode.containsKey(multiplexClass)) {
                throw new DuplicateException();
            }

            classToBCode.put(multiplexClass, aliasInfo.add(multiplexCode));
            bcodeToClass.put(aliasInfo.add(multiplexCode), multiplexClass);
            return this;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public boolean containsAliases(Set<String> subset) {
        return aliases.keySet().containsAll(subset);
    }

    @Override
    public <E extends Object> Serializer<E> getSerializer(Class<E> serializedClass) throws MissingException {
        rwLock.readLock().lock();
        try {
            Serializer serializer = serializers.get(serializedClass);
            if (serializer == null) {
                throw new MissingException(serializedClass.toString());
            }
            return (Serializer<E>) serializer;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Serializer getSerializer(Class aliasedClass, byte aliasCode, byte multiplexCode) throws MissingException {
        rwLock.readLock().lock();
        try {
            Class multiplexClass = bcodeToClass.get(Triplet.with(aliasedClass, aliasCode, multiplexCode));
            if (multiplexClass == null) {
                throw new MissingException();
            }
            Serializer serializer = serializers.get(multiplexClass);
            if (serializer == null) {
                throw new MissingException();
            }
            return serializer;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Pair<Byte, Byte> getCode(Class serializedClass) throws MissingException {
        rwLock.readLock().lock();
        try {
            Triplet<Class, Byte, Byte> multiplexInfo = classToBCode.get(serializedClass);
            if (multiplexInfo == null) {
                throw new MissingException();
            }
            return multiplexInfo.removeFrom0();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Byte getAliasCode(String alias) throws MissingException {
        rwLock.readLock().lock();
        try {
            Pair<Class,Byte> aliasInfo = aliases.get(alias);
            if (aliasInfo == null) {
                throw new MissingException();
            }
            return aliasInfo.getValue1();
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * KompicsToolbox is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.util.identifiable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import se.sics.kompics.util.Identifier;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class IdentifierRegistry {
    private static final ConcurrentHashMap<String, IdentifierFactory> identifierFactories = new ConcurrentHashMap<>();
    private static final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    
    public static void register(String factoryName, IdentifierFactory factory) {
        rwLock.writeLock().lock();
        try {
            if(identifierFactories.contains(factoryName)) {
                throw new RuntimeException("misconfigures identifier factory - double factory registration for:" + factoryName);
            }
            identifierFactories.put(factoryName, factory);
            factory.setRegisteredName(factoryName);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    public static IdentifierFactory<Identifier> lookup(String factoryName) {
        rwLock.readLock().lock();
        try {
            return identifierFactories.get(factoryName);
        } finally {
            rwLock.readLock().unlock();
        }
    }
}

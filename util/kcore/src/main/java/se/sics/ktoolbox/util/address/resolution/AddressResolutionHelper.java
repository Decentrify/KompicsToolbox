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
package se.sics.ktoolbox.util.address.resolution;

import se.sics.ktoolbox.util.address.basic.BasicAddressResolution;
import se.sics.ktoolbox.util.address.nat.NAAddressResolution;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import se.sics.kompics.network.Address;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class AddressResolutionHelper {

    private static final ReentrantReadWriteLock setRWLock = new ReentrantReadWriteLock();
    private static final ReentrantReadWriteLock resolutionRWLock = new ReentrantReadWriteLock();
    private static AddressResolution resolution;

    public static void reset() {
        setRWLock.writeLock().lock();
        try {
            resolution = null;
        } finally {
            setRWLock.writeLock().unlock();
        }
    }
    
    public static void useBasicAddresses() {
        setRWLock.writeLock().lock();
        try {
            if (resolution != null) {
                throw new RuntimeException("double set of system address type: basic/natAware");
            }
            resolution = new BasicAddressResolution();
        } finally {
            setRWLock.writeLock().unlock();
        }
    }

    public static void useNatAwareAddresses() {
        setRWLock.writeLock().lock();
        try {
            if (resolution != null) {
                throw new RuntimeException("double set of system address type: basic/natAware");
            }
            resolution = new NAAddressResolution();
        } finally {
            setRWLock.writeLock().unlock();
        }
    }

    public static Address localAddress(Address address) {
        setRWLock.readLock().lock();
        try {
            if (resolution == null) {
                throw new RuntimeException("did not set system address type: basic/natAware");
            }
        } finally {
            setRWLock.readLock().unlock();
        }
        
        resolutionRWLock.readLock().lock();
        try {
            return resolution.localAddress(address);
        } finally {
            resolutionRWLock.readLock().unlock();
        }
    }

    public static Address setDirect(Address address) {
        setRWLock.readLock().lock();
        try {
            if (resolution == null) {
                throw new RuntimeException("did not set system address type: basic/natAware");
            }
        } finally {
            setRWLock.readLock().unlock();
        }
        
        resolutionRWLock.writeLock().lock();
        try {
            return resolution.setDirect(address);
        } finally {
            resolutionRWLock.writeLock().unlock();
        }
    }

    public static Address setIndirect(Address address) {
        setRWLock.readLock().lock();
        try {
            if (resolution == null) {
                throw new RuntimeException("did not set system address type: basic/natAware");
            }
        } finally {
            setRWLock.readLock().unlock();
        }
        
        resolutionRWLock.writeLock().lock();
        try {
            return resolution.setIndirect(address);
        } finally {
            resolutionRWLock.writeLock().unlock();
        }
    }

    public static Address resolve(Address address) {
        setRWLock.readLock().lock();
        try {
            if (resolution == null) {
                throw new RuntimeException("did not set system address type: basic/natAware");
            }
        } finally {
            setRWLock.readLock().unlock();
        }
        
        resolutionRWLock.readLock().lock();
        try {
            return resolution.resolve(address);
        } finally {
            resolutionRWLock.readLock().unlock();
        }
    }

    public static void cleanup() {
        setRWLock.readLock().lock();
        try {
            if (resolution == null) {
                throw new RuntimeException("did not set system address type: basic/natAware");
            }
        } finally {
            setRWLock.readLock().unlock();
        }
        
        resolutionRWLock.writeLock().lock();
        try {
            resolution.cleanup();
        } finally {
            resolutionRWLock.writeLock().unlock();
        }
    }
    
}

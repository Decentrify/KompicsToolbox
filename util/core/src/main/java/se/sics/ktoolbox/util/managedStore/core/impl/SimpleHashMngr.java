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

package se.sics.ktoolbox.util.managedStore.core.impl;

import java.util.Set;
import se.sics.ktoolbox.util.managedStore.core.Storage;
import se.sics.ktoolbox.util.managedStore.core.HashMngr;
import se.sics.ktoolbox.util.managedStore.core.ComponentTracker;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SimpleHashMngr implements HashMngr {
    
    private final ComponentTracker pieceTracker;
    private final Storage storage;
    public final String hashType;
    private final int hashSize;
    
    public SimpleHashMngr(ComponentTracker pieceTracker, Storage storage, String hashType, int hashSize) {
        this.pieceTracker = pieceTracker;
        this.storage = storage;
        this.hashType = hashType;
        this.hashSize = hashSize;
    }
    
    @Override
    public boolean hasHash(int hashNr) {
        return pieceTracker.hasComponent(hashNr);
    }

    @Override
    public byte[] readHash(int hashNr) {
        return storage.read(hashNr * hashSize, hashSize);
    }

    @Override
    public int writeHash(int hashNr, byte[] hash) {
        if(hash.length > hashSize) {
            throw new RuntimeException("exceeding size");
        }
        pieceTracker.addComponent(hashNr);
        return storage.write(hashNr*hashSize, hash);
    }

    @Override
    public boolean isComplete(int hashNr) {
        return pieceTracker.isComplete(hashNr);
    }

    @Override
    public int contiguous(int hashNr) {
        return pieceTracker.nextComponentMissing(hashNr);
    }
    
    @Override
    public Set<Integer> nextHashes(int hashNr, int n, Set<Integer> exclude) {
        return pieceTracker.nextComponentMissing(hashNr, n, exclude);
    }
}

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
package se.sics.ktoolbox.util.managedStore.core.impl;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.javatuples.Pair;
import se.sics.ktoolbox.util.managedStore.core.ComponentTracker;
import se.sics.ktoolbox.util.managedStore.core.FileMngr;
import se.sics.ktoolbox.util.managedStore.core.HashMngr;
import se.sics.ktoolbox.util.managedStore.core.ManagedStoreHelper;
import se.sics.ktoolbox.util.managedStore.core.Storage;
import se.sics.ktoolbox.util.managedStore.core.impl.tracker.IncompleteTracker;
import se.sics.ktoolbox.util.managedStore.core.util.HashUtil;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OnDemandWithRetentionHashMngr implements HashMngr {

    private final FileMngr fileMngr;
    public final String hashAlg;

    private final int blockSize;
    //<lastBlockNr, lastBlockSize>
    private final Pair<Integer, Integer> lastBlock;

    private final TreeMap<Integer, ByteBuffer> hashes = new TreeMap<>();
    private final ComponentTracker hashCompTracker;

    public OnDemandWithRetentionHashMngr(FileMngr fileMngr, String hashAlg, int blockSize) {
        this.fileMngr = fileMngr;
        this.hashAlg = hashAlg;
        this.blockSize = blockSize;
        this.lastBlock = ManagedStoreHelper.lastComponent(fileMngr.length(), blockSize);
        this.hashCompTracker = IncompleteTracker.create(lastBlock.getValue0() + 1);
    }

    @Override
    public boolean hasHash(int hashNr) {
        int readLength;
        if (hashNr == lastBlock.getValue0()) {
            readLength = lastBlock.getValue0();
        } else {
            readLength = blockSize;
        }
        return hashCompTracker.hasComponent(hashNr) || fileMngr.has(hashNr, readLength);
    }

    @Override
    public ByteBuffer readHash(int hashNr) {
        if (hashes.containsKey(hashNr)) {
            return hashes.get(hashNr);
        }
        int readLength;
        if (hashNr == lastBlock.getValue0()) {
            readLength = lastBlock.getValue1();
        } else {
            readLength = blockSize;
        }
        ByteBuffer fileBlock = fileMngr.read(hashNr * blockSize, readLength);
        ByteBuffer hash = ByteBuffer.wrap(HashUtil.makeHash(fileBlock.array(), hashAlg));
        hashes.put(hashNr, hash);
        hashCompTracker.addComponent(hashNr);
        return hash;
    }

    @Override
    public Pair<Map<Integer, ByteBuffer>, Set<Integer>> readHashes(Set<Integer> hashNr) {
        Map<Integer, ByteBuffer> resultHashes = new HashMap<>();
        Set<Integer> missingHashes = new HashSet<>();
        for (Integer hash : hashNr) {
            if (hasHash(hash)) {
                resultHashes.put(hash, readHash(hash));
            } else {
                missingHashes.add(hash);
            }
        }
        return Pair.with(resultHashes, missingHashes);
    }

    @Override
    public int writeHash(int hashNr, byte[] hash) {
        hashes.put(hashNr, ByteBuffer.wrap(hash));
        hashCompTracker.addComponent(hashNr);
        return hash.length;
    }

    @Override
    public boolean isComplete(int hashNr) {
        return hashCompTracker.isComplete(hashNr);
    }

    @Override
    public int contiguous(int hashNr) {
        return hashCompTracker.nextComponentMissing(hashNr);
    }

    @Override
    public Set<Integer> nextHashes(int hashNr, int n, Set<Integer> exclude) {
        return hashCompTracker.nextComponentMissing(hashNr, n, exclude);
    }

    @Override
    public int nextHash(int hashNr, Set<Integer> exclude) {
        return hashCompTracker.nextComponentMissing(hashNr, exclude);
    }
}

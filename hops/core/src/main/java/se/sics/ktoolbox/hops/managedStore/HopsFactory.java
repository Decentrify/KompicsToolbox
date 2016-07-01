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
package se.sics.ktoolbox.hops.managedStore;

import org.javatuples.Pair;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.hdfs.HDFSResource;
import se.sics.ktoolbox.kafka.KafkaResource;
import se.sics.ktoolbox.util.managedStore.core.ComponentTracker;
import se.sics.ktoolbox.util.managedStore.core.FileMngr;
import se.sics.ktoolbox.util.managedStore.core.HashMngr;
import se.sics.ktoolbox.util.managedStore.core.ManagedStoreHelper;
import se.sics.ktoolbox.util.managedStore.core.Storage;
import se.sics.ktoolbox.util.managedStore.core.impl.CompleteFileMngr;
import se.sics.ktoolbox.util.managedStore.core.impl.IncompleteFileMngr;
import se.sics.ktoolbox.util.managedStore.core.impl.OnDemandWithRetentionHashMngr;
import se.sics.ktoolbox.util.managedStore.core.impl.tracker.IncompleteTracker;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HopsFactory {

    public static Pair<FileMngr, HashMngr> getComplete(HDFSResource resource, String hashAlg, int blockSize, int pieceSize) {
        Storage fileStorage = new CompleteHopsDataStorage(resource);
        FileMngr fileMngr = new CompleteFileMngr(fileStorage, blockSize, pieceSize);
        HashMngr hashMngr = new OnDemandWithRetentionHashMngr(fileMngr, hashAlg, blockSize);
        return Pair.with(fileMngr, hashMngr);
    }

    public static Pair<FileMngr, HashMngr> getIncomplete(HDFSResource resource, long fileSize, String hashAlg, int blockSize, int pieceSize) {
        Storage fileStorage = new PendingHopsDataStorage(resource, fileSize);
        int nrBlocks = ManagedStoreHelper.nrComponents(fileSize, blockSize);
        ComponentTracker ct = IncompleteTracker.create(nrBlocks);
        FileMngr fileMngr = new IncompleteFileMngr(fileStorage, ct, blockSize, pieceSize);
        HashMngr hashMngr = new OnDemandWithRetentionHashMngr(fileMngr, hashAlg, blockSize);
        return Pair.with(fileMngr, hashMngr);
    }

    public static Pair<FileMngr, HashMngr> getCompleteCached(Config config, HDFSResource hdfsResource, KafkaResource kafkaResource, String hashAlg, int blockSize, int pieceSize) {
        Storage fileStorage = new CachedHDFSStorage(config, hdfsResource, kafkaResource, -1);
        FileMngr fileMngr = new CompleteFileMngr(fileStorage, blockSize, pieceSize);
        HashMngr hashMngr = new OnDemandWithRetentionHashMngr(fileMngr, hashAlg, blockSize);
        return Pair.with(fileMngr, hashMngr);
    }
    
    public static Pair<FileMngr, HashMngr> getIncompleteCached(Config config, HDFSResource hdfsResource, KafkaResource kafkaResource, long fileSize, String hashAlg, int blockSize, int pieceSize) {
        Storage fileStorage = new CachedHDFSStorage(config, hdfsResource, kafkaResource, fileSize);
        int nrBlocks = ManagedStoreHelper.nrComponents(fileSize, blockSize);
        ComponentTracker ct = IncompleteTracker.create(nrBlocks);
        FileMngr fileMngr = new IncompleteFileMngr(fileStorage, ct, blockSize, pieceSize);
        HashMngr hashMngr = new OnDemandWithRetentionHashMngr(fileMngr, hashAlg, blockSize);
        return Pair.with(fileMngr, hashMngr);
    }
}

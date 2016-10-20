///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * GVoD is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package se.sics.ktoolbox.util.managedStore.core.impl;
//
//import se.sics.ktoolbox.util.network.ports.BlockMngr;
//import se.sics.ktoolbox.util.managedStore.core.FileMngr;
//import se.sics.ktoolbox.util.managedStore.core.HashMngr;
//import se.sics.ktoolbox.util.stream.tracker.ComponentTracker;
//import se.sics.ktoolbox.util.stream.tracker.CompleteTracker;
//import se.sics.ktoolbox.util.stream.tracker.IncompleteTracker;
//import java.io.IOException;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import org.javatuples.Pair;
//import se.sics.ktoolbox.util.managedStore.core.ManagedStoreHelper;
//import se.sics.ktoolbox.util.managedStore.core.Storage;
//import se.sics.ktoolbox.util.network.ports.InMemoryBlockMngr;
//import se.sics.ktoolbox.util.managedStore.core.impl.storage.StorageFactory;
//
///**
// * @author Alex Ormenisan <aaor@sics.se>
// */
//public class StorageMngrFactory {
//
//    public static ComponentTracker getCompletePT(int nrPieces) {
//        return new CompleteTracker(nrPieces);
//    }
//
//    public static ComponentTracker getInOrderPT(int nrPieces) {
//        return IncompleteTracker.create(nrPieces);
//    }
//
//    public static FileMngr completeMMFileMngr(String pathName, long fileLength, int blockSize, int pieceSize) throws IOException {
//        return new CompleteFileMngr(StorageFactory.existingDiskMMFile(pathName), blockSize, pieceSize);
//    }
//
//    public static FileMngr incompleteMMFileMngr(String pathName, long fileLength, int blockSize, int pieceSize) throws IOException {
//        int nrBlocks = ManagedStoreHelper.nrComponents(fileLength, blockSize);
//        return new IncompleteFileMngr(StorageFactory.emptyDiskMMFile(pathName, fileLength), getInOrderPT(nrBlocks), blockSize, pieceSize);
//    }
//
//    public static BlockMngr inMemoryBlockMngr(int blockSize, int pieceSize) {
//        int nrPieces = ManagedStoreHelper.nrComponents(blockSize, pieceSize);
//        return new InMemoryBlockMngr(StorageFactory.inMemoryEmptyBlock(blockSize), getInOrderPT(nrPieces), blockSize, pieceSize);
//    }
//
//    public static HashMngr completeMMHashMngr(String pathName, String hashType, long hashFileSize, int hashSize) throws IOException {
//        int nrHashes = ManagedStoreHelper.nrComponents(hashFileSize, hashSize);
//        return new SimpleHashMngr(getCompletePT(nrHashes), StorageFactory.existingDiskMMFile(pathName), hashType, hashSize);
//    }
//
//    public static HashMngr incompleteMMHashMngr(String pathName, String hashType, long hashFileSize, int hashSize) throws IOException {
//        int nrHashes = ManagedStoreHelper.nrComponents(hashFileSize, hashSize);
//        return new SimpleHashMngr(getInOrderPT(nrHashes), StorageFactory.emptyDiskMMFile(pathName, hashFileSize), hashType, hashSize);
//    }
//
//    public static Pair<FileMngr, HashMngr> getCompleteComb1(String pathName, String hashAlg, int blockSize, int pieceSize) {
//        try {
//            FileMngr fileMngr = new CompleteFileMngr(StorageFactory.existingDiskMMFile(pathName), blockSize, pieceSize);
//            HashMngr hashMngr = new OnDemandWithRetentionHashMngr(fileMngr, hashAlg, blockSize);
//            return Pair.with(fileMngr, hashMngr);
//        } catch (IOException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
//
//    public static Pair<FileMngr, HashMngr> getIncompleteComb1(String pathName, long fileLength, String hashAlg, int blockSize, int pieceSize) {
//        try {
//            FileMngr fileMngr = incompleteMMFileMngr(pathName, fileLength, blockSize, pieceSize);
//            HashMngr hashMngr = new OnDemandWithRetentionHashMngr(fileMngr, hashAlg, blockSize);
//            return Pair.with(fileMngr, hashMngr);
//        } catch (IOException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
//}

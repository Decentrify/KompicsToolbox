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
//import java.nio.ByteBuffer;
//import se.sics.ktoolbox.util.managedStore.core.Storage;
//import se.sics.ktoolbox.util.managedStore.core.FileMngr;
//import java.util.Set;
//import org.javatuples.Pair;
//import se.sics.ktoolbox.util.identifiable.Identifier;
//import se.sics.ktoolbox.util.stream.tracker.ComponentTracker;
//import se.sics.ktoolbox.util.managedStore.core.ManagedStoreHelper;
//
///**
// * @author Alex Ormenisan <aaor@sics.se>
// */
//public class IncompleteFileMngr implements FileMngr {
//
//    private final ComponentTracker pieceTracker;
//    private final Storage storage;
//    private final int blockSize;
//    private final int pieceSize;
//    //<lastBlockNr, lastBlockSize>
//    private final Pair<Integer, Integer> lastBlock;
//    //<lastPieceNr, lastPieceSize>
//    private final Pair<Integer, Integer> lastPiece;
//
//    public IncompleteFileMngr(Storage storage, ComponentTracker pieceTracker, int blockSize, int pieceSize) {
//        this.pieceTracker = pieceTracker;
//        this.storage = storage;
//        this.blockSize = blockSize;
//        this.pieceSize = pieceSize;
//        this.lastBlock = ManagedStoreHelper.lastComponent(storage.length(), blockSize);
//        this.lastPiece = ManagedStoreHelper.lastComponent(storage.length(), pieceSize);
//    }
//    
//    @Override
//    public void tearDown() {
//        storage.tearDown();
//    }
//
//    @Override
//    public boolean has(long readPos, int length) {
//        if (readPos > ManagedStoreHelper.MAX_BYTE_FILE_SIZE) {
//            throw new RuntimeException("read position exceeds max file size:" + ManagedStoreHelper.MAX_BYTE_FILE_SIZE);
//        }
//        int blockNr = ManagedStoreHelper.componentNr(readPos, blockSize);
//        while (length > 0) {
//            if (!pieceTracker.hasComponent(blockNr)) {
//                return false;
//            }
//            length = length - blockSize;
//            blockNr++;
//        }
//        return true;
//    }
//
//    @Override
//    public ByteBuffer read(Identifier readerId, long readPos, int length, Set<Integer> bufferBlocks) {
//        if (readPos + length > storage.length()) {
//            throw new RuntimeException("logic error");
//        }
//        return ByteBuffer.wrap(storage.read(readerId, readPos, length, bufferBlocks));
//    }
//
//    @Override
//    public boolean hasPiece(int pieceNr) {
//        long readPos = pieceNr * pieceSize;
//        int readLength;
//        if (lastPiece.getValue0() == pieceNr) {
//            readLength = lastPiece.getValue1();
//        } else {
//            readLength = pieceSize;
//        }
//        return has(readPos, readLength);
//    }
//
//    @Override
//    public ByteBuffer readPiece(Identifier readerId, int pieceNr, Set<Integer> bufferBlocks) {
//        long readPos = pieceNr * pieceSize;
//        int readLength;
//        if (lastPiece.getValue0() == pieceNr) {
//            readLength = lastPiece.getValue1();
//        } else {
//            readLength = pieceSize;
//        }
//        return read(readerId, readPos, readLength, bufferBlocks);
//    }
//
//    @Override
//    public boolean isComplete(int fromBlockNr) {
//        return pieceTracker.isComplete(fromBlockNr);
//    }
//
//    @Override
//    public int contiguous(int fromBlockNr) {
//        return pieceTracker.nextComponentMissing(fromBlockNr);
//    }
//
//    @Override
//    public int writeBlock(int blockNr, ByteBuffer block) {
//        pieceTracker.addComponent(blockNr);
//        return storage.write(blockNr * blockSize, block.array());
//    }
//
//    @Override
//    public Integer nextBlock(int blockNr, Set<Integer> exclude) {
//        return pieceTracker.nextComponentMissing(blockNr, exclude);
//    }
//
//    @Override
//    public int blockSize(int blockNr) {
//        if (lastBlock.getValue0() == blockNr) {
//            return lastBlock.getValue1();
//        } else {
//            return blockSize;
//        }
//    }
//
//    @Override
//    public double percentageCompleted() {
//        return (double) pieceTracker.completedComponents() / pieceTracker.nrComponents();
//    }
//
//    @Override
//    public long length() {
//        return storage.length();
//    }
//
//}

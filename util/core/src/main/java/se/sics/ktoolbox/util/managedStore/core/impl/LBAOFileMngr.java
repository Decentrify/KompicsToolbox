///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * KompicsToolbox is free software; you can redistribute it and/or
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
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.Set;
//import java.util.TreeMap;
//import org.javatuples.Pair;
//import se.sics.ktoolbox.util.identifiable.Identifier;
//import se.sics.ktoolbox.util.stream.tracker.ComponentTracker;
//import se.sics.ktoolbox.util.managedStore.core.FileMngr;
//import se.sics.ktoolbox.util.managedStore.core.ManagedStoreHelper;
//import se.sics.ktoolbox.util.managedStore.core.Storage;
//
///**
// * LBAO - LimitedBufferAppendOnly no jumps allowed
// *
// * @author Alex Ormenisan <aaor@kth.se>
// */
//public class LBAOFileMngr implements FileMngr {
//
//    private final ComponentTracker blockTracker;
//    private final Storage storage;
//    private final int blockSize;
//    private final int pieceSize;
//    private final int piecesPerBlock;
//    //<lastBlockNr, lastBlockSize>
//    private final Pair<Integer, Integer> lastBlock;
//    //<lastPieceNr, lastPieceSize>
//    private final Pair<Integer, Integer> lastPiece;
//
//    private final int maxBufferSize;
//    private final TreeMap<Integer, ByteBuffer> bufferedBlocks = new TreeMap<>();
//    private int writeBlockNr;
//
//    public LBAOFileMngr(Storage storage, ComponentTracker pieceTracker, int pieceSize,
//            int piecesPerBlock, int maxBufferSize) {
//        this.blockTracker = pieceTracker;
//        this.storage = storage;
//        this.pieceSize = pieceSize;
//        this.piecesPerBlock = piecesPerBlock;
//        this.blockSize = pieceSize * piecesPerBlock;
//        long length = storage.length();
//        this.lastBlock = ManagedStoreHelper.lastComponent(length, blockSize);
//        this.lastPiece = ManagedStoreHelper.lastComponent(length, pieceSize);
//        this.maxBufferSize = maxBufferSize;
//        writeBlockNr = 0;
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
//            if (!blockTracker.hasComponent(blockNr)) {
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
//        int readLength = pieceSize(pieceNr);
//        return has(readPos, readLength);
//    }
//
//    @Override
//    public ByteBuffer readPiece(Identifier readerId, int pieceNr, Set<Integer> bufferBlocks) {
//        long readPos = pieceNr * pieceSize;
//        int readLength = pieceSize(pieceNr);
//        return read(readerId, readPos, readLength, bufferBlocks);
//    }
//
//    private int pieceSize(int pieceNr) {
//        int length = pieceSize;
//        if (lastPiece.getValue0() == pieceNr) {
//            length = lastPiece.getValue1();
//        }
//        return length;
//    }
//
//    @Override
//    public boolean isComplete(int fromBlockNr) {
//        return blockTracker.isComplete(fromBlockNr);
//    }
//
//    @Override
//    public int contiguous(int fromBlockNr) {
//        return blockTracker.nextComponentMissing(fromBlockNr);
//    }
//
//    //TODO Alex - writeLength might get long if i don't limit buffer
//    @Override
//    public int writeBlock(int blockNr, ByteBuffer block) {
//        int writeLength = 0;
//        if (writeBlockNr == blockNr) {
//            blockTracker.addComponent(blockNr);
//            writeLength += storage.write(blockNr * blockSize, block.array());
//            writeBlockNr++;
//
//            if (!bufferedBlocks.isEmpty() && bufferedBlocks.firstKey().equals(writeBlockNr)) {
//                Iterator<Map.Entry<Integer, ByteBuffer>> it = bufferedBlocks.entrySet().iterator();
//                while (it.hasNext()) {
//                    Map.Entry<Integer, ByteBuffer> e = it.next();
//                    if (e.getKey().equals(writeBlockNr)) {
//                        writeLength += storage.write(writeBlockNr * blockSize, block.array());
//                        blockTracker.addComponent(writeBlockNr);
//                        writeBlockNr++;
//                        it.remove();
//                    } else {
//                        break;
//                    }
//                }
//            }
//        } else {
//            bufferedBlocks.put(blockNr, block);
//        }
//        return writeLength;
//    }
//
//    @Override
//    public Integer nextBlock(int blockNr, Set<Integer> exclude) {
//        assert blockNr == 0; //no jumps allowed
//        if (bufferedBlocks.size() < maxBufferSize) {
//            Set<Integer> myExclude = new HashSet<>(exclude);
//            myExclude.addAll(bufferedBlocks.keySet());
//            return blockTracker.nextComponentMissing(blockNr, exclude);
//        }
//        return -1;
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
//        return (double) blockTracker.completedComponents() / blockTracker.nrComponents();
//    }
//
//    @Override
//    public long length() {
//        return storage.length();
//    }
//}

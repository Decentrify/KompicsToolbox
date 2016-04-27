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

import com.google.common.base.Optional;
import com.google.common.io.BaseEncoding;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.javatuples.Pair;
import se.sics.ktoolbox.util.managedStore.core.BlockMngr;
import se.sics.ktoolbox.util.managedStore.core.FileMngr;
import se.sics.ktoolbox.util.managedStore.core.HashMngr;
import se.sics.ktoolbox.util.managedStore.core.ManagedStoreHelper;
import se.sics.ktoolbox.util.managedStore.core.util.HashUtil;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class DownloadMngr {

    private final HashMngr hashMngr;
    private final FileMngr fileMngr;
    private final String hashAlg;
    private final int pieceSize;
    private final int piecesPerBlock;
    private final int hashesPerMsg;

    private final Map<Integer, BlockMngr> queuedBlocks = new HashMap<>();
    private final Set<Integer> pendingPieces = new HashSet<>();
    private final ArrayList<Integer> nextPieces = new ArrayList<>();
    private final Set<Integer> pendingHashes = new HashSet<>();
    private final ArrayList<Integer> nextHashes = new ArrayList<>();

    public DownloadMngr(HashMngr hashMngr, FileMngr fileMngr, String hashAlg, int pieceSize, int piecesPerBlock, int hashesPerMsg) {
        this.hashMngr = hashMngr;
        this.fileMngr = fileMngr;
        this.hashAlg = hashAlg;
        this.pieceSize = pieceSize;
        this.piecesPerBlock = piecesPerBlock;
        this.hashesPerMsg = hashesPerMsg;
    }

    public void putHashes(Map<Integer, ByteBuffer> hashes, Set<Integer> missingHashes) {
        for (Map.Entry<Integer, ByteBuffer> hash : hashes.entrySet()) {
            hashMngr.writeHash(hash.getKey(), hash.getValue().array());
        }

        pendingHashes.removeAll(hashes.keySet());
        pendingHashes.removeAll(missingHashes);
        nextHashes.addAll(missingHashes);
    }

    public void putPiece(int pieceNr, ByteBuffer piece) {
        pendingPieces.remove(pieceNr);
        Pair<Integer, Integer> blockDetails = ManagedStoreHelper.blockDetails(pieceNr, piecesPerBlock);
        BlockMngr block = queuedBlocks.get(blockDetails.getValue0());
        if (block == null) {
            throw new RuntimeException("block logic error");
        }
        block.writePiece(blockDetails.getValue1(), piece.array());
    }

    public void resetPiece(int pieceNr) {
        pendingPieces.remove(pieceNr);
        nextPieces.add(pieceNr);
    }

    private Set<Integer> posToBlockNr(long pos, int size) {
        Set<Integer> result = new HashSet<>();
        int blockSize = pieceSize * piecesPerBlock;
        int blockNr = ManagedStoreHelper.componentNr(pos, blockSize);
        while (size > 0) {
            result.add(blockNr);
            blockNr++;
            size -= blockSize;
        }
        return result;
    }

    public void checkCompleteBlocks() {
        Set<Integer> completedBlocks = new HashSet<>();
        Map<Integer, ByteBuffer> resetBlocks = new HashMap<>();
        for (Map.Entry<Integer, BlockMngr> block : queuedBlocks.entrySet()) {
            int blockNr = block.getKey();
            if (!block.getValue().isComplete()) {
                continue;
            }
            if (!hashMngr.hasHash(blockNr)) {
                continue;
            }
            byte[] blockBytes = block.getValue().getBlock();
            byte[] blockHash = hashMngr.readHash(blockNr);
            if (HashUtil.checkHash(hashAlg, blockBytes, blockHash)) {
                fileMngr.writeBlock(blockNr, blockBytes);
                completedBlocks.add(blockNr);
            } else {
                resetBlocks.put(blockNr, ByteBuffer.wrap(blockBytes));
            }
        }
        for (Integer blockNr : completedBlocks) {
            queuedBlocks.remove(blockNr);
        }
        for (Integer blockNr : resetBlocks.keySet()) {
            queueBlock(blockNr);
        }
    }

    private void queueBlock(int blockNr) {
        int blockSize = fileMngr.blockSize(blockNr);
        BlockMngr blankBlock = StorageMngrFactory.inMemoryBlockMngr(blockSize, pieceSize);
        queuedBlocks.put(blockNr, blankBlock);

        for (int i = 0; i < blankBlock.nrPieces(); i++) {
            int pieceNr = blockNr * piecesPerBlock + i;
            nextPieces.add(pieceNr);
        }
    }

    public boolean download(int blockNr, int playBlockNr, int blocksAhead, int hashesAhead) {
        if (nextHashes.isEmpty() && nextPieces.isEmpty()) {
            if (fileMngr.isComplete(blockNr)) {
                if (fileMngr.isComplete(0)) {
                    return false;
                }
                blockNr = 0;
            }
            if (!prepareNew(blockNr, playBlockNr, blocksAhead, hashesAhead)) {
                if (!prepareNew(0, playBlockNr, blocksAhead, hashesAhead)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean prepareNew(int currentBlockNr, int playBlockNr, int blocksAhead, int hashesAhead) {
        int filePos = fileMngr.contiguous(currentBlockNr);
        int hashPos = hashMngr.contiguous(0);

        if (hashPos != -1) {
            if (filePos + hashesAhead > hashPos + pendingHashes.size()
                    || currentBlockNr > playBlockNr + blocksAhead) {
                Set<Integer> except = new HashSet<>();
                except.addAll(pendingHashes);
                except.addAll(nextHashes);
                Set<Integer> newNextHashes = hashMngr.nextHashes(hashPos, hashesPerMsg, except);
                nextHashes.addAll(newNextHashes);
                if (!nextHashes.isEmpty()) {
                    return true;
                }
            }
        }

        Integer nextBlockNr = fileMngr.nextBlock(currentBlockNr, queuedBlocks.keySet());
        if (nextBlockNr == -1) {
            nextBlockNr = fileMngr.nextBlock(0, queuedBlocks.keySet());
            if (nextBlockNr == -1) {
                return false;
            }
        }
        //last block might have less nr of pieces than default
        queueBlock(nextBlockNr);
        return !nextPieces.isEmpty();
    }

    public Optional<Integer> downloadData() {
        if (nextPieces.isEmpty()) {
            return Optional.absent();
        }
        Integer nextPiece = nextPieces.remove(0);
        pendingPieces.add(nextPiece);
        return Optional.of(nextPiece);
    }

    public Optional<Set<Integer>> downloadHash() {
        if (nextHashes.isEmpty()) {
            return Optional.absent();
        }
        Set<Integer> downloadHashes = new HashSet<>();
        for (int i = 0; i < hashesPerMsg && !nextHashes.isEmpty(); i++) {
            downloadHashes.add(nextHashes.remove(0));
        }
        pendingHashes.addAll(downloadHashes);
        return Optional.of(downloadHashes);
    }

    public int contiguousBlocks(int fromBlockNr) {
        return fileMngr.contiguous(fromBlockNr);
    }

    public boolean isComplete() {
        return hashMngr.isComplete(0) && fileMngr.isComplete(0);
    }

    @Override
    public String toString() {
        String status = "";
        status += "hash complete:" + hashMngr.isComplete(0) + " file complete:" + fileMngr.isComplete(0) + "\n";
        status += "pending hashes:" + pendingHashes.size() + " pending pieces:" + pendingPieces.size() + "\n";
        status += "next hashes:" + nextHashes.size() + " next pieces:" + nextPieces.size() + "\n";
        status += "queued blocks:" + queuedBlocks.keySet();
        return status;
    }
}

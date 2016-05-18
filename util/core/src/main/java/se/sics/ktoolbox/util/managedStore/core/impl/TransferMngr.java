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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.javatuples.Pair;
import se.sics.ktoolbox.util.managedStore.core.BlockMngr;
import se.sics.ktoolbox.util.managedStore.core.FileMngr;
import se.sics.ktoolbox.util.managedStore.core.HashMngr;
import se.sics.ktoolbox.util.managedStore.core.ManagedStoreHelper;
import se.sics.ktoolbox.util.managedStore.core.impl.util.PrepDwnlInfo;
import se.sics.ktoolbox.util.managedStore.core.util.HashUtil;
import se.sics.ktoolbox.util.managedStore.core.util.Torrent;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TransferMngr {

    //****************************EXTERNAL_STATE********************************
    private final Torrent torrent;
    private final HashMngr hashMngr;
    private final FileMngr fileMngr;
    //***************************INTERNAL_STATE*********************************
    private final Map<Integer, BlockMngr> queuedBlocks = new HashMap<>();
    private final Set<Integer> pendingPieces = new HashSet<>();
    private final ArrayList<Integer> nextPieces = new ArrayList<>();
    private final Set<Integer> pendingHashes = new HashSet<>();
    private final ArrayList<Integer> nextHashes = new ArrayList<>();

    public TransferMngr(Torrent torrent, HashMngr hashMngr, FileMngr fileMngr) {
        this.torrent = torrent;
        this.hashMngr = hashMngr;
        this.fileMngr = fileMngr;
    }

    public void writeHashes(Map<Integer, ByteBuffer> hashes, Set<Integer> missingHashes) {
        for (Map.Entry<Integer, ByteBuffer> hash : hashes.entrySet()) {
            hashMngr.writeHash(hash.getKey(), hash.getValue().array());
        }

        pendingHashes.removeAll(hashes.keySet());
        pendingHashes.removeAll(missingHashes);
        nextHashes.addAll(missingHashes);
    }

    public void writePiece(int pieceNr, ByteBuffer piece) {
        pendingPieces.remove(pieceNr);
        Pair<Integer, Integer> blockDetails = ManagedStoreHelper.componentDetails(pieceNr, torrent.torrentInfo.piecesPerBlock);
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
        int blockSize = torrent.torrentInfo.pieceSize * torrent.torrentInfo.piecesPerBlock;
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
            ByteBuffer blockBytes = ByteBuffer.wrap(block.getValue().getBlock());
            ByteBuffer blockHash = hashMngr.readHash(blockNr);
            if (HashUtil.checkHash(torrent.torrentInfo.hashAlg, blockBytes.array(), blockHash.array())) {
                fileMngr.writeBlock(blockNr, blockBytes);
                completedBlocks.add(blockNr);
            } else {
                resetBlocks.put(blockNr, blockBytes);
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
        BlockMngr blankBlock = StorageMngrFactory.inMemoryBlockMngr(blockSize, torrent.torrentInfo.pieceSize);
        queuedBlocks.put(blockNr, blankBlock);

        for (int i = 0; i < blankBlock.nrPieces(); i++) {
            int pieceNr = blockNr * torrent.torrentInfo.piecesPerBlock + i;
            nextPieces.add(pieceNr);
        }
    }

    public int prepareDownload(int targetBlockNr, PrepDwnlInfo prepInfo) {
        Set<Integer> excludeBlocks = new HashSet<>(queuedBlocks.keySet());
        Set<Integer> excludeHashes = new HashSet<>(pendingHashes);
        excludeHashes.addAll(nextHashes);

        int blockPos = fileMngr.nextBlock(targetBlockNr, excludeBlocks);
        int hashPos = hashMngr.nextHash(0, excludeHashes);

        boolean dwnlHashes = false;
        if (blockPos == -1) {
            blockPos = fileMngr.nextBlock(0, excludeBlocks);
            dwnlHashes = true;
        } else {
            boolean playBufferFilled = targetBlockNr + prepInfo.minBlockPlayBuffer <= blockPos;
            boolean minHashesAhead = hashPos + pendingHashes.size() + nextHashes.size() > blockPos + prepInfo.minAheadHashes;
            if (playBufferFilled || !minHashesAhead) {
                dwnlHashes = true;
            }
        }

        if (hashPos == -1 && blockPos == -1) {
            return -1;
        }

        queueBlock(blockPos);
        if (dwnlHashes && hashPos != -1) {
            int nrHashes = (blockPos > hashPos ? blockPos - hashPos : 0);
            nrHashes = nrHashes + prepInfo.hashesPerMsg * prepInfo.hashMsgPerRound;
            prepareNewHashes(hashPos, nrHashes);
        }
        int hashMsgs;
        if(nextHashes.isEmpty()) {
        hashMsgs = 0;
        } else {
            hashMsgs = nextHashes.size() % prepInfo.hashesPerMsg == 0 ? nextHashes.size() / prepInfo.hashesPerMsg : nextHashes.size() / prepInfo.hashesPerMsg + 1;
        }
        return nextPieces.size() + hashMsgs;
    }

    private void prepareNewHashes(int hashPos, int nrHashes) {
        Set<Integer> except = new HashSet<>();
        except.addAll(pendingHashes);
        except.addAll(nextHashes);
        Set<Integer> newNextHashes = hashMngr.nextHashes(hashPos, nrHashes, except);
        nextHashes.addAll(newNextHashes);
    }

    public Optional<Integer> downloadData() {
        if (nextPieces.isEmpty()) {
            return Optional.absent();
        }
        Integer nextPiece = nextPieces.remove(0);
        pendingPieces.add(nextPiece);
        return Optional.of(nextPiece);
    }

    public Optional<Set<Integer>> downloadHash(int nrHashes) {
        if (nextHashes.isEmpty()) {
            return Optional.absent();
        }
        Set<Integer> downloadHashes = new HashSet<>();
        for (int i = 0; i < nrHashes && !nextHashes.isEmpty(); i++) {
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
    
    public double percentageComplete() {
        return fileMngr.percentageCompleted();
    }
    
    @Override
    public String toString() {
        String status = "";
        status += "hash complete:" + hashMngr.isComplete(0) + " file complete:" + fileMngr.isComplete(0);
        status += " pending hashes:" + pendingHashes.size() + " pending pieces:" + pendingPieces.size();
        status += " next hashes:" + nextHashes.size() + " next pieces:" + nextPieces.size();
        status += " queued blocks:" + queuedBlocks.keySet();
        return status;
    }
}

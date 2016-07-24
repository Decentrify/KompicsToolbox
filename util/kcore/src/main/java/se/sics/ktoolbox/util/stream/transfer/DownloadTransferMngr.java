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
package se.sics.ktoolbox.util.stream.transfer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.reference.KReference;
import se.sics.ktoolbox.util.reference.KReferenceFactory;
import se.sics.ktoolbox.util.stream.StreamControl;
import se.sics.ktoolbox.util.stream.TransferMngr;
import se.sics.ktoolbox.util.stream.buffer.DelayedWrite;
import se.sics.ktoolbox.util.stream.buffer.NopDelayedWrite;
import se.sics.ktoolbox.util.stream.cache.DelayedRead;
import se.sics.ktoolbox.util.stream.cache.KHint;
import se.sics.ktoolbox.util.stream.ranges.KBlock;
import se.sics.ktoolbox.util.stream.ranges.KPiece;
import se.sics.ktoolbox.util.stream.storage.managed.AppendFileMngr;
import se.sics.ktoolbox.util.stream.util.FileDetails;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class DownloadTransferMngr implements StreamControl, TransferMngr.Writer, TransferMngr.Reader {

    private final FileDetails fileDetails;
    private final AppendFileMngr file;

    private final Map<Integer, BlockMngr> pendingBlocks = new HashMap<>();
    private final List<Integer> nextBlocks = new ArrayList<>();
    private KHint.Summary oldHint;
    private boolean moreWork = true;

    public DownloadTransferMngr(FileDetails fileDetails, AppendFileMngr file) {
        this.fileDetails = fileDetails;
        this.file = file;
    }

    //*********************************CONTROL**********************************
    @Override
    public void start() {
        file.start();
    }

    @Override
    public boolean isIdle() {
        return file.isIdle();
    }

    @Override
    public void close() {
        file.close();
    }

    public UploadTransferMngr complete() {
        return new UploadTransferMngr(fileDetails, file.complete());
    }

    //****************************TRANSFER_HINT_WRITE***************************
    @Override
    public KHint.Summary getFutureReads(int hintedBlockSpeed) {
        int newNext = hintedBlockSpeed - nextBlocks.size();
        if (newNext > 0) {
            Set<Integer> hintSet = new TreeSet<>();
            hintSet.addAll(pendingBlocks.keySet());
            hintSet.addAll(nextBlocks);
            Set<Integer> newBlocks = file.nextBlocksMissing(0, newNext, hintSet);
            newBlocks.addAll(file.nextHashesMissing(0, newNext, hintSet));
            if (!newBlocks.isEmpty()) {
                nextBlocks.addAll(newBlocks);
                hintSet.addAll(newBlocks);
                oldHint = new KHint.Summary(oldHint.lStamp + 1, hintSet);
                moreWork = true;
            } else {
                moreWork = false;
            }
        }
        return oldHint;
    }
    
    //*************************TRANSFER_HINT_READ*******************************
    @Override
    public void clean(Identifier reader) {
        file.clean(reader);
    }

    @Override
    public void setFutureReads(Identifier reader, KHint.Expanded hint) {
        file.setFutureReads(reader, hint);
    }

    //*********************************WRITER***********************************
    @Override
    public boolean moreWork() {
        return moreWork;
    }

    @Override
    public boolean pendingWork() {
        return moreWork || pendingBlocks.isEmpty();
    }
    
    /**
     * @param writeRange
     * @param value
     * @return true if block is complete, false otherwise
     */
    @Override
    public void writePiece(long pieceNr, byte[] value, DelayedWrite delayedResult) {
        KPiece piece = BlockHelper.getPieceRange(pieceNr, fileDetails);
        BlockMngr block = pendingBlocks.get(piece.parentBlock());
        block.writePiece(piece.blockPieceNr(), value);
        if (block.isComplete()) {
            KBlock blockRange = BlockHelper.getBlockRange(piece.parentBlock(), fileDetails);
            byte[] blockBytes = block.getBlock();
            KReference<byte[]> blockRef = KReferenceFactory.getReference(blockBytes);
            //TODO Alex - add keeping track of when write finish
            file.write(blockRange, blockRef, new NopDelayedWrite());
        }
    }
    
    @Override
    public void writeHash(int blockNr, byte[] value, DelayedWrite delayedResult) {
        KBlock hashRange = BlockHelper.getHashRange(blockNr, fileDetails);
        KReference<byte[]> hashVal = KReferenceFactory.getReference(value);
        file.writeHash(hashRange, hashVal, delayedResult);
    }
    
    //*********************************READER***********************************
     @Override
    public boolean hasBlock(int blockNr) {
        return file.hasBlock(blockNr);
    }

    @Override
    public boolean hasHash(int blockNr) {
        return file.hasBlock(blockNr);
    }
    
    @Override
    public void readHash(int blockNr, DelayedRead delayedResult) {
        KBlock hashRange = BlockHelper.getHashRange(blockNr, fileDetails);
        file.readHash(hashRange, delayedResult);
    }

    @Override
    public void readBlock(int blockNr, DelayedRead delayedResult) {
        KBlock blockRange = BlockHelper.getBlockRange(blockNr, fileDetails);
        file.read(blockRange, delayedResult);
    }
}

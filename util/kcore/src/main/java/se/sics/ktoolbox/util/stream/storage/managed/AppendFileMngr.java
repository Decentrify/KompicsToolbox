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
package se.sics.ktoolbox.util.stream.storage.managed;

import java.util.Set;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.managedStore.core.util.HashUtil;
import se.sics.ktoolbox.util.reference.KReference;
import se.sics.ktoolbox.util.result.Result;
import se.sics.ktoolbox.util.stream.FileMngr;
import se.sics.ktoolbox.util.stream.StreamControl;
import se.sics.ktoolbox.util.stream.buffer.DelayedWrite;
import se.sics.ktoolbox.util.stream.buffer.WriteResult;
import se.sics.ktoolbox.util.stream.cache.DelayedRead;
import se.sics.ktoolbox.util.stream.cache.KHint;
import se.sics.ktoolbox.util.stream.ranges.KBlock;
import se.sics.ktoolbox.util.stream.ranges.KRange;
import se.sics.ktoolbox.util.stream.storage.AsyncAppendStorage;
import se.sics.ktoolbox.util.stream.storage.AsyncOnDemandHashStorage;
import se.sics.ktoolbox.util.stream.tracker.ComponentTracker;
import se.sics.ktoolbox.util.stream.tracker.IncompleteTracker;
import se.sics.ktoolbox.util.stream.transfer.BlockHelper;
import se.sics.ktoolbox.util.stream.transfer.HashingException;
import se.sics.ktoolbox.util.stream.util.FileDetails;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class AppendFileMngr implements StreamControl, FileMngr.Reader, FileMngr.Writer {

    private final FileDetails fileDetails;
    private final AsyncAppendStorage file;
    private final ComponentTracker fileTracker;
    private final AsyncOnDemandHashStorage hash;
    private final ComponentTracker hashTracker;

    public AppendFileMngr(FileDetails fileDetails, AsyncAppendStorage file, AsyncOnDemandHashStorage hash) {
        this.fileDetails = fileDetails;
        this.file = file;
        this.fileTracker = IncompleteTracker.create(fileDetails.nrBlocks);
        this.hash = hash;
        this.hashTracker = IncompleteTracker.create(fileDetails.nrBlocks);
    }

    @Override
    public void start() {
        file.start();
        hash.start();
    }

    @Override
    public boolean isIdle() {
        return file.isIdle() && hash.isIdle();
    }

    @Override
    public void close() {
        file.close();
        hash.close();
    }

    public CompleteFileMngr complete() {
        return new CompleteFileMngr(fileDetails, file.complete(), hash);
    }

    //******************************BASIC_WRITE*********************************
    @Override
    public void write(final KBlock writeRange, final KReference<byte[]> val, final DelayedWrite delayedResult) {
        final int blockNr = writeRange.parentBlock();
        DelayedRead hashRead = new DelayedRead() {

            @Override
            public boolean fail(Result<KReference<byte[]>> result) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public boolean success(Result<KReference<byte[]>> result) {
                if (!result.isSuccess()) {
                    delayedResult.fail((Result) result);
                    return false;
                }
                validatedWrite(writeRange, result.getValue(), val, delayedResult);
                return true;
            }
        };

        KBlock hashRange = BlockHelper.getHashRange(blockNr, fileDetails);
        hash.read(hashRange, hashRead);
    }

    private void validatedWrite(final KBlock writeRange, KReference<byte[]> hash, KReference<byte[]> val, final DelayedWrite delayedResult) {
        if (HashUtil.checkHash(fileDetails.hashAlg, val.getValue().get(), hash.getValue().get())) {
            DelayedWrite pieceWrite = new DelayedWrite() {

                @Override
                public boolean fail(Result<WriteResult> result) {
                    return delayedResult.fail(result);
                }

                @Override
                public boolean success(Result<WriteResult> result) {
                    fileTracker.addComponent(writeRange.parentBlock());
                    return delayedResult.success(result);
                }
            };
            file.write(writeRange, val, delayedResult);
        } else {
            delayedResult.fail(Result.badRequest(new HashingException()));
        }
    }

    //*******************************BASIC_READ*********************************
    @Override
    public void read(KRange readRange, DelayedRead delayedResult) {
        file.read(readRange, delayedResult);
    }
    //***************************CACHE_HINT_READ*****************************
    @Override
    public void clean(Identifier reader) {
        file.clean(reader);
        hash.clean(reader);
    }

    @Override
    public void setFutureReads(Identifier reader, KHint.Expanded hint) {
        file.setFutureReads(reader, hint);
        hash.setFutureReads(reader, hint);
    }
    
    //*********************************READER***********************************
    @Override
    public boolean hasBlock(int blockNr) {
        return fileTracker.hasComponent(blockNr);
    }

    @Override
    public boolean hasHash(int blockNr) {
        return hashTracker.hasComponent(blockNr);
    }

    @Override
    public Set<Integer> nextBlocksMissing(int fromBlock, int nrBlocks, Set<Integer> except) {
        return fileTracker.nextComponentMissing(fromBlock, nrBlocks, except);
    }
    
    @Override
    public Set<Integer> nextHashesMissing(int fromBlock, int nrBlocks, Set<Integer> except) {
        return fileTracker.nextComponentMissing(fromBlock, nrBlocks, except);
    }
    
    @Override
    public void readHash(KBlock readRange, DelayedRead delayedResult) {
        hash.read(readRange, delayedResult);
    }
    
    //*******************************WRITER*************************************
    @Override
    public void writeHash(final KBlock writeRange, KReference<byte[]> val, final DelayedWrite delayedResult) {
        DelayedWrite hashResult = new DelayedWrite() {

            @Override
            public boolean success(Result<WriteResult> result) {
                hashTracker.addComponent(writeRange.parentBlock());
                return delayedResult.success(result);
            }

            @Override
            public boolean fail(Result<WriteResult> result) {
                return delayedResult.fail(result);
            }
        };
        hash.write(writeRange, val, hashResult);
    }
}

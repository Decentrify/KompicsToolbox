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
package se.sics.ktoolbox.hops.managedStore.storage.cache;

import com.google.common.util.concurrent.SettableFuture;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.javatuples.Pair;
import se.sics.ktoolbox.util.managedStore.core.ManagedStoreHelper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HDFSDriver implements WriteDriverI, ReadDriverI{

    private static final int MAX_THREADS = 5;
    //**************************************************************************
    private final KCache cache = new OneReaderInOrderCache();
    private final ExecutorService hdfsReaders = Executors.newFixedThreadPool(MAX_THREADS);
    private final ScheduledExecutorService resultIngestor = Executors.newSingleThreadScheduledExecutor();
    //*********************************AUX**************************************
    private Pair<ReadOp, SettableFuture<ByteBuffer>> blockingRead;

    public HDFSDriver() {
    }
    
    //*****************************WriteDriverI*********************************
    public void success(ReadOp op, ByteBuffer result) {
        cache.write(op.getRange(), result);
        if(blockingRead != null) {
            byte[] result = BlockReader.read(result, op.getRange(), blockingRead.getValue0().getRange());
            
        }
    }
    public void fail(ReadOp op, Exception ex);
    
    private synchronized void ingest() {
        while (!successResult.isEmpty()) {
            Pair<ReadOp, ByteBuffer> read = successResult.poll();
            if (read != null) {
                
            }
        }
        if (!failResult.isEmpty()) {
            Pair<ReadOp, IOException> read = failResult.poll();
            if (read != null) {
                throw new RuntimeException("HDFS read fails", read.getValue1());
            }
        }
        if(blockingRead != null) {
            resultIngestor
        }
    }

    public synchronized SettableFuture<ByteBuffer> hopsRead(long readPos, int readLength) {
        SettableFuture<ByteBuffer> futureResult = SettableFuture.create();
        ReadOp read = new ReadOp(readPos, readLength);
        byte[] result = cache.read(read.getRange());
        if (result != null) {
            futureResult.set(ByteBuffer.wrap(result));
        } else {
            blockingRead = Pair.with(read, futureResult);
        }
        return futureResult;
    }

    public synchronized void nextOp(int pieceSize, int piecesPerBlock) {
        if()
        ManagedStoreHelper.blockDetails()
    }
    
    
}

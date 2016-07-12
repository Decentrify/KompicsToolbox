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
package se.sics.ktoolbox.hdfs.cache;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SettableFuture;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.hdfs.HDFSHelper;
import se.sics.ktoolbox.hdfs.HDFSResource;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.managedStore.core.ManagedStoreHelper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HDFSReadCacheImpl implements HDFSReadCache, ReadDriverI {
    private static final Logger LOG = LoggerFactory.getLogger(HDFSReadCacheImpl.class);
    private String logPrefix;

    //**************************************************************************
    private final HDFSReadCacheKConfig cacheConfig;
    private final HDFSResource readResource;
    //**********************************AUX*************************************
    private final long fileSize;
    private final Pair<Integer, Integer> lastBlock;
    //**************************************************************************
    private HashMap<Identifier, ReaderDiskHead> readers = new HashMap<>();
    private Pair<ReadOp, SettableFuture<ByteBuffer>> blockingRead;
    //**************************************************************************
    private final ExecutorService hdfsReaders;
    //************************ENSURE_THREAD_SAFETY******************************
    private final TreeMap<Integer, CachedBlock> cache = new TreeMap<>();

    public HDFSReadCacheImpl(Config config, HDFSResource readResource, long setFileSize) {
        this.cacheConfig = new HDFSReadCacheKConfig(config);
        this.readResource = readResource;
        if (setFileSize == -1) {
            fileSize = HDFSHelper.length(readResource);
        } else {
            fileSize = setFileSize;
        }
        lastBlock = ManagedStoreHelper.lastBlock(fileSize, cacheConfig.defaultBlockSize);
        hdfsReaders = Executors.newFixedThreadPool(cacheConfig.maxThreads);
    }

    public void tearDown() {
        hdfsReaders.shutdownNow();
        try {
            boolean completed = hdfsReaders.awaitTermination(10, TimeUnit.SECONDS);
            if (!completed) {
                throw new RuntimeException("could not kill cache correctly");
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException("could not kill cache correctly");
        }
    }

    //*****************************ReadDriverI**********************************
    @Override
    public synchronized void success(ReadOp op, ByteBuffer result) {
        CachedBlock cb = cache.get(op.blockNr);
        if (cb == null) {
            return;
        }
        cb.setVal(result);
        checkIfCacheHit(cb);
    }

    private void checkIfCacheHit(CachedBlock cb) {
        if (blockingRead != null && blockingRead.getValue0().blockNr == cb.blockNr) {
            long readPos = blockingRead.getValue0().readPos;
            int readLength = blockingRead.getValue0().readLength;

            int blockOffset = ManagedStoreHelper.blockDetails(readPos, cacheConfig.defaultPiecesPerBlock, cacheConfig.defaultPieceSize).getValue1().getValue1();
            byte[] readResult = cb.read(blockOffset, readLength);
            if (readResult == null) {
                throw new RuntimeException("logic error while reading from block");
            }
            blockingRead.getValue1().set(ByteBuffer.wrap(readResult));
            blockingRead = null;
        }
    }

    @Override
    public synchronized void fail(ReadOp op, Exception ex) {
        throw new RuntimeException(ex);
    }

    //*******************************HDFSReadCache********************************
    @Override
    public synchronized void writeBlock(int blockNr, byte[] data) {
        CachedBlock cb = new CachedBlock(blockNr);
        cb.setVal(ByteBuffer.wrap(data));
        cache.put(blockNr, cb);
    }
    
    @Override
    public synchronized SettableFuture<ByteBuffer> read(Identifier reader, long readPos, int readLength, Set<Integer> cacheBlocks) {
        ReaderDiskHead rdh = readers.get(reader);
        if (rdh == null) {
            if (readers.size() > cacheConfig.maxReaders) {
                throw new RuntimeException("too many readers");
            }
            rdh = new ReaderDiskHead();
            readers.put(reader, rdh);
        }
        rdh.setCache(cacheBlocks);
        SettableFuture futureResult = rdh.read(readPos, readLength);
        return futureResult;
    }

    @Override
    public synchronized void stopRead(Identifier reader) {
        ReaderDiskHead rdh = readers.remove(reader);
        rdh.clear();
    }

    //**************************************************************************
    private CachedBlock readBlock(int blockNr) {
        CachedBlock cb = cache.get(blockNr);
        if (cb == null) {
            LOG.info("reading block:{}", blockNr);
            if (cache.size() > cacheConfig.cacheSize) {
                cleanCache();
            }
            cb = new CachedBlock(blockNr);
            cache.put(blockNr, cb);

            long readPos = (long) blockNr * cacheConfig.defaultBlockSize;
            int readLength = lastBlock.getValue0().equals(blockNr) ? lastBlock.getValue1() : cacheConfig.defaultBlockSize;
            ReadOp readOp = new ReadOp(blockNr, readPos, readLength);
            hdfsReaders.submit(new HDFSReadTask(readResource, readOp, this));
        }
        cb.incRef();
        return cb;
    }

    private void cleanCache() {
        Iterator<Map.Entry<Integer, CachedBlock>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, CachedBlock> next = it.next();
            if (next.getValue().canClean()) {
                it.remove();
                return;
            }
        }
        throw new RuntimeException("unexpected logic - too many readers?");
    }

    public static class CachedBlock {

        public final int blockNr;
        private int references;
        private ByteBuffer val;

        public CachedBlock(int blockNr) {
            this.blockNr = blockNr;
            this.references = 0;
        }

        public void incRef() {
            references++;
        }

        public void releaseRef() {
            references--;
        }

        public boolean canClean() {
            return references == 0;
        }

        public void setVal(ByteBuffer val) {
            this.val = val;
        }

        public boolean isAvailable() {
            return val != null;
        }

        public byte[] read(int readPos, int readLength) {
            val.position(readPos);
            byte[] result = new byte[readLength];
            val.get(result);
            return result;
        }
    }

    public class ReaderDiskHead {

        private final Map<Integer, CachedBlock> cacheReferences = new HashMap<>();

        public void setCache(Set<Integer> cacheBlocks) {
            Set<Integer> clearBlocks = new TreeSet<>(Sets.difference(cacheReferences.keySet(), cacheBlocks));
            Set<Integer> getBlocks = new HashSet<>(Sets.difference(cacheBlocks, cacheReferences.keySet()));

            Iterator<Integer> it = clearBlocks.iterator();
            while (it.hasNext() && cacheReferences.size() > (cacheConfig.readWindowSize + getBlocks.size())) {
                int blockNr = it.next();
                CachedBlock cb = cacheReferences.remove(blockNr);
                cb.releaseRef();
            }

            for (Integer blockNr : getBlocks) {
                cacheReferences.put(blockNr, HDFSReadCacheImpl.this.readBlock(blockNr));
                
            }
        }

        public SettableFuture<ByteBuffer> read(long readPos, int readLength) {
            Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> blockDetails = ManagedStoreHelper.blockDetails(readPos,
                    HDFSReadCacheImpl.this.cacheConfig.defaultPiecesPerBlock, HDFSReadCacheImpl.this.cacheConfig.defaultPieceSize);
            int blockNr = blockDetails.getValue1().getValue0();
            int blockOffset = blockDetails.getValue1().getValue1() * HDFSReadCacheImpl.this.cacheConfig.defaultPieceSize;

            //read pieces only
            assert blockDetails.getValue0().getValue1() == 0;

            ReadOp op = new ReadOp(blockNr, readPos, readLength);
            SettableFuture<ByteBuffer> futureResult = SettableFuture.create();

            CachedBlock cb = cacheReferences.get(blockNr);
            if (cb == null) {
                throw new RuntimeException("logic error - block should be here after setting the cache");
            }
            if (cb.isAvailable()) {
                LOG.debug("hitting cache for block:{}", blockNr);
                byte[] result = cb.read(blockOffset, readLength);
                futureResult.set(ByteBuffer.wrap(result));
            } else {
                LOG.info("waiting for block:{}", blockNr);
                HDFSReadCacheImpl.this.blockingRead = Pair.with(op, futureResult);
            }
            return futureResult;
        }

        public void clear() {
            for (CachedBlock cb : cacheReferences.values()) {
                cb.releaseRef();
            }
        }
    }
}

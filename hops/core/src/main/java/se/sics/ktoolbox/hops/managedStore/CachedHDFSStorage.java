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

import com.google.common.util.concurrent.SettableFuture;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.hdfs.HDFSHelper;
import se.sics.ktoolbox.hdfs.buffer.HDFSAppendBuffer;
import se.sics.ktoolbox.hdfs.buffer.HDFSAppendBufferImpl;
import se.sics.ktoolbox.hdfs.cache.HDFSReadCacheImpl;
import se.sics.ktoolbox.hdfs.cache.HDFSReadCacheKConfig;
import se.sics.ktoolbox.hdfs.HDFSResource;
import se.sics.ktoolbox.kafka.KafkaResource;
import se.sics.ktoolbox.util.BKOutputStream;
import se.sics.ktoolbox.kafka.parser.KafkaProducer;
import se.sics.ktoolbox.util.RABKOuputStreamImpl;
import se.sics.ktoolbox.util.RABKOutputStream;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.managedStore.core.ManagedStoreHelper;
import se.sics.ktoolbox.util.managedStore.core.Storage;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CachedHDFSStorage implements Storage {

    private static final Logger LOG = LoggerFactory.getLogger(Storage.class);
    private String logPrefix;

    private final HDFSResource hdfsResource;
    private final KafkaResource kafkaResource;
    private final HDFSReadCacheKConfig cacheConfig;

    private final HDFSAppendBuffer hfdsBuffer;
    private final HDFSReadCacheImpl hdfsCache;
    private final RABKOutputStream outputStream;

    private final long fileSize;

    public CachedHDFSStorage(Config config, HDFSResource hdfsResource, KafkaResource kafkaResource, long fileSize) {
        this.hdfsResource = hdfsResource;
        this.kafkaResource = kafkaResource;
        long appendPos = HDFSHelper.length(hdfsResource);
        if (appendPos == -1) {
            boolean result = HDFSHelper.simpleCreate(hdfsResource);
            if (!result) {
                throw new RuntimeException("cannot create file");
            }
            appendPos = 0;
        }
        if (fileSize == -1) {
            if (appendPos == 0) {
                throw new RuntimeException("logic error");
            }
            this.fileSize = appendPos;
        } else {
            this.fileSize = fileSize;
        }

        cacheConfig = new HDFSReadCacheKConfig(config);
        if (appendPos < fileSize) {
            if (appendPos != 0) {
                throw new RuntimeException("resume not supported yet");
            } else {
                int blockNr = ManagedStoreHelper.blockNr(appendPos, cacheConfig.defaultPieceSize, cacheConfig.defaultPiecesPerBlock);
                hfdsBuffer = new HDFSAppendBufferImpl(config, hdfsResource, blockNr);
                if (kafkaResource != null) {
                    List<BKOutputStream> outputStreams = new ArrayList<>();
                    outputStreams.add(new KafkaProducer(kafkaResource));
                    outputStream = new RABKOuputStreamImpl(outputStreams, appendPos);
                } else { 
                    outputStream = null;
                }
            }
        } else {
            hfdsBuffer = null;
            outputStream = null;
        }
        hdfsCache = new HDFSReadCacheImpl(config, hdfsResource, fileSize);
    }

    @Override
    public void tearDown() {
        if (outputStream != null) {
            outputStream.terminate();
        }
        if (hfdsBuffer != null && !hfdsBuffer.isEmpty()) {
            SettableFuture<Boolean> waitForBuffer = hfdsBuffer.waitEmpty();
            try {
                waitForBuffer.get();
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        }
        hdfsCache.tearDown();
    }

    @Override
    public long length() {
        return fileSize;
    }

    @Override
    public int write(long writePos, byte[] bytes) {
        if (hfdsBuffer == null) {
            throw new RuntimeException("unexpected write");
        }
        int blockNr = ManagedStoreHelper.blockNr(writePos, cacheConfig.defaultPieceSize, cacheConfig.defaultPiecesPerBlock);
        int bytesWritten = hfdsBuffer.writeBlock(blockNr, bytes);
        if (outputStream != null) {
            //TODO Alex - fix
            outputStream.write(writePos, bytes);
        }
        return bytesWritten;
    }

    @Override
    public byte[] read(Identifier readerId, long readPos, int readLength, Set<Integer> cacheBlocks) {
        if (hfdsBuffer != null) {
            int blockNr = ManagedStoreHelper.blockNr(readPos, cacheConfig.defaultPieceSize, cacheConfig.defaultPiecesPerBlock);
            byte[] bufferedBlock = hfdsBuffer.readBlock(blockNr);
            if (bufferedBlock != null) {
                hdfsCache.writeBlock(blockNr, bufferedBlock);
            }
        }
        SettableFuture<ByteBuffer> futureResult = hdfsCache.read(readerId, readPos, readLength, cacheBlocks);
        ByteBuffer result;
        try {
            result = futureResult.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
        return result.array();
    }
}

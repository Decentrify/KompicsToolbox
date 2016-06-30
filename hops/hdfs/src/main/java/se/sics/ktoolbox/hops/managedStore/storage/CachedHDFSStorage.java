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
package se.sics.ktoolbox.hops.managedStore.storage;

import com.google.common.util.concurrent.SettableFuture;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.hops.managedStore.storage.buffer.HDFSAppendBuffer;
import se.sics.ktoolbox.hops.managedStore.storage.buffer.HDFSAppendBufferImpl;
import se.sics.ktoolbox.hops.managedStore.storage.cache.HDFSReadCacheImpl;
import se.sics.ktoolbox.hops.managedStore.storage.cache.HDFSReadCacheKConfig;
import se.sics.ktoolbox.hops.managedStore.storage.util.HDFSResource;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.managedStore.core.ManagedStoreHelper;
import se.sics.ktoolbox.util.managedStore.core.Storage;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CachedHDFSStorage implements Storage {

    private static final Logger LOG = LoggerFactory.getLogger(Storage.class);
    private String logPrefix;

    private final HDFSResource resource;
    private final String user;

    private final HDFSAppendBuffer buffer;
    private final HDFSReadCacheImpl cache;
    private final HDFSReadCacheKConfig cacheConfig;

    private final long fileSize;

    public CachedHDFSStorage(Config config, HDFSResource resource, String user, long fileSize) {
        this.resource = resource;
        this.user = user;
        long appendPos = HDFSHelper.length(resource, user);
        if (appendPos == -1) {
            boolean result = HDFSHelper.simpleCreate(resource, user);
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
                buffer = new HDFSAppendBufferImpl(config, resource, user, blockNr);
            }
        } else {
            buffer = null;
        }
        cache = new HDFSReadCacheImpl(config, resource, user, fileSize);
    }

    @Override
    public void tearDown() {
        if (buffer != null && !buffer.isEmpty()) {
            SettableFuture<Boolean> waitForBuffer = buffer.waitEmpty();
            try {
                waitForBuffer.get();
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        }
        cache.tearDown();
    }

    @Override
    public long length() {
        return fileSize;
    }

    @Override
    public int write(long writePos, byte[] bytes) {
        if (buffer == null) {
            throw new RuntimeException("unexpected write");
        }
        int blockNr = ManagedStoreHelper.blockNr(writePos, cacheConfig.defaultPieceSize, cacheConfig.defaultPiecesPerBlock);
        int bytesWritten = buffer.writeBlock(blockNr, bytes);
        return bytesWritten;
    }

    @Override
    public byte[] read(Identifier readerId, long readPos, int readLength, Set<Integer> cacheBlocks) {
        if (buffer != null) {
            int blockNr = ManagedStoreHelper.blockNr(readPos, cacheConfig.defaultPieceSize, cacheConfig.defaultPiecesPerBlock);
            byte[] bufferedBlock = buffer.readBlock(blockNr);
            if (bufferedBlock != null) {
                cache.writeBlock(blockNr, bufferedBlock);
            }
        }
        SettableFuture<ByteBuffer> futureResult = cache.read(readerId, readPos, readLength, cacheBlocks);
        ByteBuffer result;
        try {
            result = futureResult.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
        return result.array();
    }
}

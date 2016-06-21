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
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.hops.managedStore.storage.HDFSHelper;
import se.sics.ktoolbox.hops.managedStore.storage.util.HDFSResource;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.managedStore.core.Storage;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CachedHDFSStorage implements Storage {

    private static final Logger LOG = LoggerFactory.getLogger(Storage.class);
    private String logPrefix;

    private final HDFSResource resource;
    private final String user;

    private final HDFSCache cache;

    private final long fileSize;
    private long appendPos;

    public CachedHDFSStorage(Config config, HDFSResource resource, String user, long fileSize) {
        this.resource = resource;
        this.user = user;
        this.appendPos = HDFSHelper.length(resource, user);
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
        HDFSCacheKCWrapper cacheConfig = new HDFSCacheKCWrapper(config);
        cache = new HDFSCache(cacheConfig, resource, user, fileSize);
    }

    @Override
    public void tearDown() {
        cache.tearDown();
    }

    @Override
    public long length() {
        return fileSize;
    }

    @Override
    public int write(long writePos, byte[] bytes) {
        if (writePos < appendPos) {
            throw new RuntimeException("can only append to HDFS");
        }
        int bytesWritten = HDFSHelper.append(resource, user, bytes);
        appendPos += bytesWritten;
        return bytesWritten;
    }

    @Override
    public byte[] read(Identifier readerId, long readPos, int readLength, Set<Integer> cacheBlocks) {
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

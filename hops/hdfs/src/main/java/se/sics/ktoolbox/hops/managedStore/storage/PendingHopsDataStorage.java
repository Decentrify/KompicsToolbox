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

import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.hops.managedStore.storage.util.HDFSResource;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.managedStore.core.Storage;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class PendingHopsDataStorage implements Storage {

    private static final Logger LOG = LoggerFactory.getLogger(Storage.class);
    private String logPrefix;

    private final HDFSResource resource;
    private final String user;
    private final long fileSize;
    private long appendPos;

    public PendingHopsDataStorage(HDFSResource resource, String user, long fileSize) {
        this.resource = resource;
        this.user = user;
        this.fileSize = fileSize;
        this.appendPos = HDFSHelper.length(resource, user);
    }

    @Override
    public void tearDown() {
    }

    @Override
    public long length() {
        return fileSize;
    }

    @Override
    public byte[] read(Identifier readerId, final long readPos, final int readLength, Set<Integer> bufferBlocks) {
        try {
            byte[] result = HDFSHelper.read(resource, user, readPos, readLength);
            return result;
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * hops can only do append
     *
     * @param writePos
     * @param bytes
     * @return
     */
    @Override
    public int write(long writePos, byte[] bytes) {
        if (appendPos != writePos) {
            throw new RuntimeException("hops can only append");
        }

        int writtenBytes = append(bytes);
        appendPos += writtenBytes;

        return writtenBytes;
    }

    private int append(byte[] bytes) {
        return HDFSHelper.append(resource, user, bytes);
    }
}

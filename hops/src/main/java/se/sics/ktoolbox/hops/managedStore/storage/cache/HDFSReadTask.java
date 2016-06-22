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

import java.io.IOException;
import java.nio.ByteBuffer;
import se.sics.ktoolbox.hops.managedStore.storage.HDFSHelper;
import se.sics.ktoolbox.hops.managedStore.storage.util.HDFSResource;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HDFSReadTask implements Runnable {

    private final HDFSResource resource;
    private final String user;
    private final ReadOp readOp;
    private final ReadDriverI callback;

    public HDFSReadTask(HDFSResource resource, String user, ReadOp readOp, ReadDriverI callback) {
        this.resource = resource;
        this.user = user;
        this.readOp = readOp;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            byte[] byte_read = HDFSHelper.read(resource, user, readOp.readPos, readOp.readLength);
            callback.success(readOp, ByteBuffer.wrap(byte_read));
        } catch (IOException | InterruptedException ex) {
            callback.fail(readOp, ex);
        }
    }
}

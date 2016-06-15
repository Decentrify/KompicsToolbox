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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.javatuples.Pair;
import se.sics.ktoolbox.hops.managedStore.storage.util.HDFSResource;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HDFSReadTask implements Runnable {

    private final HDFSResource resource;
    private final ReadOp readOp;
    private final ConcurrentLinkedQueue<Pair<ReadOp, ByteBuffer>> successResult;
    private final ConcurrentLinkedQueue<Pair<ReadOp, IOException>> failResult;

    public HDFSReadTask(HDFSResource resource, ReadOp readOp, ConcurrentLinkedQueue<Pair<ReadOp, ByteBuffer>> successResult, 
            ConcurrentLinkedQueue<Pair<ReadOp, IOException>> failResult) {
        this.resource = resource;
        this.readOp = readOp;
        this.successResult = successResult;
        this.failResult = failResult;
    }

    @Override
    public void run() {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "hdfs");
        String filePath = resource.dirPath + Path.SEPARATOR + resource.fileName;
        try (DistributedFileSystem fs = (DistributedFileSystem) FileSystem.get(conf);
                FSDataInputStream in = fs.open(new Path(filePath))) {
            byte[] byte_read = new byte[readOp.readLength];
            in.readFully(readOp.readPos, byte_read);
            successResult.add(Pair.with(readOp, ByteBuffer.wrap(byte_read)));
        } catch (IOException ex) {
            failResult.add(Pair.with(readOp, ex));
        }
    }
}

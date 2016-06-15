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
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.util.managedStore.core.Storage;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class PendingHopsDataStorage implements Storage {

    private static final Logger LOG = LoggerFactory.getLogger(Storage.class);
    private String logPrefix;

    protected final String hopsURL;
    protected final String filePath;
    private final String user;
    protected DistributedFileSystem fs;
    protected FSDataOutputStream out;
    protected FSDataInputStream in;
    private long appendPos;
    private final long length;

    public PendingHopsDataStorage(String user, String hopsURL, String filePath, long fileLength) {
        this.hopsURL = hopsURL;
        this.filePath = filePath;
        this.user = user;
        this.appendPos = 0;
        this.length = fileLength;
        setup();
    }

    private void setup() {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hopsURL);
        try {
            fs = (DistributedFileSystem) FileSystem.get(conf);
            out = fs.create(new Path(filePath));
            in = fs.open(new Path(filePath));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (ClassCastException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void tearDown() {
        try {
            this.out.close();
            this.in.close();
            this.fs.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    @Override
    public long length() {
        return length;
    }

    @Override
    public byte[] read(final long readPos, final int readLength) {
        try {
           return HDFSHelper.read(user, in, readPos, readLength);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (InterruptedException ex) {
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
        try {
            return HDFSHelper.append(user, out, bytes);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}

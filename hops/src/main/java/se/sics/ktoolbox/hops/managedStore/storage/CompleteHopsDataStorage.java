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
import java.nio.ByteBuffer;
import java.util.LinkedList;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.util.managedStore.core.ManagedStoreHelper;
import se.sics.ktoolbox.util.managedStore.core.Storage;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CompleteHopsDataStorage implements Storage {

    private static final Logger LOG = LoggerFactory.getLogger(Storage.class);
    private String logPrefix;

    protected final String hopsURL;
    protected final String filePath;
    private final String user;
    protected FileSystem fs;
    protected FSDataInputStream in;
    protected long length;

    public CompleteHopsDataStorage(String user, String hopsURL, String filePath) {
        this.hopsURL = hopsURL;
        this.filePath = filePath;
        this.user = user;
        setup();
    }

    private void setup() {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hopsURL);
        try {
            fs = HDFSHelper.getFileSystem(hopsURL, user);
            in = fs.open(new Path(filePath));
            length = HDFSHelper.length(hopsURL, filePath, user);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (ClassCastException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void tearDown() {
        try {
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
    public byte[] read(long readPos, int readLength) {
        return hopsRead(readPos, readLength);
//        return bufferedRead(readPos, readLength);
    }

    private byte[] hopsRead(long readPos, int readLength) {
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
        throw new RuntimeException("hops completed can only read");
    }
}

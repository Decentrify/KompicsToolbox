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
import se.sics.ktoolbox.util.managedStore.core.Storage;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public abstract class HopsDataStorage implements Storage {

    protected final String hopsURL;
    protected final String filePath;
    protected DistributedFileSystem fs;

    public HopsDataStorage(String hopsURL, String filePath) {
        this.hopsURL = hopsURL;
        this.filePath = filePath;
        this.fs = getFileSystem();
    }

    private DistributedFileSystem getFileSystem() {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hopsURL);
        try {
            return (DistributedFileSystem) FileSystem.get(conf);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (ClassCastException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public byte[] read(final long readPos, final int readLength) {
        try (FSDataInputStream fdis = this.fs.open(new Path(filePath))) {
            byte[] byte_read = new byte[readLength];
            fdis.readFully(byte_read, (int) readPos, readLength);
            fdis.close();
            return byte_read;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int write(long writePos, byte[] bytes) {
        try(FSDataOutputStream out = this.fs.create(new Path(filePath))) {
            out.write(bytes, (int) writePos, bytes.length);
            out.close();
            return bytes.length;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}

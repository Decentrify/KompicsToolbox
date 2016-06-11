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
    protected DistributedFileSystem fs;
    protected FSDataInputStream in;
    protected long length;

    public CompleteHopsDataStorage(String hopsURL, String filePath) {
        this.hopsURL = hopsURL;
        this.filePath = filePath;
        setup();
    }

    private void setup() {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hopsURL);
        try {
            fs = (DistributedFileSystem) FileSystem.get(conf);
            in = fs.open(new Path(filePath));
            length = fs.getLength(new Path(filePath));
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
        return bufferedRead(readPos, readLength);
    }

    private byte[] hopsRead(long readPos, int readLength) {
        try {
            byte[] byte_read = new byte[readLength];
            in.readFully(readPos, byte_read);
            return byte_read;
        } catch (IOException ex) {
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

    //small hack
    private static final int pieceSize = 1024;
    private static final int piecesPerBlock = 1024;
    private static final int blockSize = pieceSize * piecesPerBlock;
    private final LinkedList<Pair<Integer, ByteBuffer>> buffer = new LinkedList<>();

    private byte[] bufferedRead(long readPos, int readLength) {
        Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> blockDetails = ManagedStoreHelper.blockDetails(readPos, piecesPerBlock, pieceSize);
        if (readLength == blockSize) {
            if (blockDetails.getValue1().getValue1() == 0 && blockDetails.getValue0().getValue1() == 0) {
                //beginning of block
                byte[] block = hopsRead(readPos, readLength);
                int blockNr = blockDetails.getValue1().getValue0();
                buffer.addLast(Pair.with(blockNr, ByteBuffer.wrap(block)));
                if (buffer.size() > 5) {
                    buffer.removeFirst();
                }
                return block;
            }
        }

        for (Pair<Integer, ByteBuffer> bufferedBlock : buffer) {
            if (bufferedBlock.getValue0().equals(blockDetails.getValue1().getValue0())) {
                byte[] readResult = new byte[readLength];
                bufferedBlock.getValue1().position(blockDetails.getValue1().getValue1());
                bufferedBlock.getValue1().get(readResult);
                return readResult;
            }
        }
        return hopsRead(readPos, readLength);
    }
}

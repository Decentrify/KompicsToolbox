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
package se.sics.ktoolbox.hdfs.buffer;

import com.google.common.util.concurrent.SettableFuture;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.hdfs.HDFSResource;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HDFSAppendBufferImpl implements HDFSAppendBuffer, AppendDriverI {

    private static final Logger LOG = LoggerFactory.getLogger(HDFSAppendBufferImpl.class);
    private String logPrefix;

    //**************************************************************************
    private final HDFSAppendBufferKConfig bufferConfig;
    private final HDFSResource resource;
    //**************************************************************************
    private int appendBlockNr;
    private final ExecutorService hdfsWriters;
    private SettableFuture<Boolean> waitEmpty;
    //************************ENSURE_THREAD_SAFETY******************************
    private final TreeMap<Integer, ByteBuffer> buffer = new TreeMap<>();

    public HDFSAppendBufferImpl(Config config, HDFSResource resource, int appendBlockNr) {
        bufferConfig = new HDFSAppendBufferKConfig(config);
        this.resource = resource;
        hdfsWriters = Executors.newFixedThreadPool(1);
        this.appendBlockNr = appendBlockNr;
    }

    //****************************HDFSAppendBuffer*******************************
    @Override
    public synchronized int writeBlock(int blockNr, byte[] data) {
        buffer.put(blockNr, ByteBuffer.wrap(data));
        temptativeWrite(blockNr, data);
        return data.length;
    }

    @Override
    public synchronized byte[] readBlock(int blockNr) {
        ByteBuffer bb = buffer.get(blockNr);
        if (bb == null) {
            return null;
        } else {
            return bb.array();
        }
    }
    
    public synchronized boolean isEmpty() {
        return buffer.isEmpty();
    }

    @Override
    public synchronized SettableFuture<Boolean> waitEmpty() {
        if(waitEmpty != null) {
            throw new RuntimeException("two call to waitEmpty");
        }
        waitEmpty = SettableFuture.create();
        return waitEmpty;
    }
    
    //******************************AppendDriverI*******************************
    @Override
    public synchronized void success(int blockNr) {
        if (blockNr != appendBlockNr) {
            throw new RuntimeException("logic exception");
        }
        ByteBuffer bb = buffer.remove(blockNr);
        appendBlockNr++;
        
        if (!buffer.isEmpty()) {
            Map.Entry<Integer, ByteBuffer> first = buffer.firstEntry();
            temptativeWrite(first.getKey(), first.getValue().array());
        }
        if(buffer.isEmpty() && waitEmpty != null) {
            waitEmpty.set(true);
        }
    }

    @Override
    public synchronized void fail(int blockNr, Exception ex) {
        if(waitEmpty != null) {
            waitEmpty.set(true);
        }
        throw new RuntimeException(ex);
    }

    //**************************************************************************
    private void temptativeWrite(int blockNr, byte[] data) {
        if (blockNr == appendBlockNr) {
            hdfsWriters.submit(new HDFSAppendTask(resource, this, blockNr, data));
        }
    }
}

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
package se.sics.ktoolbox.util;

import com.google.common.io.BaseEncoding;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RandomAccessBufferedKompicsOutputStream buffered append stream out with
 * random access capabilities(buffered until append is possible)
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class RABKOuputStreamImpl implements RABKOutputStream {
    private static final Logger LOG = LoggerFactory.getLogger(RABKOuputStreamImpl.class);
    private final List<BKOutputStream> outStreams;
    private long writePos;
    private final TreeMap<Long, byte[]> buffer = new TreeMap<>();

    public RABKOuputStreamImpl(List<BKOutputStream> outStreams, long startWritePos) {
        this.outStreams = outStreams;
        this.writePos = startWritePos;
    }

    private void outWrite(byte[] data) {
        LOG.info("stream:{}", BaseEncoding.base16().encode(data));
        for (BKOutputStream outStream : outStreams) {
            outStream.write(data);
        }
        writePos += data.length;
    }

    private void digestBuffer() {
        Iterator<Map.Entry<Long, byte[]>> it = buffer.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, byte[]> next = it.next();
            if (next.getKey().equals(writePos)) {
                outWrite(next.getValue());
                it.remove();
            } else {
                break;
            }
        }
    }

    @Override
    public void write(byte[] data) {
        outWrite(data);
        digestBuffer();
    }

    @Override
    public void write(long pos, byte[] data) {
        if (writePos == pos) {
            write(data);
        } else {
            buffer.put(pos, data);
        }
    }

    @Override
    public boolean isIdle() {
        boolean idle = buffer.isEmpty();
        for (BKOutputStream outStream : outStreams) {
            idle = idle || outStream.isIdle();
        }
        return idle;
    }

    @Override
    public void terminate() {
        for (BKOutputStream outStream : outStreams) {
            outStream.terminate();
        }
    }
}

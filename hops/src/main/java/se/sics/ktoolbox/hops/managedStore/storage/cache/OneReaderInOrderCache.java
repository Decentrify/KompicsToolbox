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

import com.google.common.collect.Range;
import java.nio.ByteBuffer;
import java.util.TreeMap;
import org.javatuples.Pair;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
class OneReaderInOrderCache implements KCache {
    private static final int MAX_BUFFER_SIZE = 5;

    private TreeMap<Long, Pair<Range<Long>, ByteBuffer>> buffer = new TreeMap<>();

    @Override
    public void write(Range<Long> bufferedRange, ByteBuffer buf) {
        if(buffer.size() >= MAX_BUFFER_SIZE) {
            buffer.remove(buffer.firstKey());
        }
        buffer.put(bufferedRange.lowerEndpoint(), Pair.with(bufferedRange, buf));
    }
    
    @Override
    public byte[] read(Range<Long> readRange) {
        for (Pair<Range<Long>, ByteBuffer> bufferedRange : buffer.values()) {
            if (bufferedRange.getValue0().isConnected(readRange)) {
                if (bufferedRange.getValue0().encloses(readRange)) {
                    return BlockReader.read(bufferedRange.getValue1(), bufferedRange.getValue0(), readRange);
                } else {
                    throw new RuntimeException("buffered ranges should always be a multiply of readRanges - logic error");
                }
            }
        }
        return null;
    }
}

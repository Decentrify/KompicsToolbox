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
package se.sics.ktoolbox.util.stream.storage;

import se.sics.kompics.ComponentProxy;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.reference.KReference;
import se.sics.ktoolbox.util.result.DelayedExceptionSyncHandler;
import se.sics.ktoolbox.util.stream.buffer.DelayedWrite;
import se.sics.ktoolbox.util.stream.cache.KHint;
import se.sics.ktoolbox.util.stream.cache.DelayedRead;
import se.sics.ktoolbox.util.stream.cache.KCache;
import se.sics.ktoolbox.util.stream.cache.SimpleKCache;
import se.sics.ktoolbox.util.stream.ranges.KBlock;
import se.sics.ktoolbox.util.stream.ranges.KRange;
import se.sics.ktoolbox.util.stream.util.FileDetails;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class AsyncCompleteStorage implements AsyncStorage {

    private final KCache cache;

    public AsyncCompleteStorage(KCache cache) {
        this.cache = cache;
    }
    public AsyncCompleteStorage(Config config, ComponentProxy proxy, DelayedExceptionSyncHandler exSyncHandler, FileDetails fileDetails) {
         this(new SimpleKCache(config, proxy, exSyncHandler, fileDetails.mainResource));
    }
    
    @Override
    public void start() {
        cache.start();
    }

    @Override
    public boolean isIdle() {
        return cache.isIdle();
    }

    @Override
    public void close() {
        cache.close();
    }
    
    //**************************************************************************
    @Override
    public void clean(Identifier reader) {
        cache.clean(reader);
    }

    @Override
    public void setFutureReads(Identifier reader, KHint.Expanded hint) {
        cache.setFutureReads(reader, hint);
    }

    //**************************************************************************
    @Override
    public void read(KRange readRange, DelayedRead delayedResult) {
        cache.read(readRange, delayedResult);
    }
    
    @Override
    public void write(KBlock writeRange, KReference<byte[]> val, DelayedWrite delayedResult) {
        throw new UnsupportedOperationException("Not supported"); 
    }
}

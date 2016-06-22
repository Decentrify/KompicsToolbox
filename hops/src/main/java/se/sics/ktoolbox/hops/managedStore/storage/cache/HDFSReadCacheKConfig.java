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

import se.sics.kompics.config.Config;
import se.sics.ktoolbox.util.config.KConfig;
import se.sics.ktoolbox.util.config.KConfigHelper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HDFSReadCacheKConfig {
    public static final String PREFIX = "hdfsReadCache";
    public static final String MAX_THREADS = PREFIX + ".maxThreads";
    public static final String READ_WINDOW_SIZE = PREFIX + ".readWindowSize";
    public static final String MAX_READERS = PREFIX + ".maxReaders";
    public static final String PIECE_SIZE = PREFIX + ".pieceSize";
    public static final String PIECES_PER_BLOCK = PREFIX + ".piecesPerBlock";
    
    public final int maxThreads;
    public final int readWindowSize;
    public final int maxReaders;
    public final int cacheSize;
    public final int defaultPieceSize;
    public final int defaultPiecesPerBlock;
    public final int defaultBlockSize;
    
    public HDFSReadCacheKConfig(Config config) {
        this.maxThreads = KConfig.readValue(config, MAX_THREADS, Integer.class);
        this.readWindowSize = KConfig.readValue(config, READ_WINDOW_SIZE, Integer.class);
        this.maxReaders = KConfig.readValue(config, MAX_READERS, Integer.class);
        this.cacheSize = readWindowSize * maxReaders;
        this.defaultPieceSize = KConfig.readValue(config, PIECE_SIZE, Integer.class);
        this.defaultPiecesPerBlock = KConfig.readValue(config, PIECES_PER_BLOCK, Integer.class);
        this.defaultBlockSize = defaultPieceSize * defaultPiecesPerBlock;
    }
}

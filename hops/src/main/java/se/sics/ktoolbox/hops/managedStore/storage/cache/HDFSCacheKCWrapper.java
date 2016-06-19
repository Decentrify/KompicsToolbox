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
import se.sics.ktoolbox.util.config.KConfigHelper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HDFSCacheKCWrapper {
    public final int maxThreads;
    public final int readWindowSize;
    public final int maxReaders;
    public final int cacheSize;
    public final int defaultPieceSize;
    public final int defaultPiecesPerBlock;
    public final int defaultBlockSize;
    
    public HDFSCacheKCWrapper(Config config) {
        this.maxThreads = KConfigHelper.read(config, HDFSCacheKConfig.maxThreads);
        this.readWindowSize = KConfigHelper.read(config, HDFSCacheKConfig.readWindowSize);
        this.maxReaders = KConfigHelper.read(config, HDFSCacheKConfig.maxReaders);
        this.cacheSize = readWindowSize * maxReaders;
        this.defaultPieceSize = KConfigHelper.read(config, HDFSCacheKConfig.pieceSize);
        this.defaultPiecesPerBlock = KConfigHelper.read(config, HDFSCacheKConfig.piecesPerBlock);
        this.defaultBlockSize = defaultPieceSize * defaultPiecesPerBlock;
    }
}

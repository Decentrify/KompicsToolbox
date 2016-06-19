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

import se.sics.ktoolbox.util.config.KConfigOption;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HDFSCacheKConfig {
    public final static KConfigOption.Basic<Integer> maxThreads = new KConfigOption.Basic("hdfsCache.maxThreads", Integer.class);
    public final static KConfigOption.Basic<Integer> readWindowSize = new KConfigOption.Basic("hdfsCache.readWindowSize", Integer.class);
    public final static KConfigOption.Basic<Integer> maxReaders = new KConfigOption.Basic("hdfsCache.maxReaders", Integer.class);
    public final static KConfigOption.Basic<Integer> pieceSize = new KConfigOption.Basic("hdfsCache.pieceSize", Integer.class);
    public final static KConfigOption.Basic<Integer> piecesPerBlock = new KConfigOption.Basic("hdfsCache.piecesPerBlock", Integer.class);
}

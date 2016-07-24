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
package se.sics.ktoolbox.util.stream.ranges;

import com.google.common.collect.Range;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public abstract class KRangeImpl implements KRange {
    protected final Range<Long> base;
    protected final int parentBlock;
    
    public KRangeImpl(int parentBlock, long lower, long higher) {
        this.parentBlock = parentBlock;
        this.base = Range.closed(lower, higher);
    }
    
    @Override
    public long lowerAbsEndpoint() {
        return base.lowerEndpoint();
    }

    @Override
    public long upperAbsEndpoint() {
        return base.upperEndpoint();
    }

    @Override
    public boolean encloses(KRange other) {
        return base.encloses(other.getBase());
    }
    
    @Override
    public boolean isConnected(KRange other) {
        return base.isConnected(other.getBase());
    }

    @Override
    public Range<Long> getBase() {
        return base;
    }
    
    @Override
    public int parentBlock() {
        return parentBlock;
    }
}

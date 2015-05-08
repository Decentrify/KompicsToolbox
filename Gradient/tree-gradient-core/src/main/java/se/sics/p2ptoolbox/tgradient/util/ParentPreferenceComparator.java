/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
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
package se.sics.p2ptoolbox.tgradient.util;

import java.util.Comparator;
import se.sics.p2ptoolbox.gradient.util.GradientContainer;
import se.sics.p2ptoolbox.util.Java6Util;

/**
 *
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ParentPreferenceComparator implements Comparator<GradientContainer> {

    private final GradientContainer base;
    private final int branching;
    private final int kCenterSize;
    private final int idealParentRank;

    /**
     * lower(first) entries are less preferred
     */
    public ParentPreferenceComparator(GradientContainer base, int branching, int kCenterSize) {
        this.base = base;
        this.branching = branching;
        this.kCenterSize = kCenterSize;
        this.idealParentRank = (base.rank - kCenterSize) / branching;
    }

    @Override
    public int compare(GradientContainer o1, GradientContainer o2) {
        if (o1.rank == o2.rank) {
            return 0;
        }
        if (o1.rank == idealParentRank) {
            return -1;
        }
        if (o2.rank == idealParentRank) {
            return 1;
        }
        if (o1.rank > idealParentRank && o2.rank > idealParentRank) {
            return Java6Util.compareInt(o1.rank, o2.rank); //prefer smaller one
        }
        if (o1.rank < idealParentRank && o2.rank < idealParentRank) {
            return -1 * Java6Util.compareInt(o1.rank, o2.rank); //prefer larger one 
        }

        //at this point i know o1 and o2 are on different sides of parent - and i prefer the larger one
        if(o1.rank > base.rank) {
            return 1;
        }
        if(o2.rank > base.rank) {
            return -1;
        }
        return -1 * Java6Util.compareInt(o1.rank, o2.rank);
    }
}

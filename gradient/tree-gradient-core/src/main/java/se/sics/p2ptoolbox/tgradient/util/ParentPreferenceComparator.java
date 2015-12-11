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

import com.google.common.primitives.Ints;
import java.util.Comparator;
import se.sics.ktoolbox.gradient.util.GradientContainer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ParentPreferenceComparator implements Comparator<GradientContainer> {

    private final GradientContainer base;
    private final int branching;
    private final int kCenterSize;
    private final int idealParentRank;
    private int lastParentReplicaRank;

    /**
     * lower(first) entries are less preferred
     */
    public ParentPreferenceComparator(GradientContainer base, int branching, int kCenterSize) {
        this.base = base;
        this.branching = branching;
        this.kCenterSize = kCenterSize;
        this.idealParentRank = (base.rank - kCenterSize) / branching;
        int myLevel = 0;
        if (base.rank > kCenterSize) {
            myLevel = (int) Math.floor(Math.log((double) base.rank / kCenterSize + 1) / Math.log(branching));
        }
        this.lastParentReplicaRank = -1; //indexes start from 0
        for (int i = 0; i < myLevel; i++) {
            this.lastParentReplicaRank += (int) (kCenterSize * Math.pow(branching, i));
        }
//        System.out.println(myLevel + " " + idealParentRank + " " + lastParentReplicaRank);
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
        
        //best range
        if (firstRange(o1) && firstRange(o2)) {
            return Ints.compare(o1.rank, o2.rank); //prefer smaller one
        }
        //second range
        if (secondRange(o1) && secondRange(o2)) {
            return -1 * Ints.compare(o1.rank, o2.rank); //prefer larger one 
        }
        
        if (thirdRange(o1) && thirdRange(o2)) {
            return Ints.compare(o1.rank, o2.rank); //prefer smaller one 
        }
        
        if(firstRange(o1)) {
            return -1;
        }
        if(firstRange(o2)) {
            return 1;
        }
        if(secondRange(o1)) {
            return -1;
        }
        if(secondRange(o2)) {
            return 1;
        }
        if(thirdRange(o1)) {
            return -1;
        }
        if(thirdRange(o2)) {
            return 1;
        }
        
        return Ints.compare(o1.rank, o2.rank);
    }
    
    private boolean firstRange(GradientContainer o) {
        return o.rank >= idealParentRank && o.rank <= lastParentReplicaRank;
    }
    
    private boolean secondRange(GradientContainer o) {
        return o.rank < idealParentRank;
    }
    
    private boolean thirdRange(GradientContainer o) {
        return o.rank > lastParentReplicaRank && o.rank < base.rank;
    }
}

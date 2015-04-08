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
package se.sics.p2ptoolbox.gradient.util;

import java.util.Comparator;

/**
 * do not use with TreeSets or TreeMaps - it is not equal equivalent
 * sorts ascending - from low preference to high preference
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class GradientPreferenceComparator<E extends Object> implements Comparator<E> {

    private final E base;
    private final Comparator<E> simpleComparator;

    /**
     * lower(first) entries are less preferred
     */
    public GradientPreferenceComparator(E base, Comparator<E> simpleComparator) {
        this.base = base;
        this.simpleComparator = simpleComparator;
    }

    @Override
    public int compare(E o1, E o2) {
        if (simpleComparator.compare(o1, o2) == 0) {
            return 0;
        }
        //prefer equal over others - equal are closest elements to base
        if (simpleComparator.compare(o1, base) == 0) {
            return 1;
        }
        if (simpleComparator.compare(o2, base) == 0) {
            return -1;
        }

        //both 
        if (simpleComparator.compare(o1, base) > 0 && simpleComparator.compare(o2, base) > 0) {
            return -1 * simpleComparator.compare(o1, o2); //prefer smaller one - the one closer to base
        }
        if (simpleComparator.compare(o1, base) < 0 && simpleComparator.compare(o2, base) < 0) {
            return simpleComparator.compare(o1, o2); //prefer bigger one - the one closer to base
        }
        //at this point i know o1 and o2 are on different sides of base - and i prefer the higher one
        return simpleComparator.compare(o1, o2); 
    }
}

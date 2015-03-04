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

package se.sics.p2ptoolbox.util;

import java.util.Comparator;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class WrapperComparator<E extends ComparableWrapper<F>, F extends Object> implements Comparator<E> {
    private final Comparator<F> innerComparator;
    
    public WrapperComparator(Comparator<F> innerComparator) {
        this.innerComparator = innerComparator;
    }
    
    /**
     * Abides contract of equal consistency as long as the ComparableWrapper abides the same equal consistency
     * @return -1, 0, 1
     */
    public int compare(E o1, E o2) {
        double compareToValue = Math.signum(innerComparator.compare(o1.unwrap(), o2.unwrap()));
        if(compareToValue == 0) {
            return o1.compareTo(o2);
        }
        return (int)compareToValue;
    }
}

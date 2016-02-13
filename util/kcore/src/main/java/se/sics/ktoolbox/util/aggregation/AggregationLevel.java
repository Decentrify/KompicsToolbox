 /*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Croupier is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.util.aggregation;

/**
 * BASIC - log memory usage of base data structures FULL - BASIC + event count
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public enum AggregationLevel {

    NONE, BASIC, FULL;

    public static AggregationLevel create(String aggLevel) {
        if (aggLevel.compareToIgnoreCase(AggregationLevel.NONE.name()) == 0) {
            return NONE;
        } else if (aggLevel.compareToIgnoreCase(AggregationLevel.BASIC.name()) == 0) {
            return BASIC;
        } else if (aggLevel.compareToIgnoreCase(AggregationLevel.FULL.name()) == 0) {
            return FULL;
        }

        return null;
    }
}

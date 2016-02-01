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
package se.sics.ktoolbox.croupier.util;

/**
 * BASIC - log memory usage of base data structures FULL - BASIC + event count
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public enum CroupierAggLevel {

    NONE, BASIC, CORE, FULL;

    public static CroupierAggLevel create(String aggLevel) {
        if (aggLevel.compareToIgnoreCase(CroupierAggLevel.NONE.name()) == 0) {
            return NONE;
        } else if (aggLevel.compareToIgnoreCase(CroupierAggLevel.BASIC.name()) == 0) {
            return BASIC;
        } else if (aggLevel.compareToIgnoreCase(CroupierAggLevel.CORE.name()) == 0) {
            return FULL;
        } else if (aggLevel.compareToIgnoreCase(CroupierAggLevel.FULL.name()) == 0) {
            return FULL;
        }

        return null;
    }
}

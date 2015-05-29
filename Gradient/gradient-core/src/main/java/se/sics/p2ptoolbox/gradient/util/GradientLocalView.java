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

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class GradientLocalView {
    public final Object appView;
    public final int rank;
    
    public GradientLocalView(Object appView, int rank) {
        this.appView = appView;
        this.rank = rank;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + (this.appView != null ? this.appView.hashCode() : 0);
        hash = 23 * hash + this.rank;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GradientLocalView other = (GradientLocalView) obj;
        if (this.appView != other.appView && (this.appView == null || !this.appView.equals(other.appView))) {
            return false;
        }
        if (this.rank != other.rank) {
            return false;
        }
        return true;
    }
}

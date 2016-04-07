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
package se.sics.ktoolbox.util.network.nat;

import com.google.common.base.Equivalence;
import java.util.Objects;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FullAddressEquivalence extends Equivalence<NatAwareAddressImpl> {

    @Override
    protected boolean doEquivalent(NatAwareAddressImpl o1, NatAwareAddressImpl o2) {
        if (o1 == null && o2 == null) {
            return true;
        }
        if(o1 == null || o2 == null) {
            return false;
        }
        if (!Objects.equals(o1.privateAdr, o2.privateAdr)) {
            return false;
        }
        if (!Objects.equals(o1.publicAdr, o2.publicAdr)) {
            return false;
        }
        if (!Objects.equals(o1.natType, o2.natType)) {
            return false;
        }
        if (!Objects.equals(o1.parents, o2.parents)) {
            return false;
        }
        return true;
    }

    @Override
    protected int doHash(NatAwareAddressImpl t) {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(t.privateAdr);
        hash = 17 * hash + Objects.hashCode(t.publicAdr);
        hash = 17 * hash + Objects.hashCode(t.natType);
        hash = 17 * hash + Objects.hashCode(t.parents);
        return hash;
    }
}

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
package se.sics.ktoolbox.util.address.nat;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import se.sics.kompics.network.Address;
import se.sics.ktoolbox.util.address.basic.BasicAddress;
import se.sics.ktoolbox.util.address.NatAwareAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CompleteNAAddress implements NatAwareAddress {

    final StrippedNAAddress baseAdr;
    final List<BasicAddress> parents;

    public CompleteNAAddress(StrippedNAAddress baseAdr, List<BasicAddress> parents) {
        this.baseAdr = baseAdr;
        this.parents = parents;
    }
    
    public CompleteNAAddress(BasicAddress baseAdr, NatType natType, List<BasicAddress> parents) {
        this(new StrippedNAAddress(baseAdr, natType), parents);
    }
    
    @Override
    public NatType getNatType() {
        return baseAdr.getNatType();
    }
    
    @Override
    public BasicAddress getBaseAdr() {
        return baseAdr.baseAdr;
    }
    
    public List<BasicAddress> getParents() {
        return parents;
    }

    @Override
    public Integer getId() {
        return baseAdr.getId();
    }

    @Override
    public InetAddress getIp() {
        return baseAdr.getIp();
    }

    @Override
    public int getPort() {
        return baseAdr.getPort();
    }

    @Override
    public InetSocketAddress asSocket() {
        return baseAdr.asSocket();
    }

    @Override
    public boolean sameHostAs(Address other) {
        return baseAdr.sameHostAs(other);
    }

    StrippedNAAddress strip() {
        return baseAdr;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 73 * hash + Objects.hashCode(this.baseAdr);
        hash = 73 * hash + Objects.hashCode(this.parents);
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
        final CompleteNAAddress other = (CompleteNAAddress) obj;
        if (!Objects.equals(this.baseAdr, other.baseAdr)) {
            return false;
        }
        if (!Objects.equals(this.parents, other.parents)) {
            return false;
        }
        return true;
    }
    
    public static CompleteNAAddress open(BasicAddress baseAdr) {
        return new CompleteNAAddress(baseAdr, NatType.open(), new ArrayList<BasicAddress>());
    }
}

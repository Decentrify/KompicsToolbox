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
import java.util.List;
import java.util.Objects;
import se.sics.kompics.network.Address;
import se.sics.ktoolbox.util.address.basic.BasicAddress;
import se.sics.ktoolbox.util.address.NatAwareAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 * do not create copies(constructor) of this and expect its sibling CompleteNAAddress to be
 * in the AddressResolution. The CompleteNAAddress is tied only to the original
 * StrippedNAAddress reference and will be deleted once there is no reference to
 * the original StrippedNAAddress
 */
public class StrippedNAAddress implements NatAwareAddress {

    final BasicAddress baseAdr;
    final NatType natType;

    public StrippedNAAddress(BasicAddress baseAdr, NatType natType) {
        this.baseAdr = baseAdr;
        this.natType = natType;
    }

    @Override
    public NatType getNatType() {
        return natType;
    }

    @Override
    public BasicAddress getBaseAdr() {
        return baseAdr;
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

    public CompleteNAAddress complete(List<BasicAddress> parents) {
        return new CompleteNAAddress(this, parents);
    }

    /**
     * do not allow copies of this, since we are following the references to see
     * when it is safe to forget the complete addresses
     *
     * @return
     */
    StrippedNAAddress shallowCopy() {
        return new StrippedNAAddress(baseAdr, natType);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.baseAdr);
        hash = 53 * hash + Objects.hashCode(this.natType);
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
        final StrippedNAAddress other = (StrippedNAAddress) obj;
        if (!Objects.equals(this.baseAdr, other.baseAdr)) {
            return false;
        }
        if (!Objects.equals(this.natType, other.natType)) {
            return false;
        }
        return true;
    }
    
    
}

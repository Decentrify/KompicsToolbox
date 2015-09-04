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
package se.sics.p2ptoolbox.util.network.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;
import se.sics.kompics.network.Address;
import se.sics.p2ptoolbox.util.identifiable.IntegerIdentifiable;
import se.sics.p2ptoolbox.util.traits.AcceptedTraits;
import se.sics.p2ptoolbox.util.traits.Trait;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public final class DecoratedAddress implements Address, IntegerIdentifiable {

    //This needs to be set at system boot time
    private static AcceptedTraits acceptedTraits = new AcceptedTraits();

    /**
     * @param setTraits - should be non null - can be empty. Has to be set at
     * the startup of the system before anyone tries to create
     * DecoratedAddresses return old accepted traits in case of double set -
     * which should be avoided anyway
     */
    public static synchronized AcceptedTraits setAcceptedTraits(AcceptedTraits setTraits) {
        AcceptedTraits aux = acceptedTraits;
        acceptedTraits = setTraits;
        return aux;
    }
    
    public static AcceptedTraits getAcceptedTraits() {
        return acceptedTraits;
    }
    
    private final BasicAddress base;
    private final Trait[] traits;
    
    public DecoratedAddress(BasicAddress base) {
        this(base, new Trait[acceptedTraits.size()]);
    }
    
    DecoratedAddress(BasicAddress base, Trait[] traits) {
        this.base = base;
        this.traits = traits;
    }
    
    public void addTrait(Trait trait) {
        traits[acceptedTraits.getIndex(trait.getClass())] = trait;
    }
    
    public <T extends Trait> boolean hasTrait(Class<T> traitClass) {
        if (acceptedTraits.acceptedTrait(traitClass)) {
            return traits[acceptedTraits.getIndex(traitClass)] != null;
        }
        return false;
    }
    
    public <T extends Trait> T getTrait(Class<T> traitClass) {
        return (T) traits[acceptedTraits.getIndex(traitClass)];
    }
    
    Trait[] getTraits() {
        return traits;
    }
    
    @Override
    public InetAddress getIp() {
        return base.getIp();
    }
    
    @Override
    public int getPort() {
        return base.getPort();
    }
    
    @Override
    public InetSocketAddress asSocket() {
        return base.asSocket();
    }
    
    @Override
    public boolean sameHostAs(Address other) {
        return base.sameHostAs(other);
    }
    
    @Override
    public Integer getId() {
        return base.getId();
    }
    
    public BasicAddress getBase() {
        return base;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(base.toString());
        for (Trait trait : traits) {
            if (trait != null) {
                sb.append(trait.toString());
            }
        }
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + Objects.hashCode(this.base);
        hash = 17 * hash + Arrays.deepHashCode(this.traits);
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
        final DecoratedAddress other = (DecoratedAddress) obj;
        if (!Objects.equals(this.base, other.base)) {
            return false;
        }
        if (!Arrays.deepEquals(this.traits, other.traits)) {
            return false;
        }
        return true;
    }
    
    public DecoratedAddress copy() {
        DecoratedAddress copy = new DecoratedAddress(base);
        for (Trait trait : traits) {
            if (trait != null) {
                copy.addTrait(trait);
            }
        }
        return copy;
    }
    
    public DecoratedAddress changePort(int newPort) {
        DecoratedAddress copy = new DecoratedAddress(new BasicAddress(base.getIp(), newPort, base.getId()));
        for (Trait trait : traits) {
            if (trait != null) {
                copy.addTrait(trait);
            }
        }
        return copy;
    }
    
    public DecoratedAddress changeBase(BasicAddress base) {
        DecoratedAddress copy = new DecoratedAddress(base);
        for (Trait trait : traits) {
            if (trait != null) {
                copy.addTrait(trait);
            }
        }
        return copy;
    }
}

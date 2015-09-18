/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * NatTraverser is free software; you can redistribute it and/or
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
package se.sics.p2ptoolbox.util.nat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import se.sics.p2ptoolbox.util.nat.Nat.AllocationPolicy;
import se.sics.p2ptoolbox.util.nat.Nat.FilteringPolicy;
import se.sics.p2ptoolbox.util.nat.Nat.MappingPolicy;
import se.sics.p2ptoolbox.util.nat.Nat.Type;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.traits.Trait;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NatedTrait implements Trait {

    public final Type type;
    public final MappingPolicy mappingPolicy;
    public final AllocationPolicy allocationPolicy;
    public final int delta;
    public final FilteringPolicy filteringPolicy;
    public final long bindingTimeout;
    public final List<DecoratedAddress> parents;

    public static boolean isOpen(DecoratedAddress adr) {
        return adr.hasTrait(NatedTrait.class) && 
                (adr.getTrait(NatedTrait.class).type.equals(Type.OPEN) || adr.getTrait(NatedTrait.class).type.equals(Type.UPNP));
    }
    
    public static boolean isUpnp(DecoratedAddress adr) {
        return adr.hasTrait(NatedTrait.class) && adr.getTrait(NatedTrait.class).type.equals(Nat.Type.UPNP);
    }

    public static NatedTrait open() {
        return new NatedTrait(Type.OPEN, null, null, 0, null, 0, new ArrayList<DecoratedAddress>());
    }

    public static NatedTrait firewall() {
        return new NatedTrait(Type.FIREWALL, null, null, 0, null, 0, new ArrayList<DecoratedAddress>());
    }

    public static NatedTrait udpBlocked() {
        return new NatedTrait(Type.UDP_BLOCKED, null, null, 0, null, 0, new ArrayList<DecoratedAddress>());
    }

    public static NatedTrait upnp() {
        return new NatedTrait(Type.UPNP, null, null, 0, null, 0, new ArrayList<DecoratedAddress>());
    }
    
    public static NatedTrait nated(MappingPolicy mappingPolicy, AllocationPolicy allocationPolicy, int delta,
            FilteringPolicy filteringPolicy, long bindingTimeout, ArrayList<DecoratedAddress> parents) {
        assert mappingPolicy != null;
        assert allocationPolicy != null;
        assert filteringPolicy != null;
        assert bindingTimeout > 0;
        return new NatedTrait(Type.NAT, mappingPolicy, allocationPolicy, delta, filteringPolicy, bindingTimeout, parents);
    }

    private NatedTrait(Type type, MappingPolicy mappingPolicy, AllocationPolicy allocationPolicy, int delta,
            FilteringPolicy filteringPolicy, long bindingTimeout, List<DecoratedAddress> parents) {
        this.type = type;
        this.mappingPolicy = mappingPolicy;
        this.allocationPolicy = allocationPolicy;
        this.delta = delta;
        this.filteringPolicy = filteringPolicy;
        this.bindingTimeout = bindingTimeout;
        this.parents = parents;
    }

    public NatedTrait changeParents(List<DecoratedAddress> parents) {
        return new NatedTrait(type, mappingPolicy, allocationPolicy, delta, filteringPolicy, bindingTimeout, parents);
    }

    public String natToString() {
        switch (type) {
            case OPEN:
                return type.code;
            case NAT:
                return type.code + "-" + mappingPolicy.code + "-" + allocationPolicy.code + "-" + filteringPolicy.code;
            case UDP_BLOCKED:
                return type.code;
            case UPNP:
                return type.code;
            case FIREWALL:
                return type.code;
            default:
                return "unknown";
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(natToString());
        if (!parents.isEmpty()) {
            sb.append("<");
            Iterator<DecoratedAddress> it = parents.iterator();
            sb.append(it.next().getBase().toString());
            while (it.hasNext()) {
                sb.append(", ");
                sb.append(it.next().getBase().toString());
            }
            sb.append(">");
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.type);
        hash = 97 * hash + Objects.hashCode(this.mappingPolicy);
        hash = 97 * hash + Objects.hashCode(this.allocationPolicy);
        hash = 97 * hash + this.delta;
        hash = 97 * hash + Objects.hashCode(this.filteringPolicy);
        hash = 97 * hash + (int) (this.bindingTimeout ^ (this.bindingTimeout >>> 32));
        hash = 97 * hash + Objects.hashCode(this.parents);
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
        final NatedTrait other = (NatedTrait) obj;
        if (this.type != other.type) {
            return false;
        }
        if (this.mappingPolicy != other.mappingPolicy) {
            return false;
        }
        if (this.allocationPolicy != other.allocationPolicy) {
            return false;
        }
        if (this.delta != other.delta) {
            return false;
        }
        if (this.filteringPolicy != other.filteringPolicy) {
            return false;
        }
        if (this.bindingTimeout != other.bindingTimeout) {
            return false;
        }
        if (!Objects.equals(this.parents, other.parents)) {
            return false;
        }
        return true;
    }
    
    
}

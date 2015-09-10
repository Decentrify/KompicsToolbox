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
package se.sics.p2ptoolbox.util.nat;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import org.javatuples.Pair;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.p2ptoolbox.util.BitBuffer;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NatedTraitSerializer implements Serializer {

    private final static int flags = 3 + 2 + 2 + 2;

    private final int id;

    public NatedTraitSerializer(int id) {
        this.id = id;
    }

    @Override
    public int identifier() {
        return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        NatedTrait nat = (NatedTrait) o;
        BitBuffer bb = BitBuffer.create(flags);
        switch (nat.type) {
            case OPEN:
                buf.writeBytes(bb.finalise());
                return;
            case NAT:
                bb.write(Pair.with(0, true));
                break;
            case UPNP:
                bb.write(Pair.with(1, true));
                buf.writeBytes(bb.finalise());
                return;
            case FIREWALL:
                bb.write(Pair.with(0, true), Pair.with(1, true));
                buf.writeBytes(bb.finalise());
                return;
            case UDP_BLOCKED:
                bb.write(Pair.with(2, true));
                buf.writeBytes(bb.finalise());
                return;
            default:
                throw new IllegalArgumentException("unknown type:" + nat.type);
        }
        switch (nat.allocationPolicy) {
            case PORT_PRESERVATION:
                break;
            case PORT_CONTIGUITY:
                bb.write(Pair.with(3, true));
                break;
            case RANDOM:
                bb.write(Pair.with(4, true));
                break;
            default:
                throw new IllegalArgumentException("unknown allocation policy:" + nat.allocationPolicy);
        }
        switch (nat.mappingPolicy) {
            case ENDPOINT_INDEPENDENT:
                break;
            case HOST_DEPENDENT:
                bb.write(Pair.with(5, true));
                break;
            case PORT_DEPENDENT:
                bb.write(Pair.with(6, true));
                break;
            default:
                throw new IllegalArgumentException("unknown mapping policy:" + nat.mappingPolicy);
        }
        switch (nat.filteringPolicy) {
            case ENDPOINT_INDEPENDENT:
                break;
            case HOST_DEPENDENT:
                bb.write(Pair.with(7, true));
                break;
            case PORT_DEPENDENT:
                bb.write(Pair.with(8, true));
                break;
            default:
                throw new IllegalArgumentException("unknown mapping policy:" + nat.mappingPolicy);
        }

        buf.writeBytes(bb.finalise());
        buf.writeInt(nat.delta);
        buf.writeLong(nat.bindingTimeout);
        buf.writeByte(nat.parents.size());
        Serializer bAdrSerializer = Serializers.lookupSerializer(BasicAddress.class);
        for (DecoratedAddress parent : nat.parents) {
            bAdrSerializer.toBinary(parent.getBase(), buf);
        }
    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        byte[] bFlags = new byte[2];

        buf.readBytes(bFlags);
        boolean[] traitFlags = BitBuffer.extract(flags, bFlags);

        Nat.Type type = getNatType(traitFlags);
        switch (type) {
            case OPEN:
                return NatedTrait.open();
            case FIREWALL:
                return NatedTrait.firewall();
            case UPNP:
                return NatedTrait.upnp();
            case UDP_BLOCKED:
                return NatedTrait.udpBlocked();
            case NAT:
                break;
        }
        Nat.AllocationPolicy allocationPolicy = getAllocationPolicy(traitFlags);
        Nat.MappingPolicy mappingPolicy = getMappingPolicy(traitFlags);
        Nat.FilteringPolicy filteringPolicy = getFilterPolicy(traitFlags);
        int delta = buf.readInt();
        long bindingTimeout = buf.readLong();
        int nrParents = buf.readByte();
        Serializer bAdrSerializer = Serializers.lookupSerializer(BasicAddress.class);
        ArrayList<DecoratedAddress> parents = new ArrayList<>();
        for (int i = 0; i < nrParents; i++) {
            BasicAddress bParentAdr = (BasicAddress) bAdrSerializer.fromBinary(buf, hint);
            DecoratedAddress parentAdr = new DecoratedAddress(bParentAdr);
            parentAdr.addTrait(NatedTrait.open());
            parents.add(parentAdr);
        }
        return NatedTrait.nated(mappingPolicy, allocationPolicy, delta, filteringPolicy, bindingTimeout, parents);
    }

    private Nat.Type getNatType(boolean[] traitFlags) {
        if (traitFlags[0]) {
            if (traitFlags[1]) {
                return Nat.Type.FIREWALL;
            } else {
                return Nat.Type.NAT;
            }
        }
        if (traitFlags[1]) {
            return Nat.Type.UPNP;
        }
        if (traitFlags[2]) {
            return Nat.Type.UDP_BLOCKED;
        }
        return Nat.Type.OPEN;
    }

    private Nat.AllocationPolicy getAllocationPolicy(boolean[] traitFlags) {
        if (traitFlags[3]) {
            return Nat.AllocationPolicy.PORT_CONTIGUITY;
        }
        if (traitFlags[4]) {
            return Nat.AllocationPolicy.RANDOM;
        }
        return Nat.AllocationPolicy.PORT_PRESERVATION;
    }

    private Nat.MappingPolicy getMappingPolicy(boolean[] traitFlags) {
        if (traitFlags[5]) {
            return Nat.MappingPolicy.HOST_DEPENDENT;
        }
        if (traitFlags[6]) {
            return Nat.MappingPolicy.PORT_DEPENDENT;
        }
        return Nat.MappingPolicy.ENDPOINT_INDEPENDENT;
    }

    private Nat.FilteringPolicy getFilterPolicy(boolean[] traitFlags) {
        if (traitFlags[7]) {
            return Nat.FilteringPolicy.HOST_DEPENDENT;
        }
        if (traitFlags[8]) {
            return Nat.FilteringPolicy.PORT_DEPENDENT;
        }
        return Nat.FilteringPolicy.ENDPOINT_INDEPENDENT;
    }
}

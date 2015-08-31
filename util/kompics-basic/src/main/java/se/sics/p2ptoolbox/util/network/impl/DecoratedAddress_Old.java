///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * GVoD is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//
//package se.sics.p2ptoolbox.util.network.impl;
//
//import java.net.InetAddress;
//import java.net.InetSocketAddress;
//import java.util.Set;
//import se.sics.kompics.network.Address;
//import se.sics.p2ptoolbox.util.identifiable.IntegerIdentifiable;
//import se.sics.p2ptoolbox.util.traits.Nated;
//import se.sics.p2ptoolbox.util.traits.Trait;
//
///**
// * @author Alex Ormenisan <aaor@sics.se>
// */
//public class DecoratedAddress_Old implements Address, IntegerIdentifiable {
//    private final BasicAddress base;
//    
//    //traits
//    private final Set<DecoratedAddress> parents;
//
//    public DecoratedAddress_Old(BasicAddress base, Set<DecoratedAddress> parents) {
//        this.base = base;
//        this.parents = parents;
//        if(parents != null && parents.size() > 127) {
//            throw new RuntimeException("you should have less than 128 parents");
//        }
//    }
//    
//    public DecoratedAddress_Old(InetAddress addr, int port, int id) {
//        this(new BasicAddress(addr, port, id), null);
//    }
//    
//    public DecoratedAddress_Old(BasicAddress base) {
//        this(base, null);
//    }
//    
//    public static DecoratedAddress_Old addNatedTrait(Address adr, Set<DecoratedAddress> parents) {
//        if (adr instanceof BasicAddress) {
//            return new DecoratedAddress_Old((BasicAddress) adr, parents);
//        }
//        DecoratedAddress_Old dAdr = (DecoratedAddress_Old) adr;
//        return new DecoratedAddress_Old(dAdr.base, parents);
//    }
//    
//    @Override
//    public InetAddress getIp() {
//        return base.getIp();
//    }
//
//    @Override
//    public int getPort() {
//        return base.getPort();
//    }
//
//    @Override
//    public InetSocketAddress asSocket() {
//        return base.asSocket();
//    }
//
//    @Override
//    public boolean sameHostAs(Address other) {
//        return base.sameHostAs(other);
//    }
//
//    @Override
//    public Integer getId() {
//        return base.getId();
//    }
//
//    public BasicAddress getBase() {
//        return base;
//    }
//    
//    @Override
//    public int hashCode() {
//        int hash = 3;
//        hash = 47 * hash + (this.base != null ? this.base.hashCode() : 0);
//        hash = 47 * hash + (this.parents != null ? this.parents.hashCode() : 0);
//        return hash;
//    }
//    @Override
//    public boolean equals(Object obj) {
//        if (obj == null) {
//            return false;
//        }
//        if (getClass() != obj.getClass()) {
//            return false;
//        }
//        final DecoratedAddress_Old other = (DecoratedAddress_Old) obj;
//        if (this.base != other.base && (this.base == null || !this.base.equals(other.base))) {
//            return false;
//        }
//        if (this.parents != other.parents && (this.parents == null || !this.parents.equals(other.parents))) {
//            return false;
//        }
//        return true;
//    }
//    
//    @Override
//    public String toString() {
//        return base.toString();
//    }
//    
//    //********************DecoratedAddress***************************************
//    public <E extends Trait> boolean hasTrait(Class<E> traitClass) {
//        if (traitClass.equals(Nated.class)) {
//            return parents != null;
//        }
//        throw new RuntimeException("unknown address trait" + traitClass);
//    }
//    public <E extends Trait> E getTrait(Class<E> traitClass) {
//        if (traitClass.equals(Nated.class)) {
//            return (E) new Nated<DecoratedAddress>() {
//                @Override
//                public Set<DecoratedAddress> getParents() {
//                    return parents;
//                }
//            };
//        }
//        throw new RuntimeException("unknown header trait" + traitClass);
//    }
//    
//    //**********************Packaged - used for Serialization*******************
//    Set<DecoratedAddress> getParents() {
//        return parents;
//    }  
//}

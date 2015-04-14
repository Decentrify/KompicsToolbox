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

import java.util.List;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Header;
import se.sics.kompics.network.Transport;
import se.sics.p2ptoolbox.util.traits.Forwardable;
import se.sics.p2ptoolbox.util.traits.OverlayMember;
import se.sics.p2ptoolbox.util.traits.Trait;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class DecoratedHeader<Adr extends Address> implements Header<Adr> {

    private final BasicHeader<Adr> base;

    //traits
    private Route<Adr> route = null; //Forwardable
    private Integer overlayId = null; //OverlayMember

    public DecoratedHeader(BasicHeader<Adr> base, Route<Adr> route, Integer overlayId) {
        this.base = base;
        this.route = route;
        this.overlayId = overlayId;
    }
    
    public DecoratedHeader(Adr src, Adr dst, Transport protocol) {
        this(new BasicHeader(src, dst, protocol), null, null);
    }

    public static <A extends Address> DecoratedHeader<A> addOverlayMemberTrait(Header<A> header, int overlayId) {
        if (header instanceof BasicHeader) {
            return new DecoratedHeader((BasicHeader<A>) header, null, overlayId);
        }
        DecoratedHeader<A> dHeader = (DecoratedHeader<A>) header;
        return new DecoratedHeader(dHeader.base, dHeader.route, overlayId);
    }

    public static <A extends Address> DecoratedHeader<A> addForwardableTrait(Header<A> header, Route<A> route) {
        if (header instanceof BasicHeader) {
            return new DecoratedHeader((BasicHeader<A>) header, route, null);
        }
        DecoratedHeader<A> dHeader = (DecoratedHeader<A>) header;
        return new DecoratedHeader(dHeader.base, route, dHeader.overlayId);
    }

    @Override
    public Adr getSource() {
        if (route != null) {
            return route.getSource();
        }
        return base.getSource();
    }

    @Override
    public Adr getDestination() {
        if (route != null && route.hasNext()) {
            return route.getDestination();
        }
        return base.getDestination();
    }

    @Override
    public Transport getProtocol() {
        return base.getProtocol();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + (this.base != null ? this.base.hashCode() : 0);
        hash = 37 * hash + (this.route != null ? this.route.hashCode() : 0);
        hash = 37 * hash + (this.overlayId != null ? this.overlayId.hashCode() : 0);
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
        final DecoratedHeader<?> other = (DecoratedHeader<?>) obj;
        if (this.base != other.base && (this.base == null || !this.base.equals(other.base))) {
            return false;
        }
        if (this.route != other.route && (this.route == null || !this.route.equals(other.route))) {
            return false;
        }
        if (this.overlayId != other.overlayId && (this.overlayId == null || !this.overlayId.equals(other.overlayId))) {
            return false;
        }
        return true;
    }
    
    //********************DecoratedHeader***************************************
    public <E extends Trait> boolean hasTrait(Class<E> traitClass) {
        if (traitClass.equals(Forwardable.class)) {
            return route != null;
        } else if (traitClass.equals(OverlayMember.class)) {
            return overlayId != null;
        }
        throw new RuntimeException("unknown header trait" + traitClass);
    }

    public <E extends Trait> E getTrait(Class<E> traitClass) {
        if (traitClass.equals(Forwardable.class)) {
            return (E) new Forwardable<Adr>() {
                @Override
                public DecoratedHeader next() {
                    return new DecoratedHeader(base, route.next(), overlayId);
                }

                @Override
                public DecoratedHeader prependRoute(List<Adr> prependRoute) {
                    return new DecoratedHeader(base, route.prepend(prependRoute), overlayId);
                }
            };
        } else if (traitClass.equals(OverlayMember.class)) {
            return (E) new OverlayMember() {
                @Override
                public int getOverlayId() {
                    return overlayId;
                }
            };
        }
        throw new RuntimeException("unknown header trait" + traitClass);
    }
    
    //**********************Packaged - used for Serialization*******************
    BasicHeader getBase() {
        return base;
    }
    Route<Adr> getRoute() {
        return route;
    }
    int getOverlayId() {
        return overlayId;
    }
}

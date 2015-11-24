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
package se.sics.ktoolbox.util.msg;

import java.util.ArrayList;
import java.util.List;
import se.sics.kompics.network.Address;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
//routes are never empty
//routes are homogeneus
public class Route<Adr extends Address> {

    private final List<Adr> route;

    public Route(List<Adr> route) {
        this.route = route;
        if (route == null || route.isEmpty()) {
            throw new RuntimeException("null routes or empty routes are bad - should be no routes at all");
        }
        if (route != null && route.size() > 127) {
            throw new RuntimeException("routes should be less than 128 hops");
        }
    }

    public Adr getSource() {
        if (route.size() > 0) {
            return route.get(0);
        }
        return null;
    }

    public Adr getDestination() {
        if (route.size() > 1) {
            return route.get(1);
        }
        return null;
    }

    public boolean hasNext() {
        return route.size() > 1;
    }

    public Route next() {
        if (route.size() > 1) {
            return new Route(new ArrayList<Adr>(route.subList(1, route.size())));
        }
        return null;
    }

    public int size() {
        return route.size();
    }

    public Route prepend(List<Adr> newPrefix) {
        List<Adr> newRoute = new ArrayList<Adr>();
        newRoute.add(route.get(0));
        newRoute.addAll(newPrefix);
        newRoute.addAll(route.subList(1, route.size()));
        return new Route(newRoute);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.route != null ? this.route.hashCode() : 0);
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
        final Route<?> other = (Route<?>) obj;
        if (this.route != other.route && (this.route == null || !this.route.equals(other.route))) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return route.toString();
    }        

    //***********************Packaged for Serializers***************************
    List<Adr> getRoute() {
        return route;
    }
}

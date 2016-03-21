/*
 * This file is part of the Kompics Simulator.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.util.identifiable.basic;

import java.util.Objects;
import java.util.UUID;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class UUIDIdentifier implements Identifier {

    public final UUID id;

    public UUIDIdentifier(UUID id) {
        this.id = id;
    }

    public UUIDIdentifier() {
        this(UUID.randomUUID());
    }

    @Override
    public int partition(int nrPartitions) {
        return (int)(id.getLeastSignificantBits() % Integer.MAX_VALUE) % nrPartitions;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.id);
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
        final UUIDIdentifier other = (UUIDIdentifier) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return id.toString();
    }
    
    public static UUIDIdentifier randomId() {
        return new UUIDIdentifier(UUID.randomUUID());
    }

    @Override
    public int compareTo(Identifier o) {
        UUIDIdentifier that = (UUIDIdentifier)o;
        return this.id.compareTo(that.id);
    }
}

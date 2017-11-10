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
package se.sics.ktoolbox.util.identifiable.basic;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.SignedBytes;
import java.util.Arrays;
import se.sics.kompics.util.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public abstract class ByteIdentifier implements Identifier {
    public final byte[] id;

    protected ByteIdentifier(byte[] id) {
        this.id = id;
    }

    @Override
    public int partition(int nrPartitions) {
        return hashCode() % nrPartitions;
    }

    @Override
    public int compareTo(Identifier o) {
        ByteIdentifier that = (ByteIdentifier)o;
        return SignedBytes.lexicographicalComparator().compare(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ByteIdentifier other = (ByteIdentifier) obj;
        if (!Arrays.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return BaseEncoding.base16().encode(id);
    }
}

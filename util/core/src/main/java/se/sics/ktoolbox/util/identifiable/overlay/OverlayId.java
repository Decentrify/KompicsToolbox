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
package se.sics.ktoolbox.util.identifiable.overlay;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Ints;
import java.util.Comparator;
import java.util.Objects;
import se.sics.ktoolbox.util.Either;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OverlayId implements Identifier {

    public final Identifier baseId;
    public final Type type;
    public final byte owner;
    private final String sOwner;
    private final TypeComparator typeComparator;

    OverlayId(Identifier baseId, Type type, byte owner, TypeComparator typeComparator) {
        this.baseId = baseId;
        this.type = type;
        this.owner = owner;
        this.sOwner = BaseEncoding.base16().encode(new byte[]{owner});
        this.typeComparator = typeComparator;
    }

    public OverlayId changeType(Type type) {
        return new OverlayId(baseId, type, owner, typeComparator);
    }

    @Override
    public String toString() {
        return type.toString() + ":" + sOwner + ":" + baseId.toString();
    }

    @Override
    public int partition(int nrPartitions) {
        return baseId.partition(nrPartitions);
    }

    @Override
    public int compareTo(Identifier o) {
        OverlayId that = (OverlayId) o;
        int result = typeComparator.compare(this.type, that.type);
        if (result != 0) {
            return result;
        }
        if (this.owner != that.owner) {
            result = this.owner < that.owner ? -1 : 1;
            return result;
        }
        result = this.baseId.compareTo(that.baseId);
        return result;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.baseId);
        hash = 79 * hash + Objects.hashCode(this.type);
        hash = 79 * hash + this.owner;
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
        final OverlayId other = (OverlayId) obj;
        if (!Objects.equals(this.baseId, other.baseId)) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        if (this.owner != other.owner) {
            return false;
        }
        return true;
    }

    public static interface Type {
    }

    public interface TypeFactory {

        public Type fromByte(byte byteType) throws UnknownTypeException;

        public byte toByte(Type type) throws UnknownTypeException;

        public byte lastUsed();
    }

    public static interface TypeComparator<T extends Type> extends Comparator<T> {
    }

    public static enum BasicTypes implements Type {

        CROUPIER, GRADIENT, TGRADIENT, OTHER;
    }

    public static class BasicTypeFactory implements TypeFactory {

        private final byte startWith;
        private final byte croupier;
        private final byte gradient;
        private final byte tgradient;
        private final byte other;

        public BasicTypeFactory(byte startWith) {
            assert startWith < 252;
            this.startWith = startWith;
            this.croupier = (byte) (startWith + 1);
            this.gradient = (byte) (startWith + 2);
            this.tgradient = (byte) (startWith + 3);
            this.other = (byte) (startWith + 4);
        }

        @Override
        public OverlayId.Type fromByte(byte overlayType) throws UnknownTypeException {
            if (overlayType == croupier) {
                return BasicTypes.CROUPIER;
            } else if (overlayType == gradient) {
                return BasicTypes.GRADIENT;
            } else if (overlayType == tgradient) {
                return BasicTypes.TGRADIENT;
            } else if (overlayType == other) {
                return BasicTypes.OTHER;
            } else {
                throw new RuntimeException("unknown type:" + overlayType);
            }
        }

        @Override
        public byte toByte(Type type) throws UnknownTypeException {
            if (type instanceof BasicTypes) {
                BasicTypes basicType = (BasicTypes) type;
                switch (basicType) {
                    case CROUPIER:
                        return croupier;
                    case GRADIENT:
                        return gradient;
                    case TGRADIENT:
                        return tgradient;
                    case OTHER:
                        return other;
                    default:
                        throw new UnknownTypeException(type);
                }
            } else {
                throw new UnknownTypeException(type);
            }
        }

        @Override
        public byte lastUsed() {
            return other;
        }
    }

    public static class UnknownTypeException extends Exception {

        public final Either<Type, Byte> cause;

        public UnknownTypeException(byte bType) {
            super();
            this.cause = Either.right(bType);
        }

        public UnknownTypeException(Type type) {
            super();
            this.cause = Either.left(type);
        }
    }

    public static class BasicTypeComparator implements TypeComparator<BasicTypes> {

        @Override
        public int compare(BasicTypes o1, BasicTypes o2) {
            return Ints.compare(o1.ordinal(), o2.ordinal());
        }
    }
}

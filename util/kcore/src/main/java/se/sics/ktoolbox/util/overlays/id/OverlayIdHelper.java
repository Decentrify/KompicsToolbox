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
package se.sics.ktoolbox.util.overlays.id;

import com.google.common.primitives.Ints;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.IntIdentifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OverlayIdHelper {
    //1st byte - owner(4bits), layer type(4bits)
    //rest - unique ids

    public static enum Type {

        CROUPIER((byte) 1),
        GRADIENT((byte) 2),
        TGRADIENT((byte) 3);

        byte code;

        Type(byte code) {
            this.code = code;
        }

        public static Type getFromCode(byte code) {
            switch (code) {
                case 1:
                    return CROUPIER;
                case 2:
                    return GRADIENT;
                case 3:
                    return TGRADIENT;
                default:
                    throw new RuntimeException("unknown type");
            }
        }
    }

    public static IntIdentifier changeOverlayType(IntIdentifier overlayId, Type type) {
        byte[] byteId = Ints.toByteArray(overlayId.id);
        byte owner = (byte) (byteId[0] & 0xF0); //first 4 bits
        byteId[0] = (byte) (owner + type.code);
        return new IntIdentifier(Ints.fromByteArray(byteId));
    }

    public static IntIdentifier getIntIdentifier(byte owner, Type type, byte[] id) {
        assert owner == (byte) (owner & 0xF0);
        assert id.length == 3;
        byte firstByte = (byte) (owner + type.code);
        return new IntIdentifier(Ints.fromBytes(firstByte, id[0], id[1], id[2]));
    }
    
    public static byte getOwner(IntIdentifier id) {
        byte firstByte = Ints.toByteArray(id.id)[0];
        byte owner = (byte)(firstByte & 0xF0);
        return owner;
    }

    public static Type getType(IntIdentifier id) {
        byte firstByte = Ints.toByteArray(id.id)[0];
        byte bType = (byte)(firstByte & 0x0F);
        Type type = Type.getFromCode(bType);
        return type;
    }
}

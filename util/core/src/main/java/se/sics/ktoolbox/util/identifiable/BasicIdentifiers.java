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
package se.sics.ktoolbox.util.identifiable;

import java.util.Random;
import se.sics.ktoolbox.util.identifiable.basic.IntIdFactory;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdFactory;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BasicIdentifiers {

    public static enum Values {

        EVENT, MSG, OVERLAY, NODE;
    }

    public static Identifier eventId() {
        return IdentifierRegistry.lookup(Values.EVENT.toString()).randomId();
    }

    public static Identifier eventId(IdentifierBuilder builder) {
        return IdentifierRegistry.lookup(Values.EVENT.toString()).id(builder);
    }

    public static Class<? extends Identifier> eventIdType() {
        return IdentifierRegistry.lookup(Values.EVENT.toString()).idType();
    }

    public static Identifier msgId() {
        return IdentifierRegistry.lookup(Values.MSG.toString()).randomId();
    }

    public static Identifier msgId(IdentifierBuilder builder) {
        return IdentifierRegistry.lookup(Values.MSG.toString()).id(builder);
    }

    public static Class<? extends Identifier> msgIdType() {
        return IdentifierRegistry.lookup(Values.MSG.toString()).idType();
    }

    public static Identifier overlayId() {
        return IdentifierRegistry.lookup(Values.OVERLAY.toString()).randomId();
    }

    public static Identifier overlayId(IdentifierBuilder builder) {
        return IdentifierRegistry.lookup(Values.OVERLAY.toString()).id(builder);
    }

    public static Class<? extends Identifier> overlayIdType() {
        return IdentifierRegistry.lookup(Values.OVERLAY.toString()).idType();
    }

    public static Identifier nodeId() {
        return IdentifierRegistry.lookup(Values.NODE.toString()).randomId();
    }

    public static Identifier nodeId(IdentifierBuilder builder) {
        return IdentifierRegistry.lookup(Values.NODE.toString()).id(builder);
    }

    public static Class<? extends Identifier> nodeIdType() {
        return IdentifierRegistry.lookup(Values.NODE.toString()).idType();
    }

    public static void registerDefaults(long seed) {
        Random rand = new Random(seed);
        IdentifierRegistry.register(Values.EVENT.toString(), new UUIDIdFactory());
        IdentifierRegistry.register(Values.MSG.toString(), new UUIDIdFactory());
        IdentifierRegistry.register(Values.OVERLAY.toString(), new UUIDIdFactory());
        IdentifierRegistry.register(Values.NODE.toString(), new IntIdFactory(rand));
    }

    public static boolean checkBasicIdentifiers() {
        if (IdentifierRegistry.lookup(Values.EVENT.toString()) == null) {
            return false;
        }
        if (IdentifierRegistry.lookup(Values.MSG.toString()) == null) {
            return false;
        }
        if (IdentifierRegistry.lookup(Values.OVERLAY.toString()) == null) {
            return false;
        }
        if (IdentifierRegistry.lookup(Values.NODE.toString()) == null) {
            return false;
        }
        return true;
    }
}

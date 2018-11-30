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

import java.util.Optional;
import se.sics.kompics.util.Identifier;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BasicIdentifiers {

  public static Optional<Long> backupSeed = Optional.of(1234l);
  public static enum Values {

    EVENT,
    MSG,
    OVERLAY,
    NODE,
    CONN_INSTANCE;
  }

//  public static Identifier eventId() {
//    return IdentifierRegistryV2.instance(Values.EVENT, backupSeed).randomId();
//  }
//
//  public static Identifier eventId(IdentifierBuilder builder) {
//    return IdentifierRegistryV2.instance(Values.EVENT, backupSeed).id(builder);
//  }

  public static Class<? extends Identifier> eventIdType() {
    return IdentifierRegistryV2.idType(Values.EVENT);
  }

//  public static Identifier msgId() {
//    return IdentifierRegistryV2.instance(Values.MSG, backupSeed).randomId();
//  }
//
//  public static Identifier msgId(IdentifierBuilder builder) {
//    return IdentifierRegistryV2.instance(Values.MSG, backupSeed).id(builder);
//  }

  public static Class<? extends Identifier> msgIdType() {
    return IdentifierRegistryV2.idType(Values.MSG);
  }

  public static Identifier overlayId() {
    return IdentifierRegistryV2.instance(Values.OVERLAY, backupSeed).randomId();
  }

  public static Identifier overlayId(IdentifierBuilder builder) {
    return IdentifierRegistryV2.instance(Values.OVERLAY, backupSeed).id(builder);
  }

  public static Class<? extends Identifier> overlayIdType() {
    return IdentifierRegistryV2.idType(Values.OVERLAY);
  }

  public static Identifier nodeId() {
    return IdentifierRegistryV2.instance(Values.NODE, backupSeed).randomId();
  }

  public static Identifier nodeId(IdentifierBuilder builder) {
    return IdentifierRegistryV2.instance(Values.NODE, backupSeed).id(builder);
  }

  public static Class<? extends Identifier> nodeIdType() {
    return IdentifierRegistryV2.idType(Values.NODE);
  }
}

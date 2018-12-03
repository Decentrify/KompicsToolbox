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
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import se.sics.ktoolbox.util.SettableMemo;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OverlayRegistryV2 {

  private static final SettableMemo<Map<String, ByteBuffer>> reservedOverlayPrefixes 
    = new SettableMemo<>("overlay_reserved_prefixes");
  private static final SettableMemo<OverlayId.TypeComparator> typeComparator 
    = new SettableMemo<>("overlay_type_comparator");
  private static final SettableMemo<OverlayId.TypeFactory> typeFactory 
    = new SettableMemo<>("overlay_type_factory");;

  public static synchronized void initiate(OverlayId.TypeFactory tf, OverlayId.TypeComparator tc) {
    typeComparator.set(tc);
    typeFactory.set(tf);
    reservedOverlayPrefixes.set(new HashMap<>());
  }

  public static synchronized boolean isRegistered(OverlayId id) {
    return reservedOverlayPrefixes.get().values().contains(ByteBuffer.wrap(new byte[]{id.owner}));
  }

  public static synchronized boolean registerPrefix(String owner, byte prefix) {
    if (reservedOverlayPrefixes.get().containsKey(owner)) {
      throw new RuntimeException("owner name clash");
    }
    if (reservedOverlayPrefixes.get().values().contains(ByteBuffer.wrap(new byte[]{prefix}))) {
      throw new RuntimeException("prefix clash");
    }
    reservedOverlayPrefixes.get().put(owner, ByteBuffer.wrap(new byte[]{prefix}));
    return true;
  }

  public static byte getPrefix(String owner) {
    ByteBuffer prefix = reservedOverlayPrefixes.get().get(owner);
    if (prefix == null) {
      throw new RuntimeException("ups");
    }
    return prefix.array()[0];
  }

  public static synchronized OverlayId.TypeComparator getTypeComparator() {
    return typeComparator.get();
  }

  public static synchronized OverlayId.TypeFactory getTypeFactory() {
    return typeFactory.get();
  }

  public static synchronized String print() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, ByteBuffer> e : reservedOverlayPrefixes.get().entrySet()) {
      String ownerId = BaseEncoding.base16().encode(e.getValue().array()).substring(0, 1);
      sb.append(e.getKey()).append(":").append(ownerId).append("\n");
    }
    return sb.toString();
  }
}

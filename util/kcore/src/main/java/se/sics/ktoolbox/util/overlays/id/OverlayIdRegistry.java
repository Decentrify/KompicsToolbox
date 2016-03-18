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

import se.sics.ktoolbox.util.identifiable.basic.OverlayIdFactory;
import com.google.common.io.BaseEncoding;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.IntIdentifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OverlayIdRegistry {
    private static final Map<String, ByteBuffer> reservedOverlayPrefixes = new HashMap<>();

    public static synchronized boolean registerPrefix(String owner, byte prefix) {
        if(prefix != (prefix >> 4 << 4)) {
            // make sure last 4 bits are 0
            //currently allow only 4 bits to be used as prefix
            //we don't do variable length and care about one prefix being a possible prefix of another
            throw new RuntimeException("bad prefix - accepted prefix needs to have last 4 bits 0");
        }
        if(reservedOverlayPrefixes.containsKey(owner)) {
            throw new RuntimeException("owner name clash");
        }
        if (reservedOverlayPrefixes.values().contains(ByteBuffer.wrap(new byte[]{prefix}))) {
            throw new RuntimeException("prefix clash");
        }
        reservedOverlayPrefixes.put(owner, ByteBuffer.wrap(new byte[]{prefix}));
        return true;
    }
    
    public static synchronized boolean isRegistered(Identifier id) {
        byte owner = OverlayIdFactory.getOwner(id);
        return reservedOverlayPrefixes.values().contains(ByteBuffer.wrap(new byte[]{owner}));
    }
    
    public static synchronized String print() {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, ByteBuffer> e : reservedOverlayPrefixes.entrySet()) {
            String ownerId = BaseEncoding.base16().encode(e.getValue().array()).substring(0,1);
            sb.append(e.getKey()).append(":").append(ownerId).append("\n");
        }
        return sb.toString();
    }
}

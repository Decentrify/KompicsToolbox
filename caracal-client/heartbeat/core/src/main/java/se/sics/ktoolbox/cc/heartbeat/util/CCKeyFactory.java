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
package se.sics.ktoolbox.cc.heartbeat.util;

import com.google.common.primitives.Ints;
import java.nio.ByteBuffer;
import se.sics.caracaldb.Key;
import se.sics.caracaldb.KeyRange;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CCKeyFactory {

    public static Key getHeartbeatKey(byte[] schemaPrefix, byte[] overlayId, int slot) {
        Key.KeyBuilder key = new Key.KeyBuilder(schemaPrefix);
        key.append(new byte[]{(byte) overlayId.length});
        key.append(overlayId);
        key.append(Ints.toByteArray(slot));
        return key.get();
    }

    public static KeyRange getHeartbeatRange(byte[] schemaPrefix, byte[] overlayId) {
        Key.KeyBuilder prefix = new Key.KeyBuilder(schemaPrefix);
        prefix.append(new byte[]{(byte) overlayId.length});
        prefix.append(overlayId);
        return KeyRange.prefix(prefix.get());
    }

    public static byte[] extractOverlay(byte[] schemaPrefix, Key key) {
        ByteBuffer bbKey = key.getWrapper();
        bbKey.position(schemaPrefix.length);
        int overlaySize = bbKey.get();
        byte[] overlayId = new byte[overlaySize];
        bbKey.get(overlayId);
        return overlayId;
    }
}

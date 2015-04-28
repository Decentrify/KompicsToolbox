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
package se.sics.p2ptoolbox.caracalclient.heartbeat.util;

import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import se.sics.caracaldb.Key;
import se.sics.caracaldb.KeyRange;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CCKeyFactory {

    public static Key getHeartbeatKey(byte[] schemaPrefix, byte[] overlay, int slot) {
        Key.KeyBuilder key = new Key.KeyBuilder(schemaPrefix);
        key.append(overlay);
        key.append(new byte[]{(byte)overlay.length});
        key.append(Ints.toByteArray(slot));
        return key.get();
    }
    
    public static KeyRange getHeartbeatRange(byte[] schemaPrefix, byte[] overlay) {
        Key.KeyBuilder prefix = new Key.KeyBuilder(schemaPrefix);
        prefix.append(new byte[]{(byte)overlay.length});
        prefix.append(overlay);
        return KeyRange.prefix(prefix.get());
    }
    
    public static byte[] extractOverlay(byte[] schemaPrefix, Key key) {
        ByteBuffer bbKey = key.getWrapper();
        bbKey.position(schemaPrefix.length);
        int overlaySize = bbKey.get();
        byte[] overlay = new byte[overlaySize];
        bbKey.get(overlay);
        return overlay;
    }
}

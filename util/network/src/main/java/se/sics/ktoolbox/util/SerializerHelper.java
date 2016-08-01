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
package se.sics.ktoolbox.util;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;
import se.sics.kompics.network.netty.serialization.Serializers;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SerializerHelper {

    public static void stringToBinary(String val, ByteBuf buf) {
        byte[] stringB;
        try {
            stringB = val.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        buf.writeInt(stringB.length);
        buf.writeBytes(stringB);
    }

    public static String stringFromBinary(ByteBuf buf) {
        int stringLength = buf.readInt();
        byte[] stringB = new byte[stringLength];
        buf.readBytes(stringB);
        try {
            return new String(stringB, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <V extends Object> void sKMtoBinary(Map<String, V> map, Class<V> vClass, ByteBuf buf) {
        buf.writeInt(map.size());
        for (Map.Entry<String, V> e : map.entrySet()) {
            SerializerHelper.stringToBinary(e.getKey(), buf);
            Serializers.lookupSerializer(vClass).toBinary(e.getValue(), buf);
        }
    }

    public static <V extends Object> Map<String, V> sKMFromBinary(Class<V> vClass, ByteBuf buf) {
        int nrE = buf.readInt();
        Map<String, V> map = new TreeMap<>();
        for (int i = 0; i < nrE; i++) {
            String mapKey = SerializerHelper.stringFromBinary(buf);
            V mapValue = (V) Serializers.lookupSerializer(vClass).fromBinary(buf, Optional.absent());
            map.put(mapKey, mapValue);
        }
        return map;
    }
}

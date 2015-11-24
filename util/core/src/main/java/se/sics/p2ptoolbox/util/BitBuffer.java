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
package se.sics.p2ptoolbox.util;

import com.google.common.primitives.UnsignedBytes;
import java.util.ArrayList;
import java.util.Arrays;
import org.javatuples.Pair;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BitBuffer {

    private static final int ZERO = 0;
    private static final int[] POS = {1, 2, 4, 8, 16, 32, 64, 128};

    private final ArrayList<Boolean> buffer = new ArrayList<Boolean>();

    private BitBuffer() {
    }

    public static BitBuffer create(Pair<Integer, Boolean>... args) {
        BitBuffer b = new BitBuffer();
        b.write(args);
        return b;
    }

    public static BitBuffer create(int nrFlags) {
        BitBuffer b = new BitBuffer();
        for (int i = 0; i < nrFlags; i++) {
            b.write(Pair.with(i, false));
        }
        return b;
    }

    public static boolean[] extract(int numValues, byte[] bytes) {
        assert (((int) Math.ceil(((double) numValues) / 8.0)) <= bytes.length);

        boolean[] output = new boolean[numValues];
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i];
            for (int j = 0; j < 8; j++) {
                int pos = i * 8 + j;
                if (pos >= numValues) {
                    return output;
                }
                output[pos] = ((b & POS[j]) != 0);
            }
        }

        return output;
    }

    public BitBuffer write(Pair<Integer, Boolean>... args) {
        for (Pair<Integer, Boolean> arg : args) {
            while (arg.getValue0() >= buffer.size()) {
                buffer.add(false);
            }
            buffer.set(arg.getValue0(), arg.getValue1());
        }
        return this;
    }

    @Override
    public String toString() {
        return Arrays.toString(buffer.toArray());
    }

    public byte[] finalise() {
        int numBytes = (int) Math.ceil(((double) buffer.size()) / 8.0);
        byte[] bytes = new byte[numBytes];
        for (int i = 0; i < numBytes; i++) {
            int b = ZERO;
            for (int j = 0; j < 8; j++) {
                int pos = i * 8 + j;
                if (buffer.size() > pos) {
                    if (buffer.get(pos)) {
                        b = b ^ POS[j];
                    }
                }
            }
            bytes[i] = UnsignedBytes.checkedCast(b);
        }
        return bytes;
    }

}

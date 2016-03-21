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
package se.sics.ktoolbox.utility.test;

import java.util.Arrays;
import org.javatuples.Pair;
import org.junit.Assert;
import org.junit.Test;
import se.sics.ktoolbox.util.BitBuffer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BitBufferTest {

    @Test
    public void test1() {
        BitBuffer bb;
        bb = BitBuffer.create();
//        System.out.println(bb);
//        System.out.println(Arrays.toString(bb.finalise()));
        Assert.assertTrue(Arrays.equals(new byte[]{}, bb.finalise()));
    }

    @Test
    public void test2() {
        BitBuffer bb;
        bb = BitBuffer.create(Pair.with(0, true));
//        System.out.println(bb);
//        System.out.println(Arrays.toString(bb.finalise()));
        Assert.assertTrue(Arrays.equals(new byte[]{1}, bb.finalise()));
    }

    @Test
    public void test3() {
        BitBuffer bb;
        bb = BitBuffer.create(Pair.with(1, true));
//        System.out.println(bb);
//        System.out.println(Arrays.toString(bb.finalise()));
        Assert.assertTrue(Arrays.equals(new byte[]{2}, bb.finalise()));
    }

    @Test
    public void test4() {
        BitBuffer bb;
        bb = BitBuffer.create(Pair.with(1, true), Pair.with(3, true));
//        System.out.println(bb);
//        System.out.println(Arrays.toString(bb.finalise()));
        Assert.assertTrue(Arrays.equals(new byte[]{10}, bb.finalise()));
    }

    @Test
    public void test5() {
        BitBuffer bb;
        bb = BitBuffer.create(Pair.with(1, true), Pair.with(3, true), Pair.with(9, true));
//        System.out.println(bb);
//        System.out.println(Arrays.toString(bb.finalise()));
        Assert.assertTrue(Arrays.equals(new byte[]{10, 2}, bb.finalise()));
    }

    @Test
    public void test6() {
        BitBuffer bb;
        bb = BitBuffer.create(Pair.with(1, true), Pair.with(3, true), Pair.with(9, true));
//        System.out.println(bb);
//        System.out.println(Arrays.toString(bb.finalise()));
        Assert.assertTrue(Arrays.equals(new byte[]{10, 2}, bb.finalise()));
        bb.write(Pair.with(2, true));
//        System.out.println(bb);
//        System.out.println(Arrays.toString(bb.finalise()));
        Assert.assertTrue(Arrays.equals(new byte[]{14, 2}, bb.finalise()));
        bb.write(Pair.with(1, false));
//        System.out.println(bb);
//        System.out.println(Arrays.toString(bb.finalise()));
        Assert.assertTrue(Arrays.equals(new byte[]{12, 2}, bb.finalise()));
        bb.write(Pair.with(9, false));
//        System.out.println(bb);
//        System.out.println(Arrays.toString(bb.finalise()));
        Assert.assertTrue(Arrays.equals(new byte[]{12, 0}, bb.finalise()));
    }
    
}

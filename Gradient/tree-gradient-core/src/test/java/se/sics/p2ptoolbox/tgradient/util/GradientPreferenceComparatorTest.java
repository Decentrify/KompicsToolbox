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
package se.sics.p2ptoolbox.tgradient.util;

import com.google.common.primitives.Ints;
import java.util.ArrayList;
import java.util.Collections;
import se.sics.p2ptoolbox.gradient.util.GradientPreferenceComparator;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import se.sics.p2ptoolbox.gradient.util.GradientContainer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
@RunWith(JUnit4.class)
public class GradientPreferenceComparatorTest {

    List<TestGradientContainer> nodeList;

    @Before
    public void setup() {
        nodeList = new ArrayList<TestGradientContainer>();
        Random rand = new Random(1234l);
        for (int i = 0; i < 50; i++) {
            nodeList.add(new TestGradientContainer(rand.nextInt(300)));
        }
        System.out.println(nodeList);

    }

    @Test
    public void testEqualPreference() {
        TestGradientContainer base = new TestGradientContainer(100);
        TestGradientContainer tgc11 = new TestGradientContainer(11);
        TestGradientContainer tgc12 = new TestGradientContainer(12);
        TestGradientContainer tgc71 = new TestGradientContainer(71);
        TestGradientContainer tgc72 = new TestGradientContainer(72);
        TestGradientContainer tgc101 = new TestGradientContainer(101);
        TestGradientContainer tgc102 = new TestGradientContainer(102);

        int kCenterSize = 3;
        int branchingFactor = 2;
        ParentPreferenceComparator ppc = new ParentPreferenceComparator(base, branchingFactor, kCenterSize);
        System.out.println("me:"+ base.rank + " ideal parent:" + (base.rank - kCenterSize) / branchingFactor);
        Assert.assertEquals(0, ppc.compare(tgc11, tgc11));
        Assert.assertEquals(1, ppc.compare(tgc11, tgc12));
        Assert.assertEquals(-1, ppc.compare(tgc12, tgc11));
        
        Assert.assertEquals(-1, ppc.compare(tgc101, tgc102));
        Assert.assertEquals(1, ppc.compare(tgc102, tgc101));
        
        Assert.assertEquals(-1, ppc.compare(tgc71, tgc72));
        Assert.assertEquals(1, ppc.compare(tgc72, tgc71));
        
        Assert.assertEquals(-1, ppc.compare(tgc11, tgc101));
        Assert.assertEquals(1, ppc.compare(tgc101, tgc11));
        
        Assert.assertEquals(1, ppc.compare(tgc11, tgc71));
        Assert.assertEquals(-1, ppc.compare(tgc71, tgc11));
        
        Assert.assertEquals(1, ppc.compare(tgc101, tgc71));
        Assert.assertEquals(-1, ppc.compare(tgc71, tgc101));

        Collections.sort(nodeList, ppc);
        System.out.println(nodeList);
    }

    private static class TestObj {

        public final int rank;

        public TestObj(int rank) {
            this.rank = rank;
        }

        @Override
        public String toString() {
            return "" + rank;
        }
    }

    public static class TestGradientContainer extends GradientContainer {

        public TestGradientContainer(int rank) {
            super(null, null, 0, rank);
        }

        @Override
        public String toString() {
            return "" + rank;
        }
    }
}

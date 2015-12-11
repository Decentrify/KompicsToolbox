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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import se.sics.ktoolbox.gradient.util.GradientContainer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
@RunWith(JUnit4.class)
public class GradientPreferenceComparatorTest {

    @Test
    public void testEqualPreference() {
        List<TestGradientContainer> nodeList = new ArrayList<TestGradientContainer>();
        Random rand = new Random(1234l);
        for (int i = 0; i < 50; i++) {
            nodeList.add(new TestGradientContainer(rand.nextInt(300)));
        }
        System.out.println(nodeList);

        TestGradientContainer base = new TestGradientContainer(120);
        TestGradientContainer tgc11 = new TestGradientContainer(11);
        TestGradientContainer tgc12 = new TestGradientContainer(12);
        TestGradientContainer tgc71 = new TestGradientContainer(71);
        TestGradientContainer tgc72 = new TestGradientContainer(72);
        TestGradientContainer tgc101 = new TestGradientContainer(101);
        TestGradientContainer tgc102 = new TestGradientContainer(102);
        TestGradientContainer tgc131 = new TestGradientContainer(131);
        TestGradientContainer tgc132 = new TestGradientContainer(132);

        int kCenterSize = 3;
        int branching = 2;
        ParentPreferenceComparator ppc = new ParentPreferenceComparator(base, branching, kCenterSize);

        int myLevel = 0;

        if (base.rank > kCenterSize) {
            myLevel = (int) Math.floor(Math.log((double) base.rank / kCenterSize + 1) / Math.log(branching));
        }
        int lastParent = -1; //indexes start from 0
        for (int i = 0; i < myLevel; i++) {
            lastParent += (int) (kCenterSize * Math.pow(branching, i));
        }
        int idealParent = (base.rank - kCenterSize) / branching;

        System.out.println("me:" + base.rank + " ideal parent:" + idealParent + " lastParent:" + lastParent);
        Assert.assertEquals(0, ppc.compare(tgc11, tgc11));

        Assert.assertEquals(1, ppc.compare(tgc11, tgc12));
        Assert.assertEquals(-1, ppc.compare(tgc12, tgc11));

        Assert.assertEquals(-1, ppc.compare(tgc71, tgc72));
        Assert.assertEquals(1, ppc.compare(tgc72, tgc71));

        Assert.assertEquals(-1, ppc.compare(tgc101, tgc102));
        Assert.assertEquals(1, ppc.compare(tgc102, tgc101));

        Assert.assertEquals(-1, ppc.compare(tgc131, tgc132));
        Assert.assertEquals(1, ppc.compare(tgc132, tgc131));

        Assert.assertEquals(1, ppc.compare(tgc11, tgc71));
        Assert.assertEquals(-1, ppc.compare(tgc71, tgc11));

        Assert.assertEquals(-1, ppc.compare(tgc11, tgc101));
        Assert.assertEquals(1, ppc.compare(tgc101, tgc11));

        Assert.assertEquals(-1, ppc.compare(tgc11, tgc131));
        Assert.assertEquals(1, ppc.compare(tgc131, tgc11));

        Assert.assertEquals(-1, ppc.compare(tgc71, tgc101));
        Assert.assertEquals(1, ppc.compare(tgc101, tgc71));

        Assert.assertEquals(-1, ppc.compare(tgc71, tgc131));
        Assert.assertEquals(1, ppc.compare(tgc131, tgc71));

        Assert.assertEquals(-1, ppc.compare(tgc101, tgc131));
        Assert.assertEquals(1, ppc.compare(tgc131, tgc101));

        Collections.sort(nodeList, ppc);
        System.out.println(nodeList);
    }

    @Test
    public void test1() {
        int kCenterSize = 3;
        int branching = 2;
        TestGradientContainer base = new TestGradientContainer(3);
        ParentPreferenceComparator ppc = new ParentPreferenceComparator(base, branching, kCenterSize);

        List<TestGradientContainer> expected = new ArrayList<TestGradientContainer>();
        expected.add(new TestGradientContainer(0));
        expected.add(new TestGradientContainer(2));
        expected.add(new TestGradientContainer(4));

        List<TestGradientContainer> nodeList = new ArrayList<TestGradientContainer>();
        nodeList.add(new TestGradientContainer(4));
        nodeList.add(new TestGradientContainer(2));
        nodeList.add(new TestGradientContainer(0));

        Collections.sort(nodeList, ppc);
        System.out.println(nodeList);
        Assert.assertEquals(expected, nodeList);
    }

    @Test
    public void test2() {
        int kCenterSize = 3;
        int branching = 2;
        TestGradientContainer base = new TestGradientContainer(5);
        ParentPreferenceComparator ppc = new ParentPreferenceComparator(base, branching, kCenterSize);

        List<TestGradientContainer> expected = new ArrayList<TestGradientContainer>();
        expected.add(new TestGradientContainer(0));
        expected.add(new TestGradientContainer(2));
        expected.add(new TestGradientContainer(4));
        expected.add(new TestGradientContainer(6));

        List<TestGradientContainer> nodeList = new ArrayList<TestGradientContainer>();
        nodeList.add(new TestGradientContainer(6));
        nodeList.add(new TestGradientContainer(0));
        nodeList.add(new TestGradientContainer(4));
        nodeList.add(new TestGradientContainer(2));

        Collections.sort(nodeList, ppc);
        System.out.println(nodeList);
        Assert.assertEquals(expected, nodeList);
    }

    @Test
    public void test3() {
        int kCenterSize = 3;
        int branching = 2;
        TestGradientContainer base = new TestGradientContainer(10);
        ParentPreferenceComparator ppc = new ParentPreferenceComparator(base, branching, kCenterSize);

        List<TestGradientContainer> expected = new ArrayList<TestGradientContainer>();
        expected.add(new TestGradientContainer(3));
        expected.add(new TestGradientContainer(5));
        expected.add(new TestGradientContainer(8));
        expected.add(new TestGradientContainer(2));
        expected.add(new TestGradientContainer(0));
        expected.add(new TestGradientContainer(9));
        expected.add(new TestGradientContainer(11));

        List<TestGradientContainer> nodeList = new ArrayList<TestGradientContainer>();
        nodeList.add(new TestGradientContainer(11));
        nodeList.add(new TestGradientContainer(9));
        nodeList.add(new TestGradientContainer(0));
        nodeList.add(new TestGradientContainer(8));
        nodeList.add(new TestGradientContainer(3));
        nodeList.add(new TestGradientContainer(5));
        nodeList.add(new TestGradientContainer(2));

        Collections.sort(nodeList, ppc);
        System.out.println(nodeList);
        Assert.assertEquals(expected, nodeList);
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

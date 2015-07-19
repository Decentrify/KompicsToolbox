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
package se.sics.p2ptoolbox.gradient.core.util;

import java.util.ArrayList;
import java.util.Collections;
import se.sics.p2ptoolbox.gradient.util.GradientPreferenceComparator;
import java.util.Comparator;
import java.util.List;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import se.sics.p2ptoolbox.util.Java6Util;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
@RunWith(JUnit4.class)
public class GradientPreferenceComparatorTest {

    @Test
    public void testEqualPreference() {
        TestObj base = new TestObj(2);
        GradientPreferenceComparator<TestObj> pc = new GradientPreferenceComparator<TestObj>(base, new TestComparator());
        List<TestObj> myList;

        myList = new ArrayList<TestObj>();
        myList.add(new TestObj(2));
        myList.add(new TestObj(3));
        myList.add(new TestObj(2));
        Collections.sort(myList, pc);
        Assert.assertEquals(2, myList.get(1).val);
        Assert.assertEquals(2, myList.get(2).val);

        myList = new ArrayList<TestObj>();
        myList.add(new TestObj(1));
        myList.add(new TestObj(2));
        myList.add(new TestObj(2));
        Collections.sort(myList, pc);
        Assert.assertEquals(2, myList.get(1).val);
        Assert.assertEquals(2, myList.get(2).val);
    }

     @Test
    public void testPreference1() {
        TestObj base = new TestObj(5);
        GradientPreferenceComparator<TestObj> pc = new GradientPreferenceComparator<TestObj>(base, new TestComparator());
        List<TestObj> myList;

        myList = new ArrayList<TestObj>();
        myList.add(new TestObj(3));
        myList.add(new TestObj(4));
        Collections.sort(myList, pc);
        Assert.assertEquals(4, myList.get(1).val);
    }
    
    @Test
    public void testPreference2() {
        TestObj base = new TestObj(5);
        GradientPreferenceComparator<TestObj> pc = new GradientPreferenceComparator<TestObj>(base, new TestComparator());
        List<TestObj> myList;

        myList = new ArrayList<TestObj>();
        myList.add(new TestObj(6));
        myList.add(new TestObj(7));
        Collections.sort(myList, pc);
        Assert.assertEquals(6, myList.get(1).val);
    }
    
    @Test
    public void testPreference3() {
        TestObj base = new TestObj(5);
        GradientPreferenceComparator<TestObj> pc = new GradientPreferenceComparator<TestObj>(base, new TestComparator());
        List<TestObj> myList;

        myList = new ArrayList<TestObj>();
        myList.add(new TestObj(4));
        myList.add(new TestObj(7));
        Collections.sort(myList, pc);
        Assert.assertEquals(7, myList.get(1).val);
    }
    
    @Test
    public void testPreference4() {
        TestObj base = new TestObj(5);
        GradientPreferenceComparator<TestObj> pc = new GradientPreferenceComparator<TestObj>(base, new TestComparator());
        List<TestObj> myList;

        myList = new ArrayList<TestObj>();
        myList.add(new TestObj(4));
        myList.add(new TestObj(7));
        myList.add(new TestObj(8));
        Collections.sort(myList, pc);
        Assert.assertEquals(7, myList.get(2).val);
        Assert.assertEquals(8, myList.get(1).val);
    }
    
    @Test
    public void testPreference5() {
        TestObj base = new TestObj(5);
        GradientPreferenceComparator<TestObj> pc = new GradientPreferenceComparator<TestObj>(base, new TestComparator());
        List<TestObj> myList;

        myList = new ArrayList<TestObj>();
        myList.add(new TestObj(3));
        myList.add(new TestObj(4));
        myList.add(new TestObj(7));
        myList.add(new TestObj(8));
        Collections.sort(myList, pc);
        Assert.assertEquals(7, myList.get(3).val);
        Assert.assertEquals(8, myList.get(2).val);
        Assert.assertEquals(4, myList.get(1).val);
    }
    
    private static class TestObj {

        public final int val;

        public TestObj(int val) {
            this.val = val;
        }
        
        @Override
        public String toString() {
            return "" + val;
        }
    }

    private static class TestComparator implements Comparator<TestObj> {

        public int compare(TestObj o1, TestObj o2) {
            return Java6Util.compareInt(o1.val, o2.val);
        }
    }
}

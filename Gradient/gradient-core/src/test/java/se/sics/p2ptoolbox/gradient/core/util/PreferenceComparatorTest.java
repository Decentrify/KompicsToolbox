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

import java.util.Comparator;
import java.util.TreeSet;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import se.sics.p2ptoolbox.croupier.api.util.PeerView;
import se.sics.p2ptoolbox.util.Java6Util;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
@RunWith(JUnit4.class)
public class PreferenceComparatorTest {

    @Test
    public void test1() {
        GradientPreferenceComparator<MyPeerView> pc = new GradientPreferenceComparator<MyPeerView>(new MyPeerView(5), new MyPVComparator());
        TreeSet<MyPeerView> mySet = new TreeSet<MyPeerView>(pc);
        mySet.add(new MyPeerView(6));
        mySet.add(new MyPeerView(7));
        
        Assert.assertEquals(7, mySet.first().val);
    }
    
    @Test
    public void test2() {
        GradientPreferenceComparator<MyPeerView> pc = new GradientPreferenceComparator<MyPeerView>(new MyPeerView(5), new MyPVComparator());
        TreeSet<MyPeerView> mySet = new TreeSet<MyPeerView>(pc);
        mySet.add(new MyPeerView(3));
        mySet.add(new MyPeerView(4));
        
        Assert.assertEquals(3, mySet.first().val);
    }
    
    @Test
    public void test3() {
        GradientPreferenceComparator<MyPeerView> pc = new GradientPreferenceComparator<MyPeerView>(new MyPeerView(5), new MyPVComparator());
        TreeSet<MyPeerView> mySet = new TreeSet<MyPeerView>(pc);
        mySet.add(new MyPeerView(3));
        mySet.add(new MyPeerView(6));
        
        Assert.assertEquals(3, mySet.first().val);
    }

    private static class MyPeerView implements PeerView {
        public final int val;
        
        public MyPeerView(int val) {
            this.val = val;
        }
        
        public MyPeerView deepCopy() {
            return new MyPeerView(val);
        }
    }
    
    private static class MyPVComparator implements Comparator<MyPeerView> {
        public int compare(MyPeerView o1, MyPeerView o2) {
            return Java6Util.compareInt(o1.val, o2.val);
        }
    }
}

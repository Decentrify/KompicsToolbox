///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * GVoD is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package se.sics.ktoolbox.util.managedStore.core.impl.tracker;
//
//import se.sics.ktoolbox.util.stream.tracker.IncompleteTracker;
//import java.util.HashSet;
//import java.util.Set;
//import java.util.TreeSet;
//import org.junit.Assert;
//import org.junit.Test;
//
///**
// * @author Alex Ormenisan <aaor@sics.se>
// */
//public class ComponentTrackerTest {
//
//    @Test
//    public void test() {
//        IncompleteTracker tracker = IncompleteTracker.create(10);
//        Set<Integer> expected;
//
//        Assert.assertFalse(tracker.isComplete(0));
//
//        expected = new TreeSet<>();
//        expected.add(0);
//        expected.add(1);
//        expected.add(2);
//        Assert.assertEquals(expected, tracker.nextComponentMissing(0, 3, new HashSet<Integer>()));
//        Assert.assertEquals(0, tracker.nextComponentMissing(0));
//
//        tracker.addComponent(0);
//        tracker.addComponent(1);
//        tracker.addComponent(3);
//        tracker.addComponent(4);
//        tracker.addComponent(6);
//        Assert.assertFalse(tracker.isComplete(0));
//        Assert.assertEquals(2, tracker.nextComponentMissing(0));
//
//        expected = new TreeSet<>();
//        expected.add(2);
//        expected.add(5);
//        expected.add(7);
//        Assert.assertEquals(expected, tracker.nextComponentMissing(0, 3, new HashSet<Integer>()));
//
//        expected = new TreeSet<>();
//        expected.add(8);
//        expected.add(9);
//        Assert.assertEquals(expected, tracker.nextComponentMissing(8, 3, new HashSet<Integer>()));
//
//        tracker.addComponent(2);
//        tracker.addComponent(5);
//        Assert.assertEquals(7, tracker.nextComponentMissing(0));
//        tracker.addComponent(7);
//        tracker.addComponent(8);
//        tracker.addComponent(9);
//        Assert.assertEquals(-1, tracker.nextComponentMissing(0));
//        Assert.assertTrue(tracker.isComplete(0));
//    }
//    
//    @Test
//    public void nextPieceTest1() {
//        IncompleteTracker pt = IncompleteTracker.create(10);
//        
//        Assert.assertEquals((Integer)0, pt.nextComponentMissing(0, new HashSet<Integer>()));
//        
//        Assert.assertEquals((Integer)2, pt.nextComponentMissing(2, new HashSet<Integer>()));
//        
//        pt.addComponent(3);
//        Assert.assertEquals((Integer)4, pt.nextComponentMissing(3, new HashSet<Integer>()));
//    }
//}

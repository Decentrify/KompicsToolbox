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
package se.sics.ktoolbox.util.managedStore.core;

import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ManagedStoreHelperTest {

    @Test
    public void lastComponent1Test() {
        long storeSize = 10;
        int componentSize = 100;
        Pair<Integer, Integer> lastComponent = ManagedStoreHelper.lastComponent(storeSize, componentSize);
        Assert.assertEquals(0, (int) lastComponent.getValue0());
        Assert.assertEquals(10, (int) lastComponent.getValue1());
    }

    @Test
    public void lastComponent2Test() {
        long storeSize = 110;
        int componentSize = 100;
        Pair<Integer, Integer> lastComponent = ManagedStoreHelper.lastComponent(storeSize, componentSize);
        Assert.assertEquals(1, (int) lastComponent.getValue0());
        Assert.assertEquals(10, (int) lastComponent.getValue1());
    }

    @Test
    public void lastComponent3Test() {
        long storeSize = 100;
        int componentSize = 100;
        Pair<Integer, Integer> lastComponent = ManagedStoreHelper.lastComponent(storeSize, componentSize);
        Assert.assertEquals(0, (int) lastComponent.getValue0());
        Assert.assertEquals(100, (int) lastComponent.getValue1());
    }

    @Test
    public void lastComponent4Test() {
        long storeSize = 200;
        int componentSize = 100;
        Pair<Integer, Integer> lastComponent = ManagedStoreHelper.lastComponent(storeSize, componentSize);
        Assert.assertEquals(1, (int) lastComponent.getValue0());
        Assert.assertEquals(100, (int) lastComponent.getValue1());
    }

    @Test
    public void absolutePosToBlockDetails1Test() {
        Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> lastComponent = ManagedStoreHelper.blockDetails(0, 1024, 1024);
        Assert.assertEquals(0, (int) lastComponent.getValue0().getValue0());
        Assert.assertEquals(0, (int) lastComponent.getValue0().getValue1());
        Assert.assertEquals(0, (int) lastComponent.getValue1().getValue0());
        Assert.assertEquals(0, (int) lastComponent.getValue1().getValue1());
    }

    @Test
    public void absolutePosToBlockDetails2Test() {
        Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> lastComponent = ManagedStoreHelper.blockDetails(1023, 1024, 1024);
        Assert.assertEquals(0, (int) lastComponent.getValue0().getValue0());
        Assert.assertEquals(1023, (int) lastComponent.getValue0().getValue1());
        Assert.assertEquals(0, (int) lastComponent.getValue1().getValue0());
        Assert.assertEquals(0, (int) lastComponent.getValue1().getValue1());
    }

    @Test
    public void absolutePosToBlockDetails3Test() {
        Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> lastComponent = ManagedStoreHelper.blockDetails(1024, 1024, 1024);
        Assert.assertEquals(1, (int) lastComponent.getValue0().getValue0());
        Assert.assertEquals(0, (int) lastComponent.getValue0().getValue1());
        Assert.assertEquals(0, (int) lastComponent.getValue1().getValue0());
        Assert.assertEquals(1, (int) lastComponent.getValue1().getValue1());
    }

    @Test
    public void absolutePosToBlockDetails4Test() {
        Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> lastComponent = ManagedStoreHelper.blockDetails(1024 * 1024 - 1, 1024, 1024);
        Assert.assertEquals(1023, (int) lastComponent.getValue0().getValue0());
        Assert.assertEquals(1023, (int) lastComponent.getValue0().getValue1());
        Assert.assertEquals(0, (int) lastComponent.getValue1().getValue0());
        Assert.assertEquals(1023, (int) lastComponent.getValue1().getValue1());
    }
    
    @Test
    public void absolutePosToBlockDetails5Test() {
        Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> lastComponent = ManagedStoreHelper.blockDetails(10*1024 * 1024 - 1, 1024, 1024);
        Assert.assertEquals(10*1024-1, (int) lastComponent.getValue0().getValue0());
        Assert.assertEquals(1023, (int) lastComponent.getValue0().getValue1());
        Assert.assertEquals(9, (int) lastComponent.getValue1().getValue0());
        Assert.assertEquals(1023, (int) lastComponent.getValue1().getValue1());
    }
}

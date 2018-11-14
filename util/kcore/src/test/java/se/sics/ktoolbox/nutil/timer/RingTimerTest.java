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
package se.sics.ktoolbox.nutil.timer;

import java.util.List;
import java.util.Optional;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.timer.RingTimer.Container;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class RingTimerTest {

  @BeforeClass
  public static void setup() {
    IdentifierRegistryV2.registerBaseDefaults1(64);
  }

  @Test
  public void simpleTest() {
    IdentifierFactory ids = IdentifierRegistryV2.instance(BasicIdentifiers.Values.EVENT, Optional.of(1234l));

    long windowSize = 50;
    long maxTimeout = 25000;
    RingTimer timer = new RingTimer(windowSize, maxTimeout);

    timer.setTimeout(20, new TestContainer(ids.randomId()));
    Assert.assertEquals(1, timer.windowTick().size());

    timer.setTimeout(20, new TestContainer(ids.randomId()));
    timer.setTimeout(20, new TestContainer(ids.randomId()));
    Assert.assertEquals(2, timer.windowTick().size());

    timer.setTimeout(50, new TestContainer(ids.randomId()));
    Assert.assertEquals(0, timer.windowTick().size());
    Assert.assertEquals(1, timer.windowTick().size());

    timer.setTimeout(20, new TestContainer(ids.randomId()));
    timer.setTimeout(50, new TestContainer(ids.randomId()));
    Assert.assertEquals(1, timer.windowTick().size());
    Assert.assertEquals(1, timer.windowTick().size());

    timer.setTimeout(20, new TestContainer(ids.randomId()));
    timer.setTimeout(50, new TestContainer(ids.randomId()));
    timer.setTimeout(70, new TestContainer(ids.randomId()));
    Assert.assertEquals(1, timer.windowTick().size());
    Assert.assertEquals(2, timer.windowTick().size());
  }

  //150 nanos
  @Test
  public void testTimeSetOnEmpty() {
    SummaryStatistics s1 = new SummaryStatistics();
    IdentifierFactory ids = IdentifierRegistryV2.instance(BasicIdentifiers.Values.EVENT, Optional.of(1234l));
    long windowSize = 50;
    long maxTimeout = 25000;
    RingTimer timer = new RingTimer(windowSize, maxTimeout);

    for (int i = 0; i < 1000000; i++) {
      Identifier timerId = ids.randomId();
      long start = System.nanoTime();
      timer.setTimeout(70, new TestContainer(timerId));
      long stop = System.nanoTime();
      s1.addValue(stop - start);
      Assert.assertEquals(0, timer.windowTick().size());
      Assert.assertEquals(1, timer.windowTick().size());
    }
    System.err.println("set on empty wheel mean:" + s1.getMean());
    Assert.assertTrue("set on empty wheel should take bellow 200 nanos(100)", s1.getMean() < 200);
  }

  //50-100 nanos
  @Test
  public void testTimerCancel() {
    SummaryStatistics s1 = new SummaryStatistics();
    IdentifierFactory ids = IdentifierRegistryV2.instance(BasicIdentifiers.Values.EVENT, Optional.of(1234l));
    long windowSize = 50;
    long maxTimeout = 25000;
    RingTimer timer = new RingTimer(windowSize, maxTimeout);

    for (int i = 0; i < 1000000; i++) {
      Identifier timerId = ids.randomId();
      timer.setTimeout(70, new TestContainer(timerId));
      long start = System.nanoTime();
      timer.cancelTimeout(timerId);
      long stop = System.nanoTime();
      s1.addValue(stop - start);
      Assert.assertEquals(0, timer.windowTick().size());
      Assert.assertEquals(0, timer.windowTick().size());
    }
    System.err.println("cancel mean:" + s1.getMean());
    Assert.assertTrue("cancel mean should take bellow 200 nanos(100)", s1.getMean() < 200);
  }

  /*
   * 500 cancelled and 5 timeouts per tick
   * 10micros
   */
  @Test
  public void testTimerTick1() {
    SummaryStatistics s1 = new SummaryStatistics();
    IdentifierFactory ids = IdentifierRegistryV2.instance(BasicIdentifiers.Values.EVENT, Optional.of(1234l));
    long windowSize = 50;
    long maxTimeout = 25000;
    RingTimer timer = new RingTimer(windowSize, maxTimeout);

    for (int i = 0; i < 100000; i++) {
      for (int j = 0; j < 500; j++) {
        Identifier timerId = ids.randomId();
        timer.setTimeout(30, new TestContainer(timerId));
        timer.cancelTimeout(timerId);
      }
      for (int j = 0; j < 5; j++) {
        Identifier timerId = ids.randomId();
        timer.setTimeout(30, new TestContainer(timerId));
      }
      long start = System.nanoTime();
      List timedout = timer.windowTick();
      long stop = System.nanoTime();
      s1.addValue(stop - start);
      Assert.assertEquals(5, timedout.size());
    }
    System.err.println("tick 500/5 mean:" + s1.getMean());
    Assert.assertTrue("tick 500/5 should take bellow 20 micros(10)", s1.getMean() < 20*1000);
  }
  
  /*
   * 250 cancelled and 250 timeouts per tick
   * 13micros
   */
  @Test
  public void testTimerTick2() {
    SummaryStatistics s1 = new SummaryStatistics();
    IdentifierFactory ids = IdentifierRegistryV2.instance(BasicIdentifiers.Values.EVENT, Optional.of(1234l));
    long windowSize = 50;
    long maxTimeout = 25000;
    RingTimer timer = new RingTimer(windowSize, maxTimeout);

    for (int i = 0; i < 100000; i++) {
      for (int j = 0; j < 250; j++) {
        Identifier timerId = ids.randomId();
        timer.setTimeout(30, new TestContainer(timerId));
        timer.cancelTimeout(timerId);
      }
      for (int j = 0; j < 250; j++) {
        Identifier timerId = ids.randomId();
        timer.setTimeout(30, new TestContainer(timerId));
      }
      long start = System.nanoTime();
      List timedout = timer.windowTick();
      long stop = System.nanoTime();
      s1.addValue(stop - start);
      Assert.assertEquals(250, timedout.size());
    }
    System.err.println("tick 250/250 mean:" + s1.getMean());
    Assert.assertTrue("tick 250/250 should take bellow 26 micros(13)", s1.getMean() < 26*1000);
  }

  public static class TestContainer implements Container {

    public final Identifier id;

    public TestContainer(Identifier id) {
      this.id = id;
    }

    @Override
    public Identifier getId() {
      return id;
    }
  }
}

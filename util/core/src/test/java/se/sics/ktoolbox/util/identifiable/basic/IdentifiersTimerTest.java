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
package se.sics.ktoolbox.util.identifiable.basic;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Assert;
import org.junit.Test;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class IdentifiersTimerTest {

  @Test
  public void UUIDTest() {
    SummaryStatistics s1 = new SummaryStatistics();
    for (int i = 0; i < 1000000; i++) {
      long start = System.nanoTime();
      UUID.randomUUID();
      long stop = System.nanoTime();
      s1.addValue(stop - start);
    }
    System.err.println("uuid random:" + s1.getMean());
    Assert.assertTrue("uuid random should take bellow 3000 nanos(1500)", s1.getMean() < 3000);
  }

  @Test
  public void RandomIntTest() {
    SummaryStatistics s1 = new SummaryStatistics();
    Random rand = new Random(1234l);
    for (int i = 0; i < 1000000; i++) {
      long start = System.nanoTime();
      rand.nextInt();
      long stop = System.nanoTime();
      s1.addValue(stop - start);
    }
    System.err.println("random int mean:" + s1.getMean());
    Assert.assertTrue("random int should take bellow 200 nanos(100)", s1.getMean() < 200);
  }

  @Test
  public void ByteArrayTest() {
    SummaryStatistics s1 = new SummaryStatistics();
    Random rand = new Random(1234l);
    for (int i = 0; i < 1000000; i++) {
      byte[] b = new byte[128];
      long start = System.nanoTime();
      rand.nextBytes(b);
      long stop = System.nanoTime();
      s1.addValue(stop - start);
    }
    System.err.println("random bytes 128 mean:" + s1.getMean());
    Assert.assertTrue("random bytes 128 should take bellow 1000 nanos(500)", s1.getMean() < 1000);

  }

  @Test
  public void UUIDFactTest() {
    SummaryStatistics s1 = new SummaryStatistics();
    IdentifierFactory ids = new UUIDIdFactory();
    for (int i = 0; i < 1000000; i++) {
      long start = System.nanoTime();
      ids.randomId();
      long stop = System.nanoTime();
      s1.addValue(stop - start);
    }
    System.err.println("fact uuid random:" + s1.getMean());
    Assert.assertTrue("fact uuid random should take bellow 3000 nanos(1500)", s1.getMean() < 3000);

  }

  @Test
  public void IntFactTest() {
    SummaryStatistics s1 = new SummaryStatistics();
    IdentifierFactory ids = new IntIdFactory(Optional.of(1234l));
    for (int i = 0; i < 1000000; i++) {
      long start = System.nanoTime();
      ids.randomId();
      long stop = System.nanoTime();
      s1.addValue(stop - start);
    }
    System.err.println("fact int random:" + s1.getMean());
    Assert.assertTrue("fact int random should take bellow 3000 nanos(1500)", s1.getMean() < 3000);
  }

  @Test
  public void ByteFactTest() {
    SummaryStatistics s1 = new SummaryStatistics();
    IdentifierFactory ids = new SimpleByteIdFactory(Optional.of(1234l), 128);
    for (int i = 0; i < 1000000; i++) {
      long start = System.nanoTime();
      ids.randomId();
      long stop = System.nanoTime();
      s1.addValue(stop - start);
    }
    System.err.println("fact byte 128 random:" + s1.getMean());
    Assert.assertTrue("fact byte 128 random should take bellow 3000 nanos(1500)", s1.getMean() < 3000);
  }
}

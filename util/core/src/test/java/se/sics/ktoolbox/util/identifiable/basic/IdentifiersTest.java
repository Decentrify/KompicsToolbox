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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.javatuples.Pair;
import org.junit.Assert;
import org.junit.Test;
import se.sics.kompics.util.Identifier;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class IdentifiersTest {

  @Test
  public void pairTest() {
    UUID id1 = UUID.randomUUID();
    Identifier msgId1 = new UUIDId(id1);
    Identifier nodeId1 = new IntId(1);
    Pair<Identifier, Identifier> pairId1 = Pair.with(msgId1, nodeId1);
    System.err.println(pairId1.getValue0().toString() + " " + pairId1.getValue1().toString());

    Identifier msgId2 = new UUIDId(id1);
    Identifier nodeId2 = new IntId(1);
    Pair<Identifier, Identifier> pairId2 = Pair.with(msgId2, nodeId2);

    Assert.assertEquals(pairId1, pairId2);

    Map<Pair<Identifier, Identifier>, Boolean> m = new HashMap<>();
    m.put(pairId1, true);
    Assert.assertTrue(m.containsKey(pairId2));
  }

  @Test
  public void partitionTest() {
    for (int i = 0; i < 100; i++) {
      UUID id1 = UUID.randomUUID();
      Identifier msgId1 = new UUIDId(id1);
      System.out.println(msgId1.partition(2));
    }
  }
}

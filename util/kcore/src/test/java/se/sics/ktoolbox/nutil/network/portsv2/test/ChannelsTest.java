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
package se.sics.ktoolbox.nutil.network.portsv2.test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import org.junit.Ignore;
import org.junit.Test;
import se.sics.kompics.Kompics;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.util.identifiable.BasicBuilders;
import se.sics.ktoolbox.util.identifiable.basic.IntIdFactory;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ChannelsTest {

  @Ignore //run manually 
  @Test
  public void testFilterMsgsOnOnePort() throws UnknownHostException {
    IntIdFactory ids = new IntIdFactory(Optional.empty());
    Identifier id = ids.id(new BasicBuilders.IntBuilder(0));
    KAddress self = new BasicAddress(InetAddress.getLocalHost(), 20000, id);

    if (Kompics.isOn()) {
      Kompics.shutdown();
    }
    // Yes 20 is totally arbitrary
    Kompics.createAndStart(se.sics.ktoolbox.nutil.network.portsv2.oMsgFilter.ReceiverHostComp.class,
      new se.sics.ktoolbox.nutil.network.portsv2.oMsgFilter.ReceiverHostComp.Init(self),
      Runtime.getRuntime().availableProcessors(), 20);
    try {
      Kompics.waitForTermination();
    } catch (InterruptedException ex) {
      System.exit(1);
    }
  }

  @Ignore //run manually 
  @Test
  public void testOne2ManyMsgChannelDstOneBatch() throws UnknownHostException {
    IntIdFactory ids = new IntIdFactory(Optional.empty());
    Identifier id1 = ids.id(new BasicBuilders.IntBuilder(1));
    Identifier id2 = ids.id(new BasicBuilders.IntBuilder(2));
    Identifier id3 = ids.id(new BasicBuilders.IntBuilder(3));
    KAddress self = new BasicAddress(InetAddress.getLocalHost(), 20000, id1);
    KAddress dst1 = new BasicAddress(InetAddress.getLocalHost(), 20000, id2);
    KAddress dst2 = new BasicAddress(InetAddress.getLocalHost(), 20000, id3);

    if (Kompics.isOn()) {
      Kompics.shutdown();
    }
    // Yes 20 is totally arbitrary
    Kompics.createAndStart(se.sics.ktoolbox.nutil.network.portsv2.o2mMsgDstOneBatch.ReceiverHostComp.class,
      new se.sics.ktoolbox.nutil.network.portsv2.o2mMsgDstOneBatch.ReceiverHostComp.Init(self, dst1, dst2),
      Runtime.getRuntime().availableProcessors(), 20);
    try {
      Kompics.waitForTermination();
    } catch (InterruptedException ex) {
      System.exit(1);
    }
  }

  @Ignore //run manually 
  @Test
  public void testOne2ManyMsgChannelDstTwoBatch() throws UnknownHostException {
    IntIdFactory ids = new IntIdFactory(Optional.empty());
    Identifier id1 = ids.id(new BasicBuilders.IntBuilder(1));
    Identifier id2 = ids.id(new BasicBuilders.IntBuilder(2));
    Identifier id3 = ids.id(new BasicBuilders.IntBuilder(3));
    KAddress self = new BasicAddress(InetAddress.getLocalHost(), 20000, id1);
    KAddress dst1 = new BasicAddress(InetAddress.getLocalHost(), 20000, id2);
    KAddress dst2 = new BasicAddress(InetAddress.getLocalHost(), 20000, id3);

    if (Kompics.isOn()) {
      Kompics.shutdown();
    }
    // Yes 20 is totally arbitrary
    Kompics.createAndStart(se.sics.ktoolbox.nutil.network.portsv2.o2mMsgDstTwoBatch.ReceiverHostComp.class,
      new se.sics.ktoolbox.nutil.network.portsv2.o2mMsgDstTwoBatch.ReceiverHostComp.Init(self, dst1, dst2),
      Runtime.getRuntime().availableProcessors(), 20);
    try {
      Kompics.waitForTermination();
    } catch (InterruptedException ex) {
      System.exit(1);
    }
  }

  @Ignore //run manually 
  @Test
  public void testOne2ManyMsgChannelDstDoubleBatch() throws UnknownHostException {
    IntIdFactory ids = new IntIdFactory(Optional.empty());
    Identifier id1 = ids.id(new BasicBuilders.IntBuilder(1));
    Identifier id2 = ids.id(new BasicBuilders.IntBuilder(2));
    Identifier id3 = ids.id(new BasicBuilders.IntBuilder(3));
    KAddress self = new BasicAddress(InetAddress.getLocalHost(), 20000, id1);
    KAddress dst1 = new BasicAddress(InetAddress.getLocalHost(), 20000, id2);
    KAddress dst2 = new BasicAddress(InetAddress.getLocalHost(), 20000, id3);

    if (Kompics.isOn()) {
      Kompics.shutdown();
    }
    // Yes 20 is totally arbitrary
    Kompics.createAndStart(se.sics.ktoolbox.nutil.network.portsv2.o2mMsgDstDualBatch.ReceiverHostComp.class,
      new se.sics.ktoolbox.nutil.network.portsv2.o2mMsgDstDualBatch.ReceiverHostComp.Init(self, dst1, dst2),
      Runtime.getRuntime().availableProcessors(), 20);
    try {
      Kompics.waitForTermination();
    } catch (InterruptedException ex) {
      System.exit(1);
    }
  }
}

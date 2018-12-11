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
package se.sics.ktoolbox.nutil.conn.base.test;

import java.io.File;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import se.sics.kompics.Init;
import se.sics.kompics.Kompics;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.util.identifiable.BasicBuilders;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.identifiable.basic.IntIdFactory;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ConnectionTest {

  @BeforeClass
  public static void setup() throws MalformedURLException {
    String log4jFile = System.getProperty("user.dir")
      + File.separator + "src"
      + File.separator + "test"
      + File.separator + "resources"
      + File.separator + "se"
      + File.separator + "sics"
      + File.separator + "ktoolbox"
      + File.separator + "nutil"
      + File.separator + "conn"
      + File.separator + "base"
      + File.separator + "test"
      + File.separator + "log4j.properties";
    System.setProperty("log4j.configuration", new File(log4jFile).toURI().toURL().toString());
    IdentifierRegistryV2.registerBaseDefaults1(64, 1234l);
  }

//  @Ignore //run manually 
  @Test
  public void testSimpleProxy() throws UnknownHostException {
    IntIdFactory ids = new IntIdFactory(Optional.empty());
    Identifier overlayId = ids.id(new BasicBuilders.IntBuilder(0));
    
    Identifier id1 = ids.id(new BasicBuilders.IntBuilder(1));
    Identifier id2 = ids.id(new BasicBuilders.IntBuilder(2));
    KAddress serverAdr = new BasicAddress(InetAddress.getLocalHost(), 20000, id1);
    KAddress clientAdr = new BasicAddress(InetAddress.getLocalHost(), 20000, id2);

    Identifier serverBatchId = ids.id(new BasicBuilders.IntBuilder(1));
    Identifier serverBaseId = ids.id(new BasicBuilders.IntBuilder(1));

    Init init = new se.sics.ktoolbox.nutil.conn.base.simpleProxy.HostComp.Init(overlayId, serverAdr, clientAdr, 
      serverBatchId, serverBaseId);
    if (Kompics.isOn()) {
      Kompics.shutdown();
    }
    // Yes 20 is totally arbitrary
    Kompics.createAndStart(se.sics.ktoolbox.nutil.conn.base.simpleProxy.HostComp.class, init,
      Runtime.getRuntime().availableProcessors(), 20);
    try {
      Kompics.waitForTermination();
    } catch (InterruptedException ex) {
      System.exit(1);
    }
  }
  
  @Ignore //run manually 
  @Test
  public void testSimpleProxyMngr() throws UnknownHostException {
    IntIdFactory ids = new IntIdFactory(Optional.empty());
    Identifier overlayId = ids.id(new BasicBuilders.IntBuilder(0));
    
    Identifier id1 = ids.id(new BasicBuilders.IntBuilder(1));
    Identifier id2 = ids.id(new BasicBuilders.IntBuilder(2));
    KAddress serverAdr = new BasicAddress(InetAddress.getLocalHost(), 20000, id1);
    KAddress clientAdr = new BasicAddress(InetAddress.getLocalHost(), 20000, id2);

    Identifier serverBatchId = ids.id(new BasicBuilders.IntBuilder(1));
    Identifier serverBaseId = ids.id(new BasicBuilders.IntBuilder(1));

    Init init = new se.sics.ktoolbox.nutil.conn.base.simpleProxyMngr.HostComp.Init(overlayId, serverAdr, clientAdr, 
      serverBatchId, serverBaseId);
    if (Kompics.isOn()) {
      Kompics.shutdown();
    }
    // Yes 20 is totally arbitrary
    Kompics.createAndStart(se.sics.ktoolbox.nutil.conn.base.simpleProxyMngr.HostComp.class, init,
      Runtime.getRuntime().availableProcessors(), 20);
    try {
      Kompics.waitForTermination();
    } catch (InterruptedException ex) {
      System.exit(1);
    }
  }

  @Ignore //run manually 
  @Test
  public void testConnection2by2with2() throws UnknownHostException {
    IntIdFactory ids = new IntIdFactory(Optional.empty());
    Identifier overlayId = ids.id(new BasicBuilders.IntBuilder(0));
    Identifier id1 = ids.id(new BasicBuilders.IntBuilder(1));
    Identifier id2 = ids.id(new BasicBuilders.IntBuilder(2));
    Identifier id3 = ids.id(new BasicBuilders.IntBuilder(3));
    Identifier id4 = ids.id(new BasicBuilders.IntBuilder(4));
    KAddress serverAdr1 = new BasicAddress(InetAddress.getLocalHost(), 20000, id1);
    KAddress serverAdr2 = new BasicAddress(InetAddress.getLocalHost(), 20000, id2);
    KAddress clientAdr1 = new BasicAddress(InetAddress.getLocalHost(), 20000, id3);
    KAddress clientAdr2 = new BasicAddress(InetAddress.getLocalHost(), 20000, id4);

    Identifier serverBatchId1 = ids.id(new BasicBuilders.IntBuilder(1));
    Identifier serverBatchId2 = ids.id(new BasicBuilders.IntBuilder(2));
    Identifier serverBatchId3 = ids.id(new BasicBuilders.IntBuilder(3));
    Identifier serverBatchId4 = ids.id(new BasicBuilders.IntBuilder(4));

    Identifier serverBaseId1 = ids.id(new BasicBuilders.IntBuilder(1));
    Identifier serverBaseId2 = ids.id(new BasicBuilders.IntBuilder(2));
    Identifier serverBaseId3 = ids.id(new BasicBuilders.IntBuilder(3));
    Identifier serverBaseId4 = ids.id(new BasicBuilders.IntBuilder(4));

    Init init = new se.sics.ktoolbox.nutil.conn.base.multi2by2with2.HostComp.Init(overlayId, serverAdr1, serverAdr2,
      serverBatchId1, serverBaseId1, serverBatchId2, serverBaseId2,
      serverBatchId3, serverBaseId3, serverBatchId4, serverBaseId4,
      clientAdr1, clientAdr2);
    if (Kompics.isOn()) {
      Kompics.shutdown();
    }
    // Yes 20 is totally arbitrary
    Kompics.createAndStart(se.sics.ktoolbox.nutil.conn.base.multi2by2with2.HostComp.class, init,
      Runtime.getRuntime().availableProcessors(), 20);
    try {
      Kompics.waitForTermination();
    } catch (InterruptedException ex) {
      System.exit(1);
    }
  }
}

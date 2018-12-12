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
package se.sics.ktoolbox.nutil.conn.workers.test;

import java.io.File;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import se.sics.kompics.Kompics;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.conn.ConnIds;
import se.sics.ktoolbox.nutil.conn.workers.MngrCtrl;
import se.sics.ktoolbox.nutil.conn.workers.MngrState;
import se.sics.ktoolbox.nutil.conn.workers.WorkCtrl;
import se.sics.ktoolbox.nutil.conn.workers.WorkState;
import se.sics.ktoolbox.util.identifiable.BasicBuilders;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.identifiable.basic.IntIdFactory;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CtrlWorkerTest {

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
      + File.separator + "workers"
      + File.separator + "test"
      + File.separator + "log4j.properties";
    String configFile = System.getProperty("user.dir")
      + File.separator + "src"
      + File.separator + "test"
      + File.separator + "resources"
      + File.separator + "se"
      + File.separator + "sics"
      + File.separator + "ktoolbox"
      + File.separator + "nutil"
      + File.separator + "conn"
      + File.separator + "workers"
      + File.separator + "test"
      + File.separator + "application.conf";
    System.setProperty("log4j.configuration", new File(log4jFile).toURI().toURL().toString());
    System.setProperty("config.file", new File(configFile).getAbsolutePath());
    IdentifierRegistryV2.registerBaseDefaults1(64, 1234l);
  }

  @Ignore //run manually 
  @Test
  public void testFilterMsgsOnOnePort() throws UnknownHostException {
    IntIdFactory ids = new IntIdFactory(Optional.empty());
    Identifier node1 = ids.id(new BasicBuilders.IntBuilder(1));
    Identifier node2 = ids.id(new BasicBuilders.IntBuilder(2));
    KAddress ctrlCenterAdr = new BasicAddress(InetAddress.getLocalHost(), 20000, node1);
    KAddress workCenterAdr = new BasicAddress(InetAddress.getLocalHost(), 20000, node2);

    Identifier overlayId = ids.id(new BasicBuilders.IntBuilder(1));
    Identifier ctrlBatchId = ids.id(new BasicBuilders.IntBuilder(0));
    Identifier workBatchId = ids.id(new BasicBuilders.IntBuilder(1));
    Identifier baseId = ids.id(new BasicBuilders.IntBuilder(0));

    if (Kompics.isOn()) {
      Kompics.shutdown();
    }
    // Yes 20 is totally arbitrary
    Kompics.createAndStart(se.sics.ktoolbox.nutil.conn.workers.simple.HostComp.class,
      new se.sics.ktoolbox.nutil.conn.workers.simple.HostComp.Init(ctrlCenterAdr, workCenterAdr, overlayId, ctrlBatchId,
        workBatchId, baseId, ctrlServerC(), workServerC()),
      Runtime.getRuntime().availableProcessors(), 20);
    try {
      Kompics.waitForTermination();
    } catch (InterruptedException ex) {
      System.exit(1);
    }
  }

  private MngrCtrl.Server<MngrState.Client> ctrlServerC() {
    return new MngrCtrl.Server<MngrState.Client>() {
      @Override
      public void connected(ConnIds.ConnId ctrlConnId, KAddress peerId) {
        System.err.println("mngr connected");
      }
    };
  }

  private WorkCtrl.Server<WorkState.Client> workServerC() {
    return new WorkCtrl.Server<WorkState.Client>() {
      @Override
      public void connected(ConnIds.ConnId connId, KAddress peer, WorkState.Client peerState) {
        System.err.println("worker connected");
      }
    };
  }
  
//  {
//    ConnIds.InstanceId workClientId = new ConnIds.InstanceId(overlayId, selfAdr.getId(), workBatchId, baseId,
//            false);
//          ConnIds.InstanceId workServerId
//            = new ConnIds.InstanceId(overlayId, peerAdr.getId(), workBatchId, baseId, true);
//          ConnStatus connStatus = mngrServerC.connect(connId, peerAdr, peerStatus);
//          if (ConnStatus.Base.CONNECTED.equals(connStatus)) {
//            connectWorkClient(workClientId, workServerId, peerAdr, workConnConfig);
//          } else if (ConnStatus.Base.DISCONNECTED.equals(connStatus)) {
//            return Pair.with(connId, connStatus);
//          } else {
//            throw new RuntimeException("ctrl center server - not sure what to do:" + connStatus);
//          }
//  }
}

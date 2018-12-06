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
package se.sics.ktoolbox.overlaymngr.bootstrap.test;

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
import se.sics.kompics.config.TypesafeConfig;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.omngr.bootstrap.BootstrapClientConfig;
import se.sics.ktoolbox.util.identifiable.BasicBuilders;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.identifiable.basic.IntIdFactory;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BootstrapTest {

  @BeforeClass
  public static void setup() throws MalformedURLException {
    String log4jFile = System.getProperty("user.dir")
      + File.separator + "src"
      + File.separator + "test"
      + File.separator + "resources"
      + File.separator + "se"
      + File.separator + "sics"
      + File.separator + "ktoolbox"
      + File.separator + "overlaymngr"
      + File.separator + "bootstrap"
      + File.separator + "test"
      + File.separator + "log4j.properties";
    String configFile = System.getProperty("user.dir")
      + File.separator + "src"
      + File.separator + "test"
      + File.separator + "resources"
      + File.separator + "se"
      + File.separator + "sics"
      + File.separator + "ktoolbox"
      + File.separator + "overlaymngr"
      + File.separator + "bootstrap"
      + File.separator + "test"
      + File.separator + "application.conf";
    System.setProperty("log4j.configuration", new File(log4jFile).toURI().toURL().toString());
    System.setProperty("config.file", new File(configFile).getAbsolutePath());
    IdentifierRegistryV2.registerBaseDefaults1(64, 1234l);
  }

  @Ignore //run manually 
  @Test
  public void testSimpleProxy() throws UnknownHostException {
    IntIdFactory ids = new IntIdFactory(Optional.empty());
    Identifier overlayId1 = ids.id(new BasicBuilders.IntBuilder(0));
    Identifier overlayId2 = ids.id(new BasicBuilders.IntBuilder(1));

    Identifier id1 = ids.id(new BasicBuilders.IntBuilder(1));
    KAddress clientAdr = new BasicAddress(InetAddress.getLocalHost(), 20000, id1);

    BootstrapClientConfig clientConfig = BootstrapClientConfig.instance(TypesafeConfig.load()).get();

    Init init = new se.sics.ktoolbox.overlaymngr.bootstrap.setup.HostComp.Init(clientConfig.serverAdr, clientAdr,
      overlayId1, overlayId2);

    if (Kompics.isOn()) {
      Kompics.shutdown();
    }
    // Yes 20 is totally arbitrary
    Kompics.createAndStart(se.sics.ktoolbox.overlaymngr.bootstrap.setup.HostComp.class, init,
      Runtime.getRuntime().availableProcessors(), 20);
    try {
      Kompics.waitForTermination();
    } catch (InterruptedException ex) {
      System.exit(1);
    }
  }
}

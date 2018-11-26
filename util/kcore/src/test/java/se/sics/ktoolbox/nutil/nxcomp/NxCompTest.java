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
package se.sics.ktoolbox.nutil.nxcomp;

import com.google.common.base.Optional;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.kompics.Init;
import se.sics.kompics.Kompics;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NxCompTest {

  @BeforeClass
  public static void setup() {
    Config.Impl config = (Config.Impl) Kompics.getConfig();
    Config.Builder builder = Kompics.getConfig().modify(UUID.randomUUID());
//    TorrentIds.registerDefaults(builder.getValue("system.seed", Long.class));
    IdentifierRegistryV2.registerBaseDefaults1(64);
    config.apply(builder.finalise(), (Optional) Optional.absent());
    Kompics.setConfig(config);
  }

  @Test
  public void testComp() {

    if (Kompics.isOn()) {
      Kompics.shutdown();
    }
    // Yes 20 is totally arbitrary
    Kompics.createAndStart(LauncherComp.class, Init.NONE, Runtime.getRuntime().availableProcessors(), 20);
    try {
      Kompics.waitForTermination();
    } catch (InterruptedException ex) {
      System.exit(1);
    }
  }
}

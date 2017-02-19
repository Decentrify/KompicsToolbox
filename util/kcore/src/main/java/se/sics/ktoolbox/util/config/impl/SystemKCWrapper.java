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
package se.sics.ktoolbox.util.config.impl;

import com.google.common.base.Optional;
import java.util.Random;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.util.config.KConfigHelper;
import se.sics.ktoolbox.util.identifiable.BasicBuilders;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.basic.BasicAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SystemKCWrapper {

  public static class Names {

    public final static String PARALLEL_PORTS = "system.parallelPorts";
  }
  private final Config config;
  public final long seed;
  public final Identifier id;
  public final int port;
  public final Optional<Integer> parallelPorts;
  public final Optional<BasicAddress> aggregator;

  public SystemKCWrapper(Config config) {
    this.config = config;
    seed = KConfigHelper.read(config, SystemKConfig.seed);
    Optional<Integer> idOpt = SystemKConfig.id.readValue(config);
    int idVal = 0;
    if (idOpt.isPresent()) {
      idVal = idOpt.get();
    } else {
      Random rand = new Random(seed);
      idVal = rand.nextInt();
    }
    id = BasicIdentifiers.nodeId(new BasicBuilders.IntBuilder(idVal));
    port = KConfigHelper.read(config, SystemKConfig.port);
    aggregator = SystemKConfig.aggregator.readValue(config);
    parallelPorts = config.readValue(Names.PARALLEL_PORTS, Integer.class);
  }
}

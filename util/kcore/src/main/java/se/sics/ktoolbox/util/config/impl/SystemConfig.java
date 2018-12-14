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
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.util.config.KConfigHelper;
import se.sics.ktoolbox.util.config.options.BasicAddressOption;
import se.sics.ktoolbox.util.identifiable.BasicBuilders;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicAddress;
import se.sics.ktoolbox.util.trysf.Try;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SystemConfig {

  public static final String SYSTEM_SEED = "system.seed";
  public final static String SYSTEM_ID = "system.id";
  public final static String SYSTEM_PORT = "system.port";
  public final static String PARALLEL_PORTS = "system.parallelPorts";
  public static final BasicAddressOption SYSTEM_AGGREGATOR = new BasicAddressOption("system.aggregator");

  public final long seed;
  public final Identifier id;
  public final int port;
  public final Optional<Integer> parallelPorts;
  public final Optional<BasicAddress> aggregator;

  public SystemConfig(long seed, Identifier id, int port, Optional<Integer> parallelPorts,
    Optional<BasicAddress> aggregator) {
    this.seed = seed;
    this.id = id;
    this.port = port;
    this.parallelPorts = parallelPorts;
    this.aggregator = aggregator;
  }

  public static Try<SystemConfig> instance(Config config) {
    Optional<Long> seedOpt = config.readValue(SYSTEM_SEED, Long.class);
    long seed;
    if (!seedOpt.isPresent()) {
      seed = new Random().nextLong();
    } else {
      seed = seedOpt.get();
    }
    Random rand = new Random(seed);
    Optional<Integer> idOpt = config.readValue(SYSTEM_ID, Integer.class);
    int idVal;
    if (idOpt.isPresent()) {
      idVal = idOpt.get();
    } else {
      idVal = rand.nextInt();
    }
    IdentifierFactory idFactory = IdentifierRegistryV2.instance(BasicIdentifiers.Values.NODE, java.util.Optional.of(seed));
    Identifier id = idFactory.id(new BasicBuilders.IntBuilder(idVal));
    Optional<Integer> portOpt = config.readValue(SYSTEM_PORT, Integer.class);
    int port;
    if(portOpt.isPresent()) {
      port = portOpt.get();
    } else {
      port = rand.nextInt(65000);
    }
    Optional<BasicAddress> aggregatorAdr = SYSTEM_AGGREGATOR.readValue(config);
    Optional<Integer> parallelPorts = config.readValue(PARALLEL_PORTS, Integer.class);
    return new Try.Success(new SystemConfig(seed, id, port, parallelPorts, aggregatorAdr));
  }
}

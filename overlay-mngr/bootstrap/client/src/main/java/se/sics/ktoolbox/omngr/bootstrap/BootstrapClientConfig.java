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
package se.sics.ktoolbox.omngr.bootstrap;

import com.google.common.base.Optional;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.util.config.options.BasicAddressOption;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicAddress;
import se.sics.ktoolbox.util.trysf.Try;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BootstrapClientConfig {
  public static String BOOTSTRAP_SERVER = "overlays.bootstrap.server";
  public static String BOOTSTRAP_HEARTBEAT_POSITIONS = "overlays.bootstrap.heartbeat.positions";
  public static String BOOTSTRAP_HEARTBEAT_PERIOD = "overlays.bootstrap.heartbeat.period";
  
  public final int heartbeatPositions;
  public final long heartbeatPeriod;
  public final KAddress server;
  
  public BootstrapClientConfig(KAddress server, Integer heartbeatPositions, Long heartbeatPeriod) {
    this.server = server;
    this.heartbeatPositions = heartbeatPositions;
    this.heartbeatPeriod = heartbeatPeriod;
  }
  
  public static Try<BootstrapClientConfig> instance(Config config) {
    Optional<Integer> heartbeatPositions = config.readValue(BOOTSTRAP_HEARTBEAT_POSITIONS, Integer.class);
    if(!heartbeatPositions.isPresent()) {
      return new Try.Failure(new IllegalArgumentException("config - no value for " + BOOTSTRAP_HEARTBEAT_POSITIONS));
    }
    Optional<Long> heartbeatPeriod = config.readValue(BOOTSTRAP_HEARTBEAT_PERIOD, Long.class);
    if(!heartbeatPeriod.isPresent()) {
      return new Try.Failure(new IllegalArgumentException("config - no value for " + BOOTSTRAP_HEARTBEAT_PERIOD));
    }
    BasicAddressOption bootstrapServerOpt = new BasicAddressOption(BOOTSTRAP_SERVER);
    Optional<BasicAddress> server = bootstrapServerOpt.readValue(config);
    if(!server.isPresent()) {
      return new Try.Failure(new IllegalArgumentException("config - no value for " + BOOTSTRAP_SERVER));
    }
    return new Try.Success(new BootstrapClientConfig(server.get(), heartbeatPositions.get(), heartbeatPeriod.get()));
  }
}

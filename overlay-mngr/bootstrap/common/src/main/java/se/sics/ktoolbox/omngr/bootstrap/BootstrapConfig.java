package se.sics.ktoolbox.omngr.bootstrap;

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

import com.google.common.base.Optional;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.util.trysf.Try;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BootstrapConfig {
  public static final String HEARTBEAT_PERIOD = "overlays.bootstrap.heartbeat.period";
  public static final String BOOTSTRAP_SIZE = "overlays.bootstrap.size";
  
  public final long heartbeatPeriod;
  public final int bootstrapSize;
  public BootstrapConfig(long heartbeatPeriod, int bootstrapSize) {
    this.heartbeatPeriod = heartbeatPeriod;
    this.bootstrapSize = bootstrapSize;
  }
  
  public static Try<BootstrapConfig> instance(Config config) {
    Optional<Long> heartbeatPeriod = config.readValue(HEARTBEAT_PERIOD, Long.class);
    if(!heartbeatPeriod.isPresent()) {
      return new Try.Failure(new IllegalStateException("no config for:" + HEARTBEAT_PERIOD));
    }
    Optional<Integer> bootstrapSize = config.readValue(BOOTSTRAP_SIZE, Integer.class);
    if(!bootstrapSize.isPresent()) {
      return new Try.Failure(new IllegalStateException("no config for:" + BOOTSTRAP_SIZE));
    }
    return new Try.Success(new BootstrapConfig(heartbeatPeriod.get(), bootstrapSize.get()));
  }
}

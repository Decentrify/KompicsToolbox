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
import java.util.function.BiFunction;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.util.config.options.BasicAddressOption;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicAddress;
import se.sics.ktoolbox.util.trysf.Try;
import se.sics.ktoolbox.util.trysf.TryHelper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BootstrapClientConfig {

  public static String BOOTSTRAP_SERVER = "overlays.bootstrap.server";

  public final BootstrapConfig baseConfig;
  public final KAddress serverAdr;

  public BootstrapClientConfig(BootstrapConfig baseConfig, KAddress server) {
    this.baseConfig = baseConfig;
    this.serverAdr = server;
  }

  public static Try<BootstrapClientConfig> instance(Config config) {
    return BootstrapConfig.instance(config)
      .flatMap(clientConfig(config));
  }

  private static BiFunction<BootstrapConfig, Throwable, Try<BootstrapClientConfig>> clientConfig(Config config) {
    return TryHelper.tryFSucc1((baseConfig) -> {
      BasicAddressOption bootstrapServerOpt = new BasicAddressOption(BOOTSTRAP_SERVER);
      Optional<BasicAddress> server = bootstrapServerOpt.readValue(config);
      if (!server.isPresent()) {
        return new Try.Failure(new IllegalArgumentException("config - no value for " + BOOTSTRAP_SERVER));
      }
      return new Try.Success(new BootstrapClientConfig(baseConfig, server.get()));
    });
  }
}

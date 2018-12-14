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
package se.sics.ktoolbox.netmngr;

import com.google.common.base.Optional;
import java.util.EnumSet;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.netmngr.ipsolver.IpSolve;
import se.sics.ktoolbox.util.trysf.Try;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NetworkConfig {

  public static final String IP_TYPES = "network.ipType";

  public final EnumSet<IpSolve.NetworkInterfacesMask> ipTypes;

  public NetworkConfig(EnumSet<IpSolve.NetworkInterfacesMask> ipTypes) {
    this.ipTypes = ipTypes;
  }

  public static Try<NetworkConfig> instance(Config config) {
    Optional<String> sType = config.readValue(IP_TYPES, String.class);
    EnumSet<IpSolve.NetworkInterfacesMask> ipTypes;
    if (sType.isPresent()) {
      ipTypes = EnumSet.of(IpSolve.NetworkInterfacesMask.valueOf(sType.get()));
    } else {
      ipTypes = EnumSet.of(IpSolve.NetworkInterfacesMask.ALL);
    }
    return new Try.Success(new NetworkConfig(ipTypes));
  }
}

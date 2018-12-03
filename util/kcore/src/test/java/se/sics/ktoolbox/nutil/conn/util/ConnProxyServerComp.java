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
package se.sics.ktoolbox.nutil.conn.util;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.nutil.conn.ConnConfig;
import se.sics.ktoolbox.nutil.conn.ConnHelper;
import se.sics.ktoolbox.nutil.conn.ConnIds;
import se.sics.ktoolbox.nutil.conn.ConnProxy;
import se.sics.ktoolbox.nutil.conn.Connection;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ConnProxyServerComp extends ComponentDefinition {

  private Positive<Network> network = requires(Network.class);
  private Positive<Timer> timer = requires(Timer.class);

  private final Init init;

  private final ConnProxy.Server connMngr;
  private final ConnConfig connConfig;

  public ConnProxyServerComp(Init init) {
    this.init = init;
    connConfig = new ConnConfig(1000);
    connMngr = new ConnProxy.Server(init.selfAddress);
    subscribe(handleStart, control);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      connMngr.setup(proxy, logger);
      ConnHelper.SimpleConnCtrl serverCtrl = new ConnHelper.SimpleConnCtrl<>();
      ConnHelper.EmptyState initState = new ConnHelper.EmptyState();
      Connection.Server server = new Connection.Server<>(init.serverId, serverCtrl, connConfig, initState);
      connMngr.add(init.serverId, server);
    }
  };

  public static class Init extends se.sics.kompics.Init<ConnProxyServerComp> {

    public final KAddress selfAddress;
    public final ConnIds.InstanceId serverId;

    public Init(KAddress selfAddress, ConnIds.InstanceId serverId) {
      this.selfAddress = selfAddress;
      this.serverId = serverId;
    }
  }
}

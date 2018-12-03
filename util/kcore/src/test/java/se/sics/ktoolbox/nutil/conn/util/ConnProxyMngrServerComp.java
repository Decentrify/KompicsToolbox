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

import java.util.Optional;
import org.javatuples.Pair;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.nutil.conn.ConnConfig;
import se.sics.ktoolbox.nutil.conn.ConnCtrl;
import se.sics.ktoolbox.nutil.conn.ConnHelper;
import se.sics.ktoolbox.nutil.conn.ConnIds;
import se.sics.ktoolbox.nutil.conn.ConnMngrProxy;
import se.sics.ktoolbox.nutil.conn.ConnState;
import se.sics.ktoolbox.nutil.conn.ConnStatus;
import se.sics.ktoolbox.nutil.conn.Connection;
import se.sics.ktoolbox.nutil.conn.ServerListener;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ConnProxyMngrServerComp extends ComponentDefinition {

  private Positive<Network> network = requires(Network.class);
  private Positive<Timer> timer = requires(Timer.class);

  private final Init init;

  private final ConnMngrProxy connMngr;
  private final ConnConfig connConfig;

  public ConnProxyMngrServerComp(Init init) {
    this.init = init;
    connConfig = new ConnConfig(1000);
    connMngr = new ConnMngrProxy(init.selfAddress, serverListener(connConfig));
    subscribe(handleStart, control);
  }

  private ServerListener<ConnHelper.EmptyState> serverListener(ConnConfig connConfig) {
    return new ServerListener<ConnHelper.EmptyState>() {
      @Override
      public Pair<ConnStatus, Optional<Connection.Server>> connect(ConnIds.ConnId connId, ConnStatus peerStatus,
        KAddress peer, Optional<ConnHelper.EmptyState> peerState) {
        if (peerStatus.equals(ConnStatus.Base.CONNECT)) {
          ConnIds.InstanceId serverId = connId.serverId;
          ConnHelper.EmptyState initState = new ConnHelper.EmptyState();
          ConnCtrl connCtrl = new ConnHelper.SimpleConnCtrl<>();
          Connection.Server server = new Connection.Server<>(serverId, connCtrl, connConfig, initState);
          return Pair.with(ConnStatus.Base.CONNECTED, Optional.of(server));
        } else {
          return Pair.with(ConnStatus.Base.DISCONNECTED, Optional.empty());
        }
      }
    };
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      connMngr.setup(proxy, logger);
      if (init.serverId.isPresent()) {
        ConnHelper.SimpleConnCtrl serverCtrl = new ConnHelper.SimpleConnCtrl<>();
        ConnHelper.EmptyState initState = new ConnHelper.EmptyState();
        Connection.Server server = new Connection.Server<>(init.serverId.get(), serverCtrl, connConfig, initState);
        connMngr.addServer(init.serverId.get(), server);
      }
    }
  };

  public static class Init extends se.sics.kompics.Init<ConnProxyMngrServerComp> {

    public final KAddress selfAddress;
    public final Optional<ConnIds.InstanceId> serverId;

    public Init(KAddress selfAddress, Optional<ConnIds.InstanceId> serverId) {
      this.selfAddress = selfAddress;
      this.serverId = serverId;
    }
  }
}

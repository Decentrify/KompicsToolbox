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
package se.sics.ktoolbox.nutil.conn.base.util;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
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
import se.sics.ktoolbox.nutil.timer.TimerProxy;
import se.sics.ktoolbox.nutil.timer.TimerProxyImpl;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ConnProxyMngrServerComp extends ComponentDefinition {

  private Positive<Network> networkPort = requires(Network.class);
  private Positive<Timer> timerPort = requires(Timer.class);
  private TimerProxy timer;

  private final Init init;

  private final ConnMngrProxy connMngr;
  private final ConnConfig connConfig;

  private UUID periodicUpdate;
  private Set<ConnIds.InstanceId> servers = new HashSet<>();
  
  private final IdentifierFactory msgIds;

  public ConnProxyMngrServerComp(Init init) {
    this.init = init;
    timer = new TimerProxyImpl();
    connConfig = new ConnConfig(1000);
    connMngr = new ConnMngrProxy(init.selfAddress, serverListener(connConfig));
    msgIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.MSG, Optional.of(1234l));
    subscribe(handleStart, control);
  }

  private ServerListener<ConnState.Empty> serverListener(ConnConfig connConfig) {
    return new ServerListener<ConnState.Empty>() {
      @Override
      public Pair<ConnStatus.Decision, Optional<Connection.Server>> connect(ConnIds.ConnId connId,
        KAddress peer, ConnState.Empty peerState) {
        ConnIds.InstanceId serverId = connId.serverId;
        servers.add(serverId);
        ConnState.Empty initState = new ConnState.Empty();
        ConnCtrl connCtrl = new ConnHelper.SimpleConnCtrl<>();
        Connection.Server server = new Connection.Server<>(serverId, connCtrl, connConfig, initState);
        return Pair.with(ConnStatus.Decision.PROCEED, Optional.of(server));
      }
    };
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      timer.setup(proxy, logger);
      connMngr.setup(proxy, logger, msgIds);
      periodicUpdate = timer.schedulePeriodicTimer(connConfig.checkPeriod, connConfig.checkPeriod, update());
      if (init.serverId.isPresent()) {
        servers.add(init.serverId.get());
        ConnHelper.SimpleConnCtrl serverCtrl = new ConnHelper.SimpleConnCtrl<>();
        ConnState.Empty initState = new ConnState.Empty();
        Connection.Server server = new Connection.Server<>(init.serverId.get(), serverCtrl, connConfig, initState);
        connMngr.addServer(init.serverId.get(), server);
      }
    }
  };

  private Consumer<Boolean> update() {
    return (_ignore) -> {
      logger.trace("server update");
      servers.forEach((serverId) -> connMngr.updateServer(serverId, new ConnState.Empty()));
    };
  }

  public static class Init extends se.sics.kompics.Init<ConnProxyMngrServerComp> {

    public final KAddress selfAddress;
    public final Optional<ConnIds.InstanceId> serverId;

    public Init(KAddress selfAddress, Optional<ConnIds.InstanceId> serverId) {
      this.selfAddress = selfAddress;
      this.serverId = serverId;
    }
  }
}

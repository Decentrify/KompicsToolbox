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

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
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
import se.sics.ktoolbox.nutil.conn.ConnState;
import se.sics.ktoolbox.nutil.conn.Connection;
import se.sics.ktoolbox.nutil.timer.TimerProxy;
import se.sics.ktoolbox.nutil.timer.TimerProxyImpl;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ConnProxyServerComp extends ComponentDefinition {

  private Positive<Network> networkPort = requires(Network.class);
  private Positive<Timer> timerPort = requires(Timer.class);
  private TimerProxy timer;

  private final Init init;

  private final ConnProxy.Server connMngr;
  private final ConnConfig connConfig;
  
  private UUID periodicUpdate;
  
  private final IdentifierFactory msgIds;

  public ConnProxyServerComp(Init init) {
    this.init = init;
    timer = new TimerProxyImpl();
    connConfig = new ConnConfig(1000);
    connMngr = new ConnProxy.Server(init.selfAddress);
    msgIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.MSG, Optional.of(1234l));
    subscribe(handleStart, control);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      timer.setup(proxy, logger);
      connMngr.setup(proxy, logger, msgIds);
      ConnHelper.SimpleConnCtrl serverCtrl = new ConnHelper.SimpleConnCtrl<>();
      ConnState.Empty initState = new ConnState.Empty();
      Connection.Server server = new Connection.Server<>(init.serverId, serverCtrl, connConfig, initState);
      connMngr.startServer(init.serverId, server);
      periodicUpdate = timer.schedulePeriodicTimer(connConfig.checkPeriod, connConfig.checkPeriod, update());
    }
  };
  
  private Consumer<Boolean> update() {
    return (_ignore) -> {
      logger.trace("server update");
      connMngr.update(new ConnState.Empty());
    };
  }
      
  public static class Init extends se.sics.kompics.Init<ConnProxyServerComp> {

    public final KAddress selfAddress;
    public final ConnIds.InstanceId serverId;

    public Init(KAddress selfAddress, ConnIds.InstanceId serverId) {
      this.selfAddress = selfAddress;
      this.serverId = serverId;
    }
  }
}

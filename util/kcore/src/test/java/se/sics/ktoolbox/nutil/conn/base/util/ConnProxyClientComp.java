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
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.conn.ConnConfig;
import se.sics.ktoolbox.nutil.conn.ConnCtrl;
import se.sics.ktoolbox.nutil.conn.ConnIds;
import se.sics.ktoolbox.nutil.conn.ConnIds.InstanceId;
import se.sics.ktoolbox.nutil.conn.ConnProxy;
import se.sics.ktoolbox.nutil.conn.ConnState;
import se.sics.ktoolbox.nutil.conn.Connection;
import se.sics.ktoolbox.nutil.conn.util.TestConnHelper;
import se.sics.ktoolbox.nutil.timer.TimerProxy;
import se.sics.ktoolbox.nutil.timer.TimerProxyImpl;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ConnProxyClientComp extends ComponentDefinition {

  private Positive<Network> network = requires(Network.class);
  private Positive<Timer> timerPort = requires(Timer.class);
  private TimerProxy timer;

  private final ConnProxy.Client client;
  private final ConnConfig connConfig;
  private final Init init;
  private final IdentifierFactory msgIds;
  private final IdentifierFactory connBaseIds;
  
  private ConnIds.InstanceId clientId;
  private UUID periodicUpdate;

  public ConnProxyClientComp(Init init) {
    this.init = init;
    msgIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.MSG, Optional.of(1234l));
    connBaseIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.CONN_INSTANCE, Optional.of(1235l));
    connConfig = new ConnConfig(1000);
    client = new ConnProxy.Client(init.selfAddress);
    timer = new TimerProxyImpl();
    subscribe(handleStart, control);
  }
  
  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      timer.setup(proxy, logger);
      client.setup(proxy, logger);
      Identifier nodeId = init.selfAddress.getId();
      Identifier instanceId = connBaseIds.randomId();
      clientId = new ConnIds.InstanceId(init.overlayId, nodeId, init.batchId, instanceId, false);
      ConnCtrl clientCtrl = new TestConnHelper.AutoCloseClientCtrl<>(5);
      ConnState.Empty initState = new ConnState.Empty();
      Connection.Client cClient = new Connection.Client<>(clientId, clientCtrl, connConfig, msgIds, initState);
      client.set(clientId, cClient);
      timer.scheduleTimer(connConfig.checkPeriod, connect());
    }
  };

  private Consumer<Boolean> connect() {
    return (_ignore) -> {
      logger.trace("{}connect", init.batchId);
      client.connect(init.serverId, init.serverAddress);
      periodicUpdate = timer.schedulePeriodicTimer(connConfig.checkPeriod, connConfig.checkPeriod, update());
    };
  }
  
  private Consumer<Boolean> update() {
    return (_ignore) -> {
      logger.trace("client update");
      client.update(new ConnState.Empty());
    };
  }

  public static class Init extends se.sics.kompics.Init<ConnProxyClientComp> {

    public final Identifier overlayId;
    public final Identifier batchId;
    public final KAddress selfAddress;
    public final KAddress serverAddress;
    public final InstanceId serverId;

    public Init(Identifier overlayId, Identifier batchId, KAddress selfAddress,
      InstanceId serverId, KAddress serverAddress) {
      this.overlayId = overlayId;
      this.batchId = batchId;
      this.selfAddress = selfAddress;
      this.serverAddress = serverAddress;
      this.serverId = serverId;
    }
  }
}

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
import java.util.function.Consumer;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.conn.ConnConfig;
import se.sics.ktoolbox.nutil.conn.ConnHelper;
import se.sics.ktoolbox.nutil.conn.ConnIds.InstanceId;
import se.sics.ktoolbox.nutil.conn.ConnMngrProxy;
import se.sics.ktoolbox.nutil.timer.TimerProxy;
import se.sics.ktoolbox.nutil.timer.TimerProxyImpl;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ClientComp extends ComponentDefinition {

  private Positive<Network> network = requires(Network.class);
  private Positive<Timer> timerPort = requires(Timer.class);
  private TimerProxy timer;

  public final ConnMngrProxy connMngr;
  private final Init init;
  private InstanceId fullId;

  public ClientComp(Init init) {
    this.init = init;
    IdentifierFactory msgIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.MSG, Optional.of(123l));
    ConnConfig connConfig = new ConnConfig(1000);
    connMngr = new ConnMngrProxy(init.batchId, init.selfAddress, connConfig, msgIds);
    timer = new TimerProxyImpl().setup(proxy, logger);
    subscribe(handleStart, control);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      connMngr.setup(proxy, logger);
      fullId = connMngr.addClient(init.baseId, new ConnHelper.SimpleConnCtrl(), new ConnHelper.NoConnState());
      timer.scheduleTimer(1000, connect());
    }
  };
  
  private Consumer<Boolean> connect() {
    return (_ignore) -> {
      logger.trace("{}connect", init.batchId);
      connMngr.connectClient(fullId, init.serverId, init.serverAddress);
    };
  }

  public static class Init extends se.sics.kompics.Init<ClientComp> {

    public final Identifier batchId;
    public final Identifier baseId;
    public final KAddress selfAddress;

    public final KAddress serverAddress;
    public final InstanceId serverId;

    public Init(Identifier batchId, Identifier baseId, KAddress selfAddress,
      InstanceId serverId, KAddress serverAddress) {
      this.batchId = batchId;
      this.selfAddress = selfAddress;
      this.baseId = baseId;
      this.serverAddress = serverAddress;
      this.serverId = serverId;
    }
  }
}

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.javatuples.Pair;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.conn.ConnConfig;
import se.sics.ktoolbox.nutil.conn.ConnCtrl;
import se.sics.ktoolbox.nutil.conn.ConnHelper;
import se.sics.ktoolbox.nutil.conn.ConnIds;
import se.sics.ktoolbox.nutil.conn.ConnMngrProxy;
import se.sics.ktoolbox.nutil.conn.ConnStatus;
import se.sics.ktoolbox.nutil.conn.Connection;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BootstrapClientComp extends ComponentDefinition {

  //******************************CONNECTIONS*********************************
  private final Negative<BootstrapClientPort> bootstrapPort = provides(BootstrapClientPort.class);
  private final Positive<Network> networkPort = requires(Network.class);
  private final Positive<Timer> timerPort = requires(Timer.class);
  //*****************************EXTERNAL_STATE*******************************
  private final KAddress selfAdr;
  private final SystemKCWrapper systemConfig;
  private final BootstrapClientConfig clientConfig;
  private final ConnConfig connConfig;
  private final Identifier overlayBootstrapConnBatchId;
  private final Identifier overlayBootstrapConnBaseId;
  //*****************************INTERNAL_STATE*******************************
  //modified only by the ConnCtrl - read on request
  private final Map<Identifier, List<KAddress>> samples = new HashMap<>();
  private final ConnMngrProxy connMngr;
  private final IdentifierFactory msgIds;

  public BootstrapClientComp(Init init) {
    systemConfig = new SystemKCWrapper(config());
    clientConfig = BootstrapClientConfig.instance(config()).get();
    connConfig = new ConnConfig(clientConfig.baseConfig.heartbeatPeriod);
    selfAdr = init.selfAdr;
    overlayBootstrapConnBatchId = init.overlayBootstrapConnBatchId;
    overlayBootstrapConnBaseId = init.overlayBootstrapConnBaseId;
    
    loggingCtxPutAlways("nId", init.selfAdr.getId().toString());
    logger.info("initiating with seed:{}, bootstrap server:{}", systemConfig.seed, clientConfig.serverAdr);

    connMngr = new ConnMngrProxy(selfAdr, ConnHelper.noServerListener());
    msgIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.MSG, Optional.of(systemConfig.seed));

    subscribe(handleStart, control);
    subscribe(handleHeartbeatStart, bootstrapPort);
    subscribe(handleHeartbeatStop, bootstrapPort);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      connMngr.setup(proxy, logger);
    }
  };

  @Override
  public void tearDown() {
    connMngr.close();
  }

  Handler handleHeartbeatStart = new Handler<BootstrapClientEvent.Start>() {
    @Override
    public void handle(BootstrapClientEvent.Start req) {
      logger.info("heartbeat start:{}", req.overlay);
      createClient(req.overlay, periodicReBootstrap(req));
    }
  };
  
  Handler handleHeartbeatStop = new Handler<BootstrapClientEvent.Stop>() {
    @Override
    public void handle(BootstrapClientEvent.Stop req) {
      logger.info("heartbeat stop:{}", req.overlay);
      ConnIds.InstanceId clientId = clientId(req.overlay);
      connMngr.closeClient(clientId);
    }
  };

  private Consumer<List<KAddress>> periodicReBootstrap(BootstrapClientEvent.Start sampleReq) {
    return (sample) -> {
      answer(sampleReq, sampleReq.sample(sample));
    };
  }

  private ConnCtrl connCtrl(Consumer<List<KAddress>> periodicReBootstrap) {
    return new ConnCtrl<BootstrapState.Init, BootstrapState.Sample>() {
      @Override
      public Map<ConnIds.ConnId, ConnStatus> selfUpdate(ConnIds.InstanceId instanceId,
        BootstrapState.Init selfSate) {
        //no necessary updates on the connections
        return new HashMap<>();
      }

      @Override
      public Pair<ConnIds.ConnId, ConnStatus> partnerUpdate(ConnIds.ConnId connId, BootstrapState.Init selfState,
        ConnStatus peerStatus, KAddress peer, Optional<BootstrapState.Sample> peerState) {
        if (peerState.isPresent()) {
          samples.put(connId.clientId.overlayId, peerState.get().sample);
          periodicReBootstrap.accept(peerState.get().sample);
        }
        if (peerStatus.equals(ConnStatus.Base.DISCONNECTED)) {
          logger.warn("{} disconnected", connId);
          //TODO - Alex reconnect logic?
        }
        return Pair.with(connId, peerStatus);
      }

      @Override
      public void close(ConnIds.ConnId connId) {
        samples.remove(connId.clientId.overlayId);
      }
    };
  }

  private void createClient(Identifier overlayId, Consumer<List<KAddress>> periodicReBootstrap) {
    ConnIds.InstanceId clientId = clientId(overlayId);
    ConnIds.InstanceId serverId = new ConnIds.InstanceId(overlayId, clientConfig.serverAdr.getId(),
      overlayBootstrapConnBatchId, overlayBootstrapConnBaseId, true);

    BootstrapState.Init initState = new BootstrapState.Init();
    Connection.Client client = new Connection.Client<>(clientId, connCtrl(periodicReBootstrap), connConfig, msgIds, 
      initState);
    connMngr.addClient(clientId, client);
    connMngr.connectClient(clientId, serverId, clientConfig.serverAdr);
  }

  private ConnIds.InstanceId clientId(Identifier overlayId) {
    return new ConnIds.InstanceId(overlayId, selfAdr.getId(),
      overlayBootstrapConnBatchId, overlayBootstrapConnBaseId, false);
  }

  //**************************************************************************
  public static class Init extends se.sics.kompics.Init<BootstrapClientComp> {

    public final Identifier overlayBootstrapConnBatchId;
    public final Identifier overlayBootstrapConnBaseId;
    public final KAddress selfAdr;

    public Init(KAddress selfAdr, Identifier overlayBootstrapConnBatchId, Identifier overlayBootstrapConnBaseId) {
      this.selfAdr = selfAdr;
      this.overlayBootstrapConnBatchId = overlayBootstrapConnBatchId;
      this.overlayBootstrapConnBaseId = overlayBootstrapConnBaseId;
    }
  }
}

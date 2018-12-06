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

import com.google.common.collect.HashBasedTable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.javatuples.Pair;
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
import se.sics.ktoolbox.nutil.conn.ConnMngrProxy;
import se.sics.ktoolbox.nutil.conn.ConnStatus;
import se.sics.ktoolbox.nutil.conn.Connection;
import se.sics.ktoolbox.nutil.conn.ServerListener;
import se.sics.ktoolbox.nutil.timer.TimerProxy;
import se.sics.ktoolbox.nutil.timer.TimerProxyImpl;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.trysf.Try;
import se.sics.ktoolbox.util.trysf.TryHelper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BootstrapServerComp extends ComponentDefinition {

  //******************************CONNECTIONS*********************************
  private final Positive<Network> networkPort = requires(Network.class);
  private final Positive<Timer> timerPort = requires(Timer.class);
  //*****************************EXTERNAL_STATE*******************************
  private final KAddress selfAdr;
  //*****************************INTERNAL_STATE*******************************
  private BootstrapConfig config;
  private ConnConfig connConfig;
  private final Identifier overlayBootstrapConnBatchId;
  private final Identifier overlayBootstrapConnBaseId;
  private final Random rand;
  //
  private ConnMngrProxy connMngr;
  //<overlay,pos,adr>
  private HashBasedTable<Identifier, Integer, KAddress> samples = HashBasedTable.create();
  private UUID rebootstrapTId;

  public BootstrapServerComp(Init init) {
    selfAdr = init.selfAdr;
    loggingCtxPutAlways("nId", init.selfAdr.getId().toString());

    readConfig();
    overlayBootstrapConnBatchId = init.overlayBootstrapConnBatchId;
    overlayBootstrapConnBaseId = init.overlayBootstrapConnBaseId;

    rand = new Random(1234l);

    connMngr = new ConnMngrProxy(selfAdr, serverListener());

    subscribe(handleStart, control);
  }

  private void readConfig() {
    Try<BootstrapConfig> bootstrapConfig = BootstrapConfig.instance(config());
    if (bootstrapConfig.isFailure()) {
      throw new RuntimeException(TryHelper.tryError(bootstrapConfig));
    } else {
      config = bootstrapConfig.get();
    }
    connConfig = new ConnConfig(bootstrapConfig.get().heartbeatPeriod);
  }

  private ServerListener serverListener() {
    return new ServerListener<BootstrapState.Init>() {
      @Override
      public Pair<ConnStatus, Optional<Connection.Server>> connect(ConnIds.ConnId connId, ConnStatus peerStatus,
        KAddress peer, Optional<BootstrapState.Init> peerState) {
        ConnCtrl ctrl = connCtrl();
        BootstrapState.Sample initState = new BootstrapState.Sample(new LinkedList<>());
        ConnIds.InstanceId serverId = new ConnIds.InstanceId(connId.serverId.overlayId, selfAdr.getId(),
          overlayBootstrapConnBatchId, overlayBootstrapConnBaseId, true);
        if (serverId.equals(connId.serverId)) {
          Connection.Server server = new Connection.Server(serverId, connCtrl(), connConfig, initState);
          connMngr.addServer(serverId, server);
          return Pair.with(ConnStatus.Base.CONNECTED, Optional.of(server));
        } else {
          logger.warn("bad server adr expected:{}, found:{}", serverId, connId.serverId);
          return Pair.with(ConnStatus.Base.DISCONNECTED, Optional.empty());
        }
      }
    };
  }

  private ConnCtrl<BootstrapState.Sample, BootstrapState.Init> connCtrl() {
    return new ConnCtrl<BootstrapState.Sample, BootstrapState.Init>() {
      @Override
      public Map<ConnIds.ConnId, ConnStatus> selfUpdate(ConnIds.InstanceId serverId, BootstrapState.Sample state) {
        //nothing
        //we do not partnerUpdate any of the connection states;
        return new HashMap<>();
      }

      @Override
      public Pair<ConnIds.ConnId, ConnStatus> partnerUpdate(ConnIds.ConnId connId, BootstrapState.Sample selfState,
        ConnStatus peerStatus, KAddress peer, Optional<BootstrapState.Init> peerState) {
        Identifier overlayId = connId.serverId.overlayId;
        int pos = rand.nextInt(config.bootstrapSize);
        if (ConnStatus.Base.CONNECT.equals(peerStatus)) {
          samples.put(overlayId, pos, peer);
          connMngr.updateServer(connId.serverId, new BootstrapState.Sample(sampleWithoutDuplicates(overlayId)));
          return Pair.with(connId, ConnStatus.Base.CONNECTED);
        } else if (ConnStatus.Base.HEARTBEAT.equals(peerStatus)) {
          samples.put(overlayId, pos, peer);
          connMngr.updateServer(connId.serverId, new BootstrapState.Sample(sampleWithoutDuplicates(overlayId)));
          return Pair.with(connId, ConnStatus.Base.HEARTBEAT_ACK);
        } else if (ConnStatus.Base.DISCONNECT.equals(peerStatus)) {
          return Pair.with(connId, ConnStatus.Base.DISCONNECTED);
        } else {
          throw new RuntimeException("unknown:" + peerStatus);
        }
      }

      @Override
      public void close(ConnIds.ConnId connId) {
        //nothing - live nodes will repopulate the sample
      }
    };
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

  private List<KAddress> sampleWithoutDuplicates(Identifier overlayId) {
    Set<Identifier> ids = new HashSet<>();
    List<KAddress> adrs = new ArrayList<>();
    for (KAddress adr : samples.row(overlayId).values()) {
      if (ids.contains(adr.getId())) {
        continue;
      }
      adrs.add(adr);
      ids.add(adr.getId());
    }
    return adrs;
  }

  public static class Init extends se.sics.kompics.Init<BootstrapServerComp> {

    public final KAddress selfAdr;
    public final Identifier overlayBootstrapConnBatchId;
    public final Identifier overlayBootstrapConnBaseId;

    public Init(KAddress selfAdr, Identifier overlayBootstrapConnBatchId, Identifier overlayBootstrapConnBaseId) {
      this.selfAdr = selfAdr;
      this.overlayBootstrapConnBatchId = overlayBootstrapConnBatchId;
      this.overlayBootstrapConnBaseId = overlayBootstrapConnBaseId;
    }
  }
}

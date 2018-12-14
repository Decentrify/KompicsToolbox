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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
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

  private final IdentifierFactory msgIds;
  public BootstrapServerComp(Init init) {
    selfAdr = init.selfAdr;
    loggingCtxPutAlways("nId", init.selfAdr.getId().toString());

    readConfig();
    overlayBootstrapConnBatchId = init.overlayBootstrapConnBatchId;
    overlayBootstrapConnBaseId = init.overlayBootstrapConnBaseId;

    rand = new Random(1234l);
    msgIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.MSG, Optional.of(1234l));
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
      public Pair<ConnStatus.Decision, Optional<Connection.Server>> connect(ConnIds.ConnId connId,
        KAddress peer, BootstrapState.Init peerState) {
        ConnCtrl ctrl = connCtrl();
        BootstrapState.Sample initState = new BootstrapState.Sample(new LinkedList<>());
        ConnIds.InstanceId serverId = new ConnIds.InstanceId(connId.serverId.overlayId, selfAdr.getId(),
          overlayBootstrapConnBatchId, overlayBootstrapConnBaseId, true);
        if (serverId.equals(connId.serverId)) {
          Connection.Server server = new Connection.Server(serverId, connCtrl(), connConfig, initState);
          connMngr.addServer(serverId, server);
          return Pair.with(ConnStatus.Decision.PROCEED, Optional.of(server));
        } else {
          logger.warn("bad server adr expected:{}, found:{}", serverId, connId.serverId);
          return Pair.with(ConnStatus.Decision.PROCEED, Optional.empty());
        }
      }
    };
  }

  private ConnCtrl<BootstrapState.Sample, BootstrapState.Init> connCtrl() {
    return new ConnCtrl<BootstrapState.Sample, BootstrapState.Init>() {
      @Override
      public ConnStatus.Decision connect(ConnIds.ConnId connId, KAddress partnerAdr, BootstrapState.Sample selfState,
        Optional<BootstrapState.Init> partnerState) {
        int pos = rand.nextInt(config.bootstrapSize);
        Identifier overlayId = connId.serverId.overlayId;
        samples.put(overlayId, pos, partnerAdr);
        connMngr.updateServer(connId.serverId, new BootstrapState.Sample(sampleWithoutDuplicates(overlayId)));
        return ConnStatus.Decision.PROCEED;
      }

      @Override
      public ConnStatus.Decision connected(ConnIds.ConnId connId, KAddress partnerAdr, BootstrapState.Sample selfState,
        BootstrapState.Init partnerState) {
        return ConnStatus.Decision.PROCEED;
      }

      @Override
      public ConnStatus.Decision selfUpdate(ConnIds.ConnId connId, KAddress partnerAdr, BootstrapState.Sample selfState,
        BootstrapState.Init partnerState) {
        return ConnStatus.Decision.PROCEED;
      }

      @Override
      public ConnStatus.Decision partnerUpdate(ConnIds.ConnId connId, KAddress partnerAdr, BootstrapState.Sample selfState,
        BootstrapState.Init partnerState) {
        int pos = rand.nextInt(config.bootstrapSize);
        Identifier overlayId = connId.serverId.overlayId;
        samples.put(overlayId, pos, partnerAdr);
        connMngr.updateServer(connId.serverId, new BootstrapState.Sample(sampleWithoutDuplicates(overlayId)));
        return ConnStatus.Decision.PROCEED;
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
      connMngr.setup(proxy, logger, msgIds);
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

/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.nutil.conn.workers;

import se.sics.ktoolbox.nutil.conn.workers.WorkCtrl;
import se.sics.ktoolbox.nutil.conn.workers.MngrState;
import se.sics.ktoolbox.nutil.conn.workers.WorkState;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.javatuples.Pair;
import org.slf4j.Logger;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.conn.ConnConfig;
import se.sics.ktoolbox.nutil.conn.ConnCtrl;
import se.sics.ktoolbox.nutil.conn.ConnIds;
import se.sics.ktoolbox.nutil.conn.ConnProxy;
import se.sics.ktoolbox.nutil.conn.ConnState;
import se.sics.ktoolbox.nutil.conn.ConnStatus;
import se.sics.ktoolbox.nutil.conn.Connection;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class WorkCenter extends ComponentDefinition {

  private final Positive<Network> network = requires(Network.class);
  private final Positive<Timer> timerPort = requires(Timer.class);

  private final KAddress selfAdr;
  private final Identifier overlayId;
  private final Identifier ctrlBatchId;
  private final Identifier workBatchId;
  private final Identifier baseId;

  private final ConnProxy.Client ctrlClient;
  private final ConnProxy.Server workServer;

  private ConnCtrl mngrCtrl;
  private ConnCtrl workCtrl;

  private final WorkCtrl.Server<WorkState.Client> workServerC;
  private ConnConfig connConfig;

  public WorkCenter(KAddress selfAdr, Identifier overlayId, Identifier ctrlBatchId, Identifier workBatchId,
    Identifier baseId,
    WorkCtrl.Server workServerC) {
    this.selfAdr = selfAdr;
    this.overlayId = overlayId;
    this.ctrlBatchId = ctrlBatchId;
    this.workBatchId = workBatchId;
    this.baseId = baseId;
    ctrlClient = new ConnProxy.Client(selfAdr);
    workServer = new ConnProxy.Server(selfAdr);
    this.workServerC = workServerC;
  }

  public void setup(ComponentProxy proxy, Logger logger, ConnConfig connConfig, IdentifierFactory msgIds,
    MngrState.Client clientInitState, WorkState.Server serverInitState) {
    this.connConfig = connConfig;
    ctrlClient.setup(proxy, logger);
    workServer.setup(proxy, logger);
    ConnIds.InstanceId ctrlClientId = new ConnIds.InstanceId(overlayId, selfAdr.getId(), ctrlBatchId, baseId, false);
    ConnIds.InstanceId workServerId = new ConnIds.InstanceId(overlayId, selfAdr.getId(), workBatchId, baseId, true);
    mngrCtrl = clientCtrl();
    workCtrl = serverCtrl();
    ctrlClient.set(ctrlClientId, ctrlClient(ctrlClientId, msgIds, clientInitState));
    workServer.add(workServerId, workServer(workServerId, serverInitState));
  }

  public void connect(KAddress ctrlCenterAdr) {
    ConnIds.InstanceId ctrlServerId
      = new ConnIds.InstanceId(overlayId, ctrlCenterAdr.getId(), ctrlBatchId, baseId, true);
    ctrlClient.connect(ctrlServerId, ctrlCenterAdr);
  }

  public void close() {
    workServer.close();
    ctrlClient.close();
  }

  private Connection.Client<ConnState.Empty, MngrState.Client> ctrlClient(ConnIds.InstanceId ctrlClientId,
    IdentifierFactory msgIds, MngrState.Client initState) {
    return new Connection.Client<>(ctrlClientId, mngrCtrl, connConfig, msgIds, initState);
  }

  private ConnCtrl<MngrState.Client, ConnState.Empty> clientCtrl() {
    return new ConnCtrl<MngrState.Client, ConnState.Empty>() {
      @Override
      public Map<ConnIds.ConnId, ConnStatus> selfUpdate(ConnIds.InstanceId instanceId, MngrState.Client selfSate) {
        //nothing
        return new HashMap<>();
      }

      @Override
      public ConnStatus partnerUpdate(ConnIds.ConnId connId, MngrState.Client selfState,
        ConnStatus peerStatus, KAddress peer, ConnState.Empty peerState) {
        if (ConnStatus.Base.CONNECTED.equals(peerStatus)) {
          return ConnStatus.Base.CONNECTED_ACK;
        } else if (ConnStatus.Base.CONNECTED_ACK.equals(peerStatus)) {
          return peerStatus;
        } else if (ConnStatus.Base.HEARTBEAT_ACK.equals(peerStatus)) {
          return peerStatus;
        } else if (ConnStatus.Base.DISCONNECT.equals(peerStatus)) {
          return peerStatus;
        } else {
          throw new RuntimeException("unknown:" + peerStatus);
        }
      }

      @Override
      public void close(ConnIds.ConnId connId) {
        //nothing
      }
    };
  }

  private Connection.Server<WorkState.Server, WorkState.Client> workServer(ConnIds.InstanceId workServerId,
    WorkState.Server initState) {
    return new Connection.Server<>(workServerId, workCtrl, connConfig, initState);
  }

  private ConnCtrl<WorkState.Server, WorkState.Client> serverCtrl() {
    return new ConnCtrl<WorkState.Server, WorkState.Client>() {
      @Override
      public Map<ConnIds.ConnId, ConnStatus> selfUpdate(ConnIds.InstanceId instanceId, WorkState.Server selfSate) {
        //nothing urgent - allow periodic update to send state
        return new HashMap<>();
      }

      @Override
      public ConnStatus partnerUpdate(ConnIds.ConnId connId, WorkState.Server selfState,
        ConnStatus peerStatus, KAddress peer, WorkState.Client peerState) {
        if (ConnStatus.Base.CONNECT.equals(peerStatus)) {
          return ConnStatus.Base.CONNECTED;
        } else if (ConnStatus.Base.CONNECTED_ACK.equals(peerStatus)) {
          workServerC.connected(connId, peer, peerState);
          return peerStatus;
        } else if (ConnStatus.Base.HEARTBEAT.equals(peerStatus)) {
          return ConnStatus.Base.HEARTBEAT_ACK;
        } else if (ConnStatus.Base.DISCONNECT.equals(peerStatus)) {
          return ConnStatus.Base.DISCONNECTED;
        } else {
          throw new RuntimeException("unknown:" + peerStatus);
        }
      }

      @Override

      public void close(ConnIds.ConnId connId) {
        //nothing
      }
    };

  }
}

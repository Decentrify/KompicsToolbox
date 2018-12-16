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

import java.util.Optional;
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
import se.sics.ktoolbox.nutil.conn.ConnStatus;
import se.sics.ktoolbox.nutil.conn.Connection;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class WorkCtrlCenterProxy extends ComponentDefinition {

  private final Positive<Network> network = requires(Network.class);
  private final Positive<Timer> timerPort = requires(Timer.class);

  private final KAddress selfAdr;
  private final Identifier overlayId;
  private final Identifier batchId;
  private final Identifier baseId;

  private final ConnProxy.Client workerConn;
  private final WorkCtrlProxy workerCtrl;

  private IdentifierFactory msgIds;
  private ConnConfig connConfig;

  public WorkCtrlCenterProxy(KAddress selfAdr, Identifier overlayId, Identifier batchId, Identifier baseId) {
    this.selfAdr = selfAdr;
    this.overlayId = overlayId;
    this.batchId = batchId;
    this.baseId = baseId;
    workerConn = new ConnProxy.Client(selfAdr);
    workerCtrl = new WorkCtrlProxy(selfAdr);
  }

  public void setup(ComponentProxy proxy, Logger logger, ConnConfig connConfig,
    IdentifierFactory msgIds, IdentifierFactory eventIds) {
    this.msgIds = msgIds;
    this.connConfig = connConfig;
    workerConn.setup(proxy, logger);
    workerCtrl.setup(proxy, logger, msgIds, eventIds);
  }

  public void startClient(WorkCtrlState clientInitState, KAddress ctrlCenterAdr) {
    ConnIds.InstanceId clientId = new ConnIds.InstanceId(overlayId, selfAdr.getId(), batchId, baseId, false);
    ConnIds.InstanceId serverId = new ConnIds.InstanceId(overlayId, selfAdr.getId(), batchId, baseId, true);
    ConnCtrl ctrl = ctrl();
    Connection.Client clientAux = new Connection.Client<>(clientId, ctrl, connConfig, msgIds, clientInitState);
    ConnIds.InstanceId ctrlServerId
      = new ConnIds.InstanceId(overlayId, ctrlCenterAdr.getId(), batchId, baseId, true);
    workerConn.startClient(clientId, clientAux, ctrlServerId, ctrlCenterAdr);
  }

  public void close() {
    workerConn.close();
    workerCtrl.close();
  }

  private ConnCtrl<WorkCtrlState, WorkMngrState> ctrl() {
    return new ConnCtrl<WorkCtrlState, WorkMngrState>() {

      @Override
      public ConnStatus.Decision connect(ConnIds.ConnId connId, KAddress partnerAdr, WorkCtrlState selfState,
        Optional<WorkMngrState> partnerState) {
        return ConnStatus.Decision.PROCEED;
      }

      @Override
      public ConnStatus.Decision connected(ConnIds.ConnId connId, KAddress partnerAdr, WorkCtrlState selfState, 
        WorkMngrState partnerState) {
        return ConnStatus.Decision.PROCEED;
      }

      @Override
      public ConnStatus.Decision selfUpdate(ConnIds.ConnId connId, KAddress partnerAdr, WorkCtrlState selfState, 
        WorkMngrState partnerState) {
        return ConnStatus.Decision.PROCEED;
      }

      @Override
      public ConnStatus.Decision partnerUpdate(ConnIds.ConnId connId, KAddress partnerAdr, WorkCtrlState selfState, 
        WorkMngrState partnerState) {
        return ConnStatus.Decision.PROCEED;
      }

      @Override
      public void close(ConnIds.ConnId connId) {
        //nothing
      }
    };
  }
}

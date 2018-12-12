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
import se.sics.kompics.ComponentProxy;
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
public class MngrCenter {

  private final KAddress selfAdr;
  private final Identifier overlayId;
  private final Identifier ctrlBatchId;
  private final Identifier workBatchId;
  private final Identifier baseId;

  private final ConnProxy.Server mngrServer;
  private final ConnProxy.Client workClient;
  private ConnCtrl mngrCtrl;
  private WorkConnCtrl workCtrl;

  private final MngrCtrl.Server<MngrState.Client> mngrServerC;
  private IdentifierFactory msgIds;
  private ConnConfig connConfig;

  public MngrCenter(KAddress selfAdr, Identifier overlayId, Identifier mngrBatchId, Identifier workBatchId,
    Identifier baseId, MngrCtrl.Server<MngrState.Client> mngrServerC) {
    this.selfAdr = selfAdr;
    this.overlayId = overlayId;
    this.ctrlBatchId = mngrBatchId;
    this.workBatchId = workBatchId;
    this.baseId = baseId;
    mngrServer = new ConnProxy.Server(selfAdr);
    workClient = new ConnProxy.Client(selfAdr);
    this.mngrServerC = mngrServerC;
  }

  public void setup(ComponentProxy proxy, Logger logger, ConnConfig connConfig, IdentifierFactory msgIds,
    MngrState.Server serverInitState) {
    this.msgIds = msgIds;
    this.connConfig = connConfig;
    mngrServer.setup(proxy, logger, msgIds);
    workClient.setup(proxy, logger);
    ConnIds.InstanceId ctrlServerId = new ConnIds.InstanceId(overlayId, selfAdr.getId(), ctrlBatchId, baseId, true);
    mngrCtrl = mngrCtrl();
    workCtrl = workCtrl();
    mngrServer.add(ctrlServerId, ctrlServer(ctrlServerId, serverInitState));
    mngrServerC.setWorkConnCtrl(workCtrl);
  }

  private Connection.Server<MngrState.Server, MngrState.Client> ctrlServer(ConnIds.InstanceId ctrlServerId,
    MngrState.Server initState) {
    return new Connection.Server<>(ctrlServerId, mngrCtrl, connConfig, initState);
  }

  private ConnCtrl<MngrState.Server, MngrState.Client> mngrCtrl() {
    return new ConnCtrl<MngrState.Server, MngrState.Client>() {

      @Override
      public ConnStatus.Decision connect(ConnIds.ConnId connId, KAddress partnerAdr, MngrState.Server selfState,
        Optional<MngrState.Client> partnerState) {
        mngrServerC.connect(connId, partnerAdr);
        return ConnStatus.Decision.PROCEED;
      }

      @Override
      public ConnStatus.Decision connected(ConnIds.ConnId connId, MngrState.Server selfState,
        MngrState.Client partnerState) {
        mngrServerC.connected(connId);
        return ConnStatus.Decision.PROCEED;
      }

      @Override
      public ConnStatus.Decision selfUpdate(ConnIds.ConnId connId, MngrState.Server selfState,
        MngrState.Client partnerState) {
        return ConnStatus.Decision.PROCEED;
      }

      @Override
      public ConnStatus.Decision serverUpdate(ConnIds.ConnId connId, MngrState.Server selfState,
        MngrState.Client partnerState) {
        return ConnStatus.Decision.PROCEED;
      }
      
      @Override
      public void close(ConnIds.ConnId connId) {
        //nothing
      }
    };
  }

  private void connectWorkClient(ConnIds.InstanceId workClientId, ConnIds.InstanceId workServerId,
    KAddress workServerAdr) {
    workClient.set(workClientId, workClient(workClientId));
    workClient.connect(workServerId, workServerAdr);
  }

  private Connection.Client<WorkState.Server, WorkState.Client> workClient(ConnIds.InstanceId workClientId) {
    WorkState.Client initState = new WorkState.Client() {
    };
    return new Connection.Client<>(workClientId, workCtrl, connConfig, msgIds, initState);
  }

  private WorkConnCtrl<WorkState.Client, WorkState.Server> workCtrl() {
    return new WorkConnCtrl<>();
  }

  class WorkConnCtrl<C extends WorkState.Client, S extends WorkState.Server> implements ConnCtrl<C, S> {
    //protocol
    @Override
    public ConnStatus.Decision connect(ConnIds.ConnId connId, KAddress partnerAdr, C selfState, Optional<S> partnerState) {
      return ConnStatus.Decision.PROCEED;
    }

    @Override
    public ConnStatus.Decision connected(ConnIds.ConnId connId, C selfState, S partnerState) {
      return ConnStatus.Decision.PROCEED;
    }

    @Override
    public ConnStatus.Decision selfUpdate(ConnIds.ConnId connId, C selfState, S partnerState) {
      return ConnStatus.Decision.PROCEED;
    }

    @Override
    public ConnStatus.Decision serverUpdate(ConnIds.ConnId connId, C selfState, S partnerState) {
      return ConnStatus.Decision.PROCEED;
    }
    
    @Override
    public void close(ConnIds.ConnId connId) {
      //nothing
    }
    
    //outside control
    public ConnIds.ConnId connectClient(Identifier clientBaseId, WorkState.Client clientState, KAddress workServerAdr) {
      ConnIds.InstanceId workClientId = new ConnIds.InstanceId(overlayId, selfAdr.getId(), workBatchId, baseId, false);
      ConnIds.InstanceId workServerId = new ConnIds.InstanceId(overlayId, workServerAdr.getId(), workBatchId, baseId,
        true);
      connectWorkClient(workClientId, workServerId, workServerAdr);
      return new ConnIds.ConnId(workServerId, workClientId);
    }
  }
}

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
package se.sics.ktoolbox.nutil.conn;

import com.google.common.collect.HashBasedTable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.conn.ConnIds.ConnId;
import se.sics.ktoolbox.nutil.conn.ConnIds.InstanceId;
import se.sics.ktoolbox.nutil.timer.TimerProxy;
import se.sics.ktoolbox.util.TupleHelper;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class Connection {

  public static class Client<S extends ConnState, C extends ConnState> {

    public final InstanceId clientId;
    private final ConnCtrl<C, S> ctrl;
    private final ConnConfig config;
    private final IdentifierFactory msgIds;

    private TupleHelper.PairConsumer<KAddress, ConnMsgs.Base> networkSend;
    private TimerProxy timer;
    private Logger logger;

    private UUID periodicCheck;

    private final Map<ConnId, ServerState> servers = new HashMap<>();

    private C state;
    private ConnStatus.System systemStatus;

    

    public Client(InstanceId clientId, ConnCtrl<C, S> ctrl, ConnConfig config, IdentifierFactory msgIds, C state) {
      this.clientId = clientId;
      this.ctrl = ctrl;
      this.config = config;
      this.msgIds = msgIds;
      this.state = state;
    }

    public Client setup(TimerProxy timer, TupleHelper.PairConsumer<KAddress, ConnMsgs.Base> networkSend,
      Logger logger) {
      this.timer = timer;
      this.networkSend = networkSend;
      this.logger = logger;
      periodicCheck = timer.schedulePeriodicTimer(config.checkPeriod, config.checkPeriod, periodicCheck());
      return this;
    }

    public void close() {
      servers.entrySet().forEach((server) -> {
        ConnId connId = server.getKey();
        KAddress serverAddress = server.getValue().address;
        ctrl.close(connId);
        ConnMsgs.Client content = ConnMsgs.clientDisconnect(msgIds.randomId(), connId);
        networkSend.accept(serverAddress, content);
      });
      if (periodicCheck != null) {
        timer.cancelPeriodicTimer(periodicCheck);
        periodicCheck = null;
      }
    }

    public void connect(InstanceId serverId, KAddress server) {
      ConnId connId = new ConnId(serverId, clientId);
      ConnMsgs.Client msg = ConnMsgs.clientConnect(msgIds.randomId(), connId, state);
      networkSend.accept(server, msg);
    }

    public void update(C state) {
      this.state = state;
      ctrl.selfUpdate(clientId, state).entrySet().forEach((server) -> {
        ConnId connId = server.getKey();
        ConnStatus decidedStatus = server.getValue();
        ServerState serverState = servers.get(server.getKey());
        if (serverState == null) {
          throw new RuntimeException("weird");
        }
        logger.debug("{} up:{}", connId, decidedStatus);
        processUpdateDecision(connId, decidedStatus, serverState.address);
      });
    }

    private Consumer<Boolean> periodicCheck() {
      return (_ignore) -> {
        Iterator<Map.Entry<ConnId, ServerState>> it = servers.entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry<ConnId, ServerState> aux = it.next();
          ConnId connId = aux.getKey();
          ServerState serverState = aux.getValue();
          serverState.period();
          if (serverState.isDead()) {
            ctrl.close(connId);
            it.remove();
          } else {
            ConnMsgs.Client content = ConnMsgs.clientHeartbeat(msgIds.randomId(), connId);
            networkSend.accept(serverState.address, content);
          }
        }
      };
    }

    public void handleContent(KAddress serverAddress, ConnMsgs.Server<S> content) {
      ConnIds.ConnId connId = content.connId;
      ConnStatus status = content.status;
      Identifier msgId = content.msgId;
      logger.debug("{} partner:{}", connId, status);
      if (ConnStatus.Base.CONNECTED.equals(status)) {
        ConnStatus decidedStatus = ctrl.partnerUpdate(connId, state, status, serverAddress, content.state.get());
        logger.debug("{} self:{}", connId, decidedStatus);
        processConnectedDecision(connId, decidedStatus, msgId, serverAddress, content.state.get());
      } else {
        ServerState serverState = servers.get(connId);
        if (serverState == null) {
          if (!ConnStatus.Base.DISCONNECTED.equals(status)) {
            logger.debug("{} self:{}", connId, ConnStatus.Base.DISCONNECT);
            disconnectMsg(connId, connId, serverAddress);
          }
          return;
        }
        if (ConnStatus.Base.HEARTBEAT_ACK.equals(status)) {
          serverState.heartbeat();
        } else if (ConnStatus.Base.DISCONNECTED.equals(status)) {
          disconnectCleanup(connId);
        } else if (ConnStatus.Base.SERVER_STATE.equals(status)) {
          S newServerState = content.state.get();
          ConnStatus decidedStatus = ctrl.partnerUpdate(connId, state, status, serverAddress, newServerState);
          logger.debug("{} self:{}", connId, decidedStatus);
          processStateDecision(connId, decidedStatus, msgId, serverAddress, newServerState);
        }
      }
    }

    private void processConnectedDecision(ConnId connId, ConnStatus decided, Identifier msgId, KAddress peer, S state) {
      if (decided.equals(ConnStatus.Base.CONNECTED_ACK)) {
        servers.put(connId, new ServerState(connId.serverId, peer, config));
        ConnMsgs.Client reply = ConnMsgs.clientConnectedAck(msgId, connId);
        networkSend.accept(peer, reply);
      } else if (decided.equals(ConnStatus.Base.DISCONNECTED)) {
        disconnect(connId, msgId, peer);
      } else {
        throw new RuntimeException("weird:" + decided);
      }
    }

    private void processStateDecision(ConnId connId, ConnStatus decided, Identifier msgId, KAddress peer, S state) {
      if (decided.equals(ConnStatus.Base.DISCONNECTED)) {
        disconnect(connId, msgId, peer);
      } else if (decided.equals(ConnStatus.Base.NOTHING)) {
        //nothing
      } else {
        throw new RuntimeException("weird");
      }
    }

    private void processUpdateDecision(ConnId connId, ConnStatus decided, KAddress peer) {
      if (decided.equals(ConnStatus.Base.DISCONNECT)) {
        disconnect(connId, msgIds.randomId(), peer);
      } else if (decided.equals(ConnStatus.Base.CLIENT_STATE)) {
        ConnMsgs.Client content = ConnMsgs.clientState(msgIds.randomId(), connId, state);
        networkSend.accept(peer, content);
      } else {
        throw new RuntimeException("weird update:" + decided);
      }
    }

    private void disconnect(ConnId connId, Identifier msgId, KAddress peer) {
      disconnectCleanup(connId);
      disconnectMsg(connId, msgId, peer);
    }

    private void disconnectCleanup(ConnId connId) {
      servers.remove(connId);
      ctrl.close(connId);
    }

    private void disconnectMsg(ConnId connId, Identifier msgId, KAddress peer) {
      ConnMsgs.Client reply = ConnMsgs.clientDisconnect(msgId, connId);
      networkSend.accept(peer, reply);
    }
  }

  public static class Server<S extends ConnState, C extends ConnState> {

    public final InstanceId serverId;
    private final ConnCtrl<S, C> ctrl;
    private final ConnConfig config;
    private S state;

    private TupleHelper.PairConsumer<KAddress, ConnMsgs.Base> networkSend;
    private TimerProxy timer;
    private Logger logger;
    private UUID periodicCheck;

    private final Map<ConnId, ClientState> clients = new HashMap<>();

    public Server(InstanceId serverId, ConnCtrl ctrl, ConnConfig config, S state) {
      this.serverId = serverId;
      this.ctrl = ctrl;
      this.config = config;
      this.state = state;
    }

    public Server setup(TimerProxy timer, TupleHelper.PairConsumer<KAddress, ConnMsgs.Base> networkSend,
      Logger logger) {
      this.timer = timer;
      this.networkSend = networkSend;
      this.logger = logger;
      periodicCheck = timer.schedulePeriodicTimer(config.checkPeriod, config.checkPeriod, periodicCheck());
      return this;
    }

    public void close() {
      clients.entrySet().forEach((client) -> {
        ConnId connId = client.getKey();
        ClientState clientState = client.getValue();
        ctrl.close(connId);
        disconnectMsg(connId, clientState.connectMsgId, clientState.address);
      });
      if (periodicCheck != null) {
        timer.cancelPeriodicTimer(periodicCheck);
        periodicCheck = null;
      }
    }

    public void update(S state) {
      this.state = state;
      ctrl.selfUpdate(serverId, state).entrySet().forEach((client) -> {
        ConnId connId = client.getKey();
        ClientState clientState = clients.get(connId);
        if (clientState == null) {
          throw new RuntimeException("weird - client is disconnected?");
        }
        ConnStatus decidedStatus = client.getValue();
        logger.debug("{} up:{}", connId, decidedStatus);
        processUpdateDecision(connId, decidedStatus, clientState.connectMsgId, clientState.address);
      });
    }

    public void handleContent(KAddress clientAddress, ConnMsgs.Base<C> content) {
      ConnIds.ConnId connId = content.connId;
      ConnStatus status = content.status;
      logger.debug("{} partner:{}", connId, status);
      if (ConnStatus.Base.CONNECT.equals(status)) {
        C newClientState = content.state.get();
        ConnStatus decidedStatus = ctrl.partnerUpdate(connId, state, status, clientAddress, newClientState);
        logger.debug("{} self:{}", connId, decidedStatus);
        processConnectDecision(connId, decidedStatus, content.msgId, clientAddress, newClientState);
      } else {
        ClientState<C> clientState = clients.get(connId);
        if (clientState == null) {
          if (!ConnStatus.Base.DISCONNECT.equals(status)) {
            logger.debug("{} self:{}", connId, ConnStatus.Base.DISCONNECTED);
            disconnectMsg(connId, content.msgId, clientAddress);
          }
          return;
        }
        if (ConnStatus.Base.HEARTBEAT.equals(status)) {
          clientState.heartbeat();
          ConnMsgs.Server reply = ConnMsgs.serverHeartbeat(content.msgId, content.connId);
          networkSend.accept(clientAddress, reply);
        } else if (ConnStatus.Base.DISCONNECT.equals(status)) {
          disconnectCleanup(connId);
        } else if (ConnStatus.Base.CONNECTED_ACK.equals(status)) {
          ConnStatus decidedStatus = ctrl.partnerUpdate(connId, state, status, clientAddress, clientState.state);
          logger.debug("{} self:{}", connId, decidedStatus);
          processOtherDecision(connId, decidedStatus, content.msgId, clientAddress);
        } else if (ConnStatus.Base.CLIENT_STATE.equals(status)) {
          C newClientState = content.state.get();
          ConnStatus decidedStatus = ctrl.partnerUpdate(connId, state, status, clientAddress, newClientState);
          logger.debug("{} self:{}", connId, decidedStatus);
          processOtherDecision(connId, decidedStatus, content.msgId, clientAddress);
        }
      }
    }

    private void processConnectDecision(ConnId connId, ConnStatus decided, Identifier msgId, KAddress peer, C state) {
      if (decided.equals(ConnStatus.Base.CONNECTED)) {
        clients.put(connId, new ClientState(peer, msgId, state, config));
        ConnMsgs.Server reply = ConnMsgs.serverConnected(msgId, connId, state);
        networkSend.accept(peer, reply);
      } else if (decided.equals(ConnStatus.Base.DISCONNECTED)) {
        disconnect(connId, msgId, peer);
      } else {
        throw new RuntimeException("weird:" + decided);
      }
    }

    private void processUpdateDecision(ConnId connId, ConnStatus decided, Identifier msgId, KAddress peer) {
      if (decided.equals(ConnStatus.Base.DISCONNECTED)) {
        disconnect(connId, msgId, peer);
      } else if (decided.equals(ConnStatus.Base.SERVER_STATE)) {
        ConnMsgs.Server content = ConnMsgs.serverState(msgId, connId, state);
        networkSend.accept(peer, content);
      } else {
        throw new RuntimeException("unknown:" + decided);
      }
    }

    private void processOtherDecision(ConnId connId, ConnStatus status, Identifier msgId, KAddress peer) {
      if (status.equals(ConnStatus.Base.DISCONNECTED)) {
        disconnect(connId, msgId, peer);
      } else if (status.equals(ConnStatus.Base.NOTHING)) {
        //nothing
      } else {
        throw new RuntimeException("weird");
      }
    }

    private void disconnect(ConnId connId, Identifier msgId, KAddress peer) {
      disconnectCleanup(connId);
      disconnectMsg(connId, msgId, peer);
    }

    private void disconnectCleanup(ConnId connId) {
      clients.remove(connId);
      ctrl.close(connId);
    }

    private void disconnectMsg(ConnId connId, Identifier msgId, KAddress peer) {
      ConnMsgs.Server reply = ConnMsgs.serverDisconnect(msgId, connId);
      networkSend.accept(peer, reply);
    }

    private Consumer<Boolean> periodicCheck() {
      return (_ignore) -> {
        Iterator<Map.Entry<ConnId, ClientState>> it = clients.entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry<ConnId, ClientState> aux = it.next();
          ConnId connId = aux.getKey();
          ClientState clientState = aux.getValue();
          clientState.period();
          if (clientState.isDead()) {
            ctrl.close(connId);
            it.remove();
          }
        }
      };
    }
  }

  public static class ServerState {

    public final ConnConfig config;
    public final Identifier id;
    public final KAddress address;

    private int missedHeartbeatAck = 0;

    public ServerState(Identifier id, KAddress address, ConnConfig config) {
      this.id = id;
      this.address = address;
      this.config = config;
    }

    public void heartbeat() {
      missedHeartbeatAck = 0;
    }

    public void period() {
      missedHeartbeatAck++;
    }

    public boolean isDead() {
      return missedHeartbeatAck > config.missedHeartbeats;
    }
  }

  public static class ClientState<C extends ConnState> {

    public final ConnConfig config;

    public final KAddress address;
    public final Identifier connectMsgId;
    public final C state;

    private int missedHeartbeat = 0;

    public ClientState(KAddress address, Identifier connectMsgId, C state, ConnConfig config) {
      this.address = address;
      this.connectMsgId = connectMsgId;
      this.state = state;
      this.config = config;
    }

    public void heartbeat() {
      missedHeartbeat = 0;
    }

    public void period() {
      missedHeartbeat++;
    }

    public boolean isDead() {
      return missedHeartbeat > config.missedHeartbeats;
    }
  }

  public static class ServerSendState {

    public final Identifier msgId;
    public final ConnIds.ConnId connId;
    public final KAddress serverAdr;

    public ServerSendState(Identifier msgId, ConnIds.ConnId connId, KAddress serverAdr) {
      this.msgId = msgId;
      this.connId = connId;
      this.serverAdr = serverAdr;
    }
  }
}

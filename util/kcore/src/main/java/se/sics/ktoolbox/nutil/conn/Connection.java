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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.javatuples.Pair;
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

  public static class Client {

    private final InstanceId clientId;
    private final ConnCtrl ctrl;
    private final ConnConfig config;
    private final IdentifierFactory msgIds;

    private ConnState state;

    private TupleHelper.PairConsumer<KAddress, ConnMsgs.Client> networkSend;
    private TimerProxy timer;
    private Logger logger;
    
    private UUID periodicCheck;

    private final Map<ConnId, ServerState> servers = new HashMap<>();

    public Client(InstanceId clientId, ConnCtrl ctrl, ConnConfig config, IdentifierFactory msgIds, ConnState state) {
      this.clientId = clientId;
      this.ctrl = ctrl;
      this.config = config;
      this.msgIds = msgIds;
      this.state = state;
    }

    public Client setup(TimerProxy timer, TupleHelper.PairConsumer<KAddress, ConnMsgs.Client> networkSend, 
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
        ConnMsgs.Client content = new ConnMsgs.Client(msgIds.randomId(), connId, state, ConnStatus.Base.DISCONNECT);
        networkSend.accept(serverAddress, content);
      });
      if (periodicCheck != null) {
        timer.cancelPeriodicTimer(periodicCheck);
        periodicCheck = null;
      }
    }

    public void connect(InstanceId serverId, KAddress server) {
      ConnId connId = new ConnId(serverId, clientId);
      ConnMsgs.Client msg = new ConnMsgs.Client(msgIds.randomId(), connId, state, ConnStatus.Base.CONNECT);
      networkSend.accept(server, msg);
    }

    public void update(ConnState state) {
      this.state = state;
      ctrl.updateState(clientId, state).entrySet().forEach((server) -> {
        ConnId connId = server.getKey();
        ConnStatus serverStatus = server.getValue();
        ServerState serverState = servers.get(connId);
        if (serverState == null) {
          throw new RuntimeException("weird - server is disconnected?");
        }
        ConnMsgs.Client content = new ConnMsgs.Client(msgIds.randomId(), connId, state, serverStatus);
        networkSend.accept(serverState.address, content);
        if (serverStatus.equals(ConnStatus.Base.DISCONNECT)) {
          servers.remove(connId);
        }
      });
    }

    public void handleContent(KAddress serverAddress, ConnMsgs.Server content) {
      Pair<ConnId, ConnStatus> connRes = ctrl.update(content.connId, state,
        content.state, content.status, serverAddress);
      ConnId connId = connRes.getValue0();
      ConnStatus connStatus = connRes.getValue1();
      if (connStatus.equals(ConnStatus.Base.DISCONNECT)) {
        servers.remove(connId);
      } else if (connStatus.equals(ConnStatus.Base.CONNECTED)) {
        servers.put(connId, new ServerState(connId.serverId, serverAddress, config));
      } else if (connStatus.equals(ConnStatus.Base.HEARTBEAT_ACK)) {
        logger.debug("{} {}", new Object[]{connId, content});
        ServerState serverState = servers.get(connId);
        if (serverState == null) {
          throw new RuntimeException("weird - server is disconnected?");
        }
        serverState.heartbeat();
      }
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
            ConnMsgs.Client content = new ConnMsgs.Client(msgIds.randomId(), connId, state, ConnStatus.Base.HEARTBEAT);
            networkSend.accept(serverState.address, content);
          }
        }
      };
    }
  }

  public static class Server {

    private final InstanceId serverId;
    private final ConnCtrl ctrl;
    private final ConnConfig config;
    private ConnState state;

    private TupleHelper.PairConsumer<KAddress, ConnMsgs.Server> networkSend;
    private TimerProxy timer;
    private Logger logger;
    private UUID periodicCheck;

    private final Map<ConnId, ClientState> clients = new HashMap<>();

    public Server(InstanceId serverId, ConnCtrl ctrl, ConnConfig config, ConnState state) {
      this.serverId = serverId;
      this.ctrl = ctrl;
      this.config = config;
      this.state = state;
    }

    public Server setup(TimerProxy timer, TupleHelper.PairConsumer<KAddress, ConnMsgs.Server> networkSend,
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
        ConnMsgs.Server reply = clientState.connect.reply(state, ConnStatus.Base.DISCONNECTED);
        networkSend.accept(clientState.address, reply);
      });
      if (periodicCheck != null) {
        timer.cancelPeriodicTimer(periodicCheck);
        periodicCheck = null;
      }
    }

    public void update(ConnState state) {
      this.state = state;
      ctrl.updateState(serverId, state).entrySet().forEach((client) -> {
        ConnId connId = client.getKey();
        ConnStatus clientStatus = client.getValue();
        ClientState clientState = clients.get(connId);
        if (clientState == null) {
          throw new RuntimeException("weird - client is disconnected?");
        }
        ConnMsgs.Server reply = clientState.connect.reply(state, clientStatus);
        networkSend.accept(clientState.address, reply);
        if (clientStatus.equals(ConnStatus.Base.DISCONNECTED)) {
          clients.remove(connId);
        }
      });
    }

    public void handleContent(KAddress clientAddress, ConnMsgs.Client content) {
      Pair<ConnId, ConnStatus> aux = ctrl.update(content.connId, state,
        content.state, content.status, clientAddress);
      ConnId connId = aux.getValue0();
      ConnStatus connStatus = aux.getValue1();
      ConnMsgs.Server reply = content.reply(state, connStatus);
      networkSend.accept(clientAddress, reply);
      if (connStatus.equals(ConnStatus.Base.CONNECTED)
        && content.status.equals(ConnStatus.Base.CONNECT)) {
        clients.put(connId, new ClientState(clientAddress, content, config));
      } else if (connStatus.equals(ConnStatus.Base.DISCONNECTED)) {
        clients.remove(connId);
      } else if (connStatus.equals(ConnStatus.Base.HEARTBEAT)) {
        ClientState clientState = clients.get(connId);
        if (clientState == null) {
          throw new RuntimeException("weird - client is disconnected?");
        }
        clientState.heartbeat();
      }
    }

    private Consumer<Boolean> periodicCheck() {
      return (_ignore) -> {
        Iterator<Map.Entry<ConnId, ClientState>> it = clients.entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry<ConnId, ClientState> aux = it.next();
          Identifier connId = aux.getKey();
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

  public static class ClientState {

    public final ConnConfig config;

    public final KAddress address;
    public final ConnMsgs.Client connect;

    private int missedHeartbeat = 0;

    public ClientState(KAddress address, ConnMsgs.Client connect, ConnConfig config) {
      this.address = address;
      this.connect = connect;
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
}

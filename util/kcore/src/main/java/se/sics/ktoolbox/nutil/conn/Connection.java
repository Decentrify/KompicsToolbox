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
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.timer.TimerProxy;
import se.sics.ktoolbox.util.TupleHelper;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class Connection {

  public static class Client {

    private final ConnCtrl ctrl;
    private final ConnConfig config;
    private final IdentifierFactory msgIds;

    private ConnState state;

    private TupleHelper.PairConsumer<KAddress, ConnMsgs.Client> networkSend;
    private TimerProxy timer;
    private UUID periodicCheck;

    private final Map<Identifier, ServerState> servers = new HashMap<>();

    public Client(ConnCtrl ctrl, ConnConfig config, IdentifierFactory msgIds, ConnState state) {
      this.ctrl = ctrl;
      this.config = config;
      this.msgIds = msgIds;
      this.state = state;
    }

    public Client setup(TimerProxy timer, TupleHelper.PairConsumer<KAddress, ConnMsgs.Client> networkSend) {
      this.timer = timer;
      this.networkSend = networkSend;
      timer.schedulePeriodicTimer(config.checkPeriod, config.checkPeriod, periodicCheck());
      return this;
    }

    public void close() {
      servers.entrySet().forEach((server) -> {
        Identifier connId = server.getKey();
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

    public void update(ConnState state) {
      this.state = state;
      ctrl.updateState(state).entrySet().forEach((server) -> {
        Identifier connId = server.getKey();
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
      Map.Entry<Identifier, ConnStatus> connStatus = ctrl.update(state, content.state, content.status, serverAddress);
      if (connStatus.getValue().equals(ConnStatus.Base.DISCONNECT)) {
        servers.remove(connStatus.getKey());
      } else if (connStatus.getValue().equals(ConnStatus.Base.CONNECTED)) {
        servers.put(connStatus.getKey(), new ServerState(serverAddress, config));
      } else if (connStatus.getValue().equals(ConnStatus.Base.HEARTBEAT_ACK)) {
        ServerState serverState = servers.get(connStatus.getKey());
        if (serverState == null) {
          throw new RuntimeException("weird - server is disconnected?");
        }
        serverState.heartbeat();
      }
    }

    private Consumer<Boolean> periodicCheck() {
      return (_ignore) -> {
        Iterator<Map.Entry<Identifier, ServerState>> it = servers.entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry<Identifier, ServerState> aux = it.next();
          Identifier connId = aux.getKey();
          ServerState serverState = aux.getValue();
          serverState.period();
          if (serverState.isDead()) {
            ctrl.close(connId);
            it.remove();
          }
        }
      };
    }
  }

  public static class Server {

    private final ConnCtrl ctrl;
    private final ConnConfig config;
    private ConnState state;

    private TupleHelper.PairConsumer<KAddress, ConnMsgs.Server> networkSend;
    private TimerProxy timer;
    private UUID periodicCheck;
    
    public Server(ConnCtrl ctrl, ConnConfig config, ConnState state) {
      this.ctrl = ctrl;
      this.config = config;
      this.state = state;
    }

    private final Map<Identifier, ClientState> clients = new HashMap<>();

    public Server setup(TimerProxy timer, TupleHelper.PairConsumer<KAddress, ConnMsgs.Server> networkSend) {
      this.timer = timer;
      this.networkSend = networkSend;
      timer.schedulePeriodicTimer(config.checkPeriod, config.checkPeriod, periodicCheck());
      return this;
    }

    public void close() {
      clients.entrySet().forEach((client) -> {
        Identifier connId = client.getKey();
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
      ctrl.updateState(state).entrySet().forEach((client) -> {
        Identifier clientId = client.getKey();
        ConnStatus clientStatus = client.getValue();
        ClientState clientState = clients.get(clientId);
        if (clientState == null) {
          throw new RuntimeException("weird - client is disconnected?");
        }
        ConnMsgs.Server reply = clientState.connect.reply(state, clientStatus);
        networkSend.accept(clientState.address, reply);
        if (clientStatus.equals(ConnStatus.Base.DISCONNECTED)) {
          clients.remove(clientId);
        }
      });
    }

    public void handleContent(KAddress clientAddress, ConnMsgs.Client content) {
      Map.Entry<Identifier, ConnStatus> aux = ctrl.update(state, content.state, content.status, clientAddress);
      Identifier connId = aux.getKey();
      ConnStatus connStatus = aux.getValue();
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
        Iterator<Map.Entry<Identifier, ClientState>> it = clients.entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry<Identifier, ClientState> aux = it.next();
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

    public final KAddress address;

    private int missedHeartbeatAck = 0;

    public ServerState(KAddress address, ConnConfig config) {
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

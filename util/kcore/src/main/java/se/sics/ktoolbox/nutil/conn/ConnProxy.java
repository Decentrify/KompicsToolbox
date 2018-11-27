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
import org.slf4j.Logger;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.timer.TimerProxy;
import se.sics.ktoolbox.nutil.timer.TimerProxyImpl;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ConnProxy {

  public static class Client {

    private final KAddress self;
    private final ConnCtrl ctrl;
    private final ConnConfig config;
    private final IdentifierFactory msgIds;

    private ConnState state;
    private ComponentProxy proxy;
    private Positive<Network> network;
    private Positive<Timer> timerPort;
    private TimerProxy timer;
    private Logger logger;
    private UUID periodicCheck;
    private final Map<Identifier, ServerState> servers = new HashMap<>();

    public Client(KAddress self, ConnCtrl ctrl, ConnConfig config, IdentifierFactory msgIds,
      ConnState state) {
      this.self = self;
      this.ctrl = ctrl;
      this.config = config;
      this.msgIds = msgIds;
      this.state = state;
    }

    public void setup(ComponentProxy proxy, Logger logger) {
      this.proxy = proxy;
      this.logger = logger;

      network = proxy.requires(Network.class);
      timerPort = proxy.requires(Timer.class);
      timer = new TimerProxyImpl();
      timer.setup(proxy);

      proxy.subscribe(handleServer, network);

      timer.schedulePeriodicTimer(config.checkPeriod, config.checkPeriod, periodicCheck());
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

    public void close() {
      servers.entrySet().forEach((server) -> {
        Identifier connId = server.getKey();
        KAddress serverAddress = server.getValue().address;
        ctrl.close(connId);
        sendToServer(serverAddress, connId, ConnStatus.Base.DISCONNECT);
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
        sendToServer(serverState.address, connId, serverStatus);
        if (serverStatus.equals(ConnStatus.Base.DISCONNECT)) {
          servers.remove(connId);
        }
      });
    }

    ClassMatchedHandler handleServer
      = new ClassMatchedHandler<ConnMsgs.Server, KContentMsg<KAddress, ?, ConnMsgs.Server>>() {

      @Override
      public void handle(ConnMsgs.Server content, KContentMsg<KAddress, ?, ConnMsgs.Server> container) {
        KAddress serverAddress = container.getHeader().getSource();
        logger.trace("conn client rec:{} from:{}", content, serverAddress);
        Map.Entry<Identifier, ConnStatus> connStatus = ctrl.update(state, content.state, content.status, serverAddress);
        if (connStatus.getValue().equals(ConnStatus.Base.DISCONNECT)) {
          servers.remove(connStatus.getKey());
        } else if(connStatus.getValue().equals(ConnStatus.Base.CONNECTED)) {
          servers.put(connStatus.getKey(), new ServerState(serverAddress, config));
        } else if (connStatus.getValue().equals(ConnStatus.Base.HEARTBEAT_ACK)) {
          ServerState serverState = servers.get(connStatus.getKey());
          if (serverState == null) {
            throw new RuntimeException("weird - server is disconnected?");
          }
          serverState.heartbeat();
        }
      }
    };

    private void sendToServer(KAddress server, Identifier connId, ConnStatus status) {
      KHeader header = new BasicHeader<>(self, server, Transport.UDP);
      ConnMsgs.Client content = new ConnMsgs.Client(msgIds.randomId(), connId, state, status);
      KContentMsg msg = new BasicContentMsg(header, content);
      logger.trace("conn client send:{} to:{}", content, server);
      proxy.trigger(msg, network);
    }
  }

  public static class Server {

    private final ConnCtrl ctrl;
    private final ConnConfig config;
    private ConnState state;
    private ComponentProxy proxy;
    private Positive<Network> network;
    private Positive<Timer> timerPort;
    private TimerProxy timer;
    private Logger logger;
    private UUID periodicCheck;
    private final Map<Identifier, ClientState> clients = new HashMap<>();

    public Server(ConnCtrl ctrl, ConnConfig config, ConnState state) {
      this.ctrl = ctrl;
      this.config = config;
      this.state = state;
    }

    public void setup(ComponentProxy proxy, Logger logger) {
      this.proxy = proxy;
      this.logger = logger;

      network = proxy.requires(Network.class);
      timerPort = proxy.requires(Timer.class);
      timer = new TimerProxyImpl();
      timer.setup(proxy);

      proxy.subscribe(handleClient, network);

      timer.schedulePeriodicTimer(config.checkPeriod, config.checkPeriod, periodicCheck());
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

    public void close() {
      clients.entrySet().forEach((client) -> {
        Identifier connId = client.getKey();
        ClientState clientState = client.getValue();
        ctrl.close(connId);
        ConnMsgs.Server reply = clientState.connect.reply(state, ConnStatus.Base.DISCONNECTED);
        replyToClient(clientState.address, clientState.msg, reply);
      });
      if (periodicCheck != null) {
        timer.cancelPeriodicTimer(periodicCheck);
        periodicCheck = null;
      }
    }

    public void updateState(ConnState state) {
      this.state = state;
      ctrl.updateState(state).entrySet().forEach((client) -> {
        Identifier clientId = client.getKey();
        ConnStatus clientStatus = client.getValue();
        ClientState clientState = clients.get(clientId);
        if (clientState == null) {
          throw new RuntimeException("weird - client is disconnected?");
        }
        ConnMsgs.Server reply = clientState.connect.reply(state, clientStatus);
        replyToClient(clientState.address, clientState.msg, reply);
        if (clientStatus.equals(ConnStatus.Base.DISCONNECTED)) {
          clients.remove(clientId);
        }
      });
    }

    private void replyToClient(KAddress client, KContentMsg clientMsg, KompicsEvent reply) {
      KContentMsg<KAddress, ?, ConnMsgs.Server> replyMsg = clientMsg.answer(reply);
      logger.trace("conn server send:{} to:{}", reply, client);
      proxy.trigger(replyMsg, network);
    }

    ClassMatchedHandler handleClient
      = new ClassMatchedHandler<ConnMsgs.Client, KContentMsg<KAddress, ?, ConnMsgs.Client>>() {

      @Override
      public void handle(ConnMsgs.Client content, KContentMsg<KAddress, ?, ConnMsgs.Client> container) {
        KAddress clientAddress = container.getHeader().getSource();
        logger.trace("conn server rec:{} from:{}", content, clientAddress);
        Map.Entry<Identifier, ConnStatus> aux = ctrl.update(state, content.state, content.status, clientAddress);
        Identifier connId = aux.getKey();
        ConnStatus connStatus = aux.getValue();
        ConnMsgs.Server reply = container.getContent().reply(state, connStatus);
        replyToClient(clientAddress, container, reply);
        if (connStatus.equals(ConnStatus.Base.CONNECTED) 
          && content.status.equals(ConnStatus.Base.CONNECT)) {
          clients.put(connId, new ClientState(container, config));
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
    };
  }

  public static class ClientState {

    public final ConnConfig config;

    public final KAddress address;
    public final ConnMsgs.Client connect;
    public final KContentMsg<?, ?, ConnMsgs.Client> msg;

    private int missedHeartbeat = 0;

    public ClientState(KContentMsg<?, ?, ConnMsgs.Client> msg, ConnConfig config) {
      this.address = msg.getHeader().getSource();
      this.connect = msg.getContent();
      this.msg = msg;
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
}

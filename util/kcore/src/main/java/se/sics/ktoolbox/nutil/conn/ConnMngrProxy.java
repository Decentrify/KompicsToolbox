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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.javatuples.Pair;
import org.slf4j.Logger;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.conn.ConnIds.InstanceId;
import se.sics.ktoolbox.nutil.timer.TimerProxy;
import se.sics.ktoolbox.nutil.timer.TimerProxyImpl;
import se.sics.ktoolbox.util.TupleHelper;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ConnMngrProxy {

  private final KAddress self;
  private final ServerListener serverListener;

  private Logger logger;
  private ComponentProxy proxy;
  private Positive<Network> network;
  private Positive<Timer> timerPort;
  private TimerProxy timer;

  private Map<ConnIds.InstanceId, Connection.Client> clients = new HashMap<>();
  private Map<ConnIds.InstanceId, Connection.Server> servers = new HashMap<>();

  public ConnMngrProxy(KAddress self, ServerListener serverListener) {
    this.self = self;
    this.serverListener = serverListener;
  }

  public ConnMngrProxy setup(ComponentProxy proxy, Logger logger) {
    this.proxy = proxy;
    this.logger = logger;

    network = proxy.getNegative(Network.class).getPair();
    timerPort = proxy.getNegative(Timer.class).getPair();
    timer = new TimerProxyImpl().setup(proxy, logger);
    proxy.subscribe(handleClient, network);
    proxy.subscribe(handleServer, network);

    return this;
  }

  public void close() {
    clients.values().forEach((client) -> client.close());
    servers.values().forEach((server) -> server.close());
    timer.cancel();
  }

  public void addServer(InstanceId serverId, Connection.Server server) {
    logger.info("conn mngr proxy server:{} add", serverId);
    server.setup(timer, serverNetworkSend(), logger);
    servers.put(serverId, server);
  }
  
  public List<ConnIds.InstanceId> overlayServers(Identifier overlayId) {
    List<ConnIds.InstanceId> result = servers.keySet().parallelStream()
      .filter((serverId) -> serverId.overlayId.equals(overlayId))
      .collect(Collectors.toList());
    return result;
  }
  
  public void addClient(InstanceId clientId, Connection.Client client) {
    logger.info("conn mngr proxy client:{} add", clientId);
    client.setup(timer, clientNetworkSend(), logger);
    clients.put(clientId, client);
  }

  public void connectClient(InstanceId clientId, InstanceId serverId, KAddress serverAddress) {
    Connection.Client client = clients.get(clientId);
    if (client == null) {
      throw new RuntimeException("logic error");
    }
    logger.info("conn mngr proxy client:{} connect to:{}", client.clientId, serverId);

    client.connect(serverId, serverAddress);
  }

  public void updateClient(InstanceId clientId, ConnState state) {
    Connection.Client client = clients.get(clientId);
    if (client == null) {
      throw new RuntimeException("logic error");
    }
    client.update(state);
  }

  public void closeClient(InstanceId clientId) {
    Connection.Client client = clients.remove(clientId);
    if (client == null) {
      throw new RuntimeException("logic error");
    }
    client.close();
  }

  public void updateServer(InstanceId serverId, ConnState state) {
    Connection.Server server = servers.get(serverId);
    if (server == null) {
      throw new RuntimeException("logic error");
    }
    server.update(state);
  }

  public void closeServer(InstanceId serverId) {
    Connection.Server server = servers.remove(serverId);
    if (server == null) {
      throw new RuntimeException("logic error");
    }
    server.close();
  }

  TupleHelper.PairConsumer<KAddress, ConnMsgs.Client> clientNetworkSend() {
    return TupleHelper.pairConsumer((server) -> (content) -> {
      KHeader header = new BasicHeader<>(self, server, Transport.UDP);
      KContentMsg msg = new BasicContentMsg(header, content);
      logger.trace("conn mngr proxy client send:{} to:{}", content, server);
      proxy.trigger(msg, network);
    });
  }

  TupleHelper.PairConsumer<KAddress, ConnMsgs.Server> serverNetworkSend() {
    return TupleHelper.pairConsumer((client) -> (content) -> {
      KHeader header = new BasicHeader<>(self, client, Transport.UDP);
      KContentMsg msg = new BasicContentMsg(header, content);
      logger.trace("conn mngr proxy server send:{} to:{}", content, client);
      proxy.trigger(msg, network);
    });
  }

  ClassMatchedHandler handleServer
    = new ClassMatchedHandler<ConnMsgs.Server, KContentMsg<KAddress, ?, ConnMsgs.Server>>() {
    @Override
    public void handle(ConnMsgs.Server content, KContentMsg<KAddress, ?, ConnMsgs.Server> container) {
      KAddress serverAddress = container.getHeader().getSource();
      logger.trace("conn mngr proxy client rec:{} from:{}", content, serverAddress);
      Connection.Client client = clients.get(content.connId.clientId);
      if (client == null) {
        //TODO Alex - investigate later
        return;
      }
      client.handleContent(serverAddress, content);
    }
  };

  ClassMatchedHandler handleClient
    = new ClassMatchedHandler<ConnMsgs.Client, KContentMsg<KAddress, ?, ConnMsgs.Client>>() {

    @Override
    public void handle(ConnMsgs.Client content, KContentMsg<KAddress, ?, ConnMsgs.Client> container) {
      KAddress clientAddress = container.getHeader().getSource();
      logger.trace("{} conn mngr proxy server rec:{} from:{}", new Object[]{content.connId, content, clientAddress});
      Connection.Server server = servers.get(content.connId.serverId);
      if (server == null) {
        Pair<ConnStatus, Optional<Connection.Server>> connect = serverListener.connect(content.connId, content.status,
          clientAddress, content.state);
        if (connect.getValue0().equals(ConnStatus.Base.CONNECTED)) {
          server = connect.getValue1().get().setup(timer, serverNetworkSend(), logger);
          servers.put(server.serverId, server);
          server.handleContent(clientAddress, content);
        } else if (connect.getValue0().equals(ConnStatus.Base.DISCONNECTED)) {
          ConnMsgs.Server reply = content.reply(connect.getValue0());
          serverNetworkSend().accept(clientAddress, reply);
        }
      } else {
        server.handleContent(clientAddress, content);
      }
    }
  };
}

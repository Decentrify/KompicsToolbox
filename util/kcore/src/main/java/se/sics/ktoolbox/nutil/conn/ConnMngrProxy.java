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
import java.util.Map;
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
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ConnMngrProxy {

  private final ConnConfig config;
  private final IdentifierFactory msgIds;
  private final KAddress self;

  private Logger logger;
  private ComponentProxy proxy;
  private Positive<Network> network;
  private Positive<Timer> timerPort;
  private TimerProxy timer;

  private Map<Identifier, Connection.Client> clients = new HashMap<>();
  private Map<Identifier, Connection.Server> servers = new HashMap<>();

  public ConnMngrProxy(KAddress self, ConnConfig config, IdentifierFactory msgIds) {
    this.self = self;
    this.config = config;
    this.msgIds = msgIds;
  }

  public ConnMngrProxy setup(ComponentProxy proxy, Logger logger) {
    this.proxy = proxy;
    this.logger = logger;
    network = proxy.getPositive(Network.class);
    timerPort = proxy.getPositive(Timer.class);
    timer = new TimerProxyImpl().setup(proxy);
    return this;
  }
  
  public void close() {
    clients.values().forEach((client) -> client.close());
    servers.values().forEach((server) -> server.close());
    timer.cancel();
  }

  public void addClient(InstanceId clientId, ConnCtrl ctrl, ConnState state) {
    Connection.Client client = new Connection.Client(clientId, ctrl, config, msgIds, state);
    client.setup(timer, clientNetworkSend());
    clients.put(clientId, client);
  }

  public void connectClient(InstanceId clientId, InstanceId serverId, KAddress serverAddress) {
    Connection.Client client =  clients.get(clientId);
    if(client == null) {
      throw new RuntimeException("logic error");
    }
    client.connect(serverId, self);
  }
  
  public void updateClient(Identifier clientId, ConnState state) {
    Connection.Client client = clients.get(clientId);
    if (client == null) {
      throw new RuntimeException("logic error");
    }
    client.update(state);
  }
  
  public void closeClient(Identifier clientId) {
    Connection.Client client = clients.remove(clientId);
    if (client == null) {
      throw new RuntimeException("logic error");
    }
    client.close();
  }
  
  public void addServer(InstanceId serverId, ConnCtrl ctrl, ConnState state) {
    Connection.Server server = new Connection.Server(serverId, ctrl, config, state);
    server.setup(timer, serverNetworkSend());
    servers.put(serverId, server);
  }

  public void updateServer(Identifier serverId, ConnState state) {
    Connection.Server server = servers.get(serverId);
    if (server == null) {
      throw new RuntimeException("logic error");
    }
    server.update(state);
  }
  
  public void closeServer(Identifier serverId) {
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
      Connection.Client client = clients.get(content.connId);
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
      logger.trace("conn mngr proxy server rec:{} from:{}", content, clientAddress);
      Connection.Server server = servers.get(content.connId);
      if (server == null) {
        //TODO Alex - investigate later
        return;
      }
      server.handleContent(clientAddress, content);
    }
  };
  
}

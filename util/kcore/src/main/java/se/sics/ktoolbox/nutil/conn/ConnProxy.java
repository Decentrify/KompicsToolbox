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

import java.util.Optional;
import org.slf4j.Logger;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.Timer;
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
public class ConnProxy {

  public static class Client {

    private final KAddress self;

    private Logger logger;
    private ComponentProxy proxy;
    private Positive<Network> network;
    private Positive<Timer> timerPort;
    private TimerProxy timer;

    private ConnIds.InstanceId clientId;
    private Connection.Client client;

    public Client(KAddress self) {
      this.self = self;
    }

    public ConnProxy.Client setup(ComponentProxy proxy, Logger logger) {
      this.proxy = proxy;
      this.logger = logger;

      network = proxy.getNegative(Network.class).getPair();
      timerPort = proxy.getNegative(Timer.class).getPair();
      timer = new TimerProxyImpl().setup(proxy, logger);

      proxy.subscribe(handleServer, network);
      return this;
    }
    
    public void startClient(ConnIds.InstanceId clientId, Connection.Client client, 
      InstanceId serverId, KAddress serverAddress) {
      this.clientId = clientId;
      this.client = client
        .setup(timer, networkSend(), logger)
        .start()
        .connect(serverId, serverAddress, Optional.empty());
      logger.info("conn proxy client:{} connect to:{}", client.clientId, serverId);
    }

    public void close() {
      client.close();
      timer.cancel();
    }

    public void update(ConnState state) {
      client.update(state);
    }

    TupleHelper.PairConsumer<KAddress, ConnMsgs.Client> networkSend() {
      return TupleHelper.pairConsumer((server) -> (content) -> {
        KHeader header = new BasicHeader<>(self, server, Transport.UDP);
        KContentMsg msg = new BasicContentMsg(header, content);
        logger.trace("n:{} c:{} conn proxy client send:{} to:{}", new Object[]{self.getId(), content.connId, content, server});
        proxy.trigger(msg, network);
      });
    }

    ClassMatchedHandler handleServer
      = new ClassMatchedHandler<ConnMsgs.Server, KContentMsg<KAddress, ?, ConnMsgs.Server>>() {

      @Override
      public void handle(ConnMsgs.Server content, KContentMsg<KAddress, ?, ConnMsgs.Server> container) {
        KAddress serverAddress = container.getHeader().getSource();
        logger.trace("n:{} c:{} conn client rec:{} from:{}", new Object[]{self.getId(), content.connId, content, serverAddress});
        client.handleContent(serverAddress, content);
      }
    };
  }

  public static class Server {

    private final KAddress self;

    private Logger logger;
    private ComponentProxy proxy;
    private Positive<Network> network;
    private Positive<Timer> timerPort;
    private TimerProxy timer;

    private IdentifierFactory msgIds;
    private ConnIds.InstanceId serverId;
    private Connection.Server server;

    public Server(KAddress self) {
      this.self = self;
    }

    public Server setup(ComponentProxy proxy, Logger logger, IdentifierFactory msgIds) {
      this.logger = logger;
      this.proxy = proxy;
      this.msgIds = msgIds;

      network = proxy.getNegative(Network.class).getPair();
      timerPort = proxy.getNegative(Timer.class).getPair();
      timer = new TimerProxyImpl().setup(proxy, logger);

      proxy.subscribe(handleClient, network);

      return this;
    }
    
    public void startServer(ConnIds.InstanceId serverId, Connection.Server server) {
      this.serverId = serverId;
      this.server = server.setup(timer, networkSend(), logger, msgIds).start();
      logger.info("conn proxy {}", serverId);
    }

    public void update(ConnState state) {
      server.update(state);
    }

    public void close() {
      server.close();
      timer.cancel();
    }

    TupleHelper.PairConsumer<KAddress, ConnMsgs.Server> networkSend() {
      return TupleHelper.pairConsumer((client) -> (content) -> {
        KHeader header = new BasicHeader<>(self, client, Transport.UDP);
        KContentMsg msg = new BasicContentMsg(header, content);
        logger.trace("n:{} c:{} conn proxy server send:{} to:{}", new Object[]{self.getId(), content.connId, content, client});
        proxy.trigger(msg, network);
      });
    }

    ClassMatchedHandler handleClient
      = new ClassMatchedHandler<ConnMsgs.Client, KContentMsg<KAddress, ?, ConnMsgs.Client>>() {

      @Override
      public void handle(ConnMsgs.Client content, KContentMsg<KAddress, ?, ConnMsgs.Client> container) {
        KAddress clientAddress = container.getHeader().getSource();
        logger.trace("n:{} c:{} conn proxy server rec:{} from:{}", 
          new Object[]{self.getId(),content.connId, content, clientAddress});
        server.handleContent(clientAddress, content);
      }
    };
  }
}

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

import org.slf4j.Logger;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.Timer;
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
public class ConnProxy {

  public static class Client {

    private final KAddress self;
    private final Connection.Client base;

    private ComponentProxy proxy;
    private Positive<Network> network;
    private Positive<Timer> timerPort;
    private TimerProxy timer;
    private Logger logger;

    public Client(KAddress self, Connection.Client base) {
      this.self = self;
      this.base = base;
    }

    public void setup(ComponentProxy proxy, Logger logger) {
      this.proxy = proxy;
      this.logger = logger;

      network = proxy.requires(Network.class);
      timerPort = proxy.requires(Timer.class);
      timer = new TimerProxyImpl();
      timer.setup(proxy);

      proxy.subscribe(handleServer, network);

      base.setup(timer, networkSend());
    }

    public void update(ConnState state) {
      base.update(state);
    }

    public void close() {
      base.close();
    }

    TupleHelper.PairConsumer<KAddress, ConnMsgs.Client> networkSend() {
      return TupleHelper.pairConsumer((server) -> (content) -> {
        KHeader header = new BasicHeader<>(self, server, Transport.UDP);
        KContentMsg msg = new BasicContentMsg(header, content);
        logger.trace("conn proxy client send:{} to:{}", content, server);
        proxy.trigger(msg, network);
      });
    }

    ClassMatchedHandler handleServer
      = new ClassMatchedHandler<ConnMsgs.Server, KContentMsg<KAddress, ?, ConnMsgs.Server>>() {

      @Override
      public void handle(ConnMsgs.Server content, KContentMsg<KAddress, ?, ConnMsgs.Server> container) {
        KAddress serverAddress = container.getHeader().getSource();
        logger.trace("conn client rec:{} from:{}", content, serverAddress);
        base.handleContent(serverAddress, content);
      }
    };
  }

  public static class Server {

    private final KAddress self;
    private Connection.Server base;
    private ComponentProxy proxy;
    private Positive<Network> network;
    private Positive<Timer> timerPort;
    private TimerProxy timer;
    private Logger logger;

    public Server(KAddress self, Connection.Server base) {
      this.self = self;
      this.base = base;
    }

    public void setup(ComponentProxy proxy, Logger logger) {
      this.proxy = proxy;
      this.logger = logger;

      network = proxy.requires(Network.class);
      timerPort = proxy.requires(Timer.class);
      timer = new TimerProxyImpl();
      timer.setup(proxy);

      proxy.subscribe(handleClient, network);
      base.setup(timer, networkSend());
    }

    public void update(ConnState state) {
      base.update(state);
    }

    public void close() {
      base.close();
    }

    TupleHelper.PairConsumer<KAddress, ConnMsgs.Server> networkSend() {
      return TupleHelper.pairConsumer((client) -> (content) -> {
        KHeader header = new BasicHeader<>(self, client, Transport.UDP);
        KContentMsg msg = new BasicContentMsg(header, content);
        logger.trace("conn proxy server send:{} to:{}", content, client);
        proxy.trigger(msg, network);
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
        base.handleContent(clientAddress, content);
      }
    };
  }
}

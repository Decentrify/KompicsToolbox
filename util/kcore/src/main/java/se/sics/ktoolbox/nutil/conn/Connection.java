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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.timer.TimerProxy;
import se.sics.ktoolbox.util.TupleHelper;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class Connection {

  public static class Client<S extends ConnState, C extends ConnState> {

    public final ConnIds.InstanceId clientId;
    private final ConnCtrl<C, S> ctrl;
    private final ConnConfig config;
    private final IdentifierFactory msgIds;

    private TupleHelper.PairConsumer<KAddress, ConnMsgs.Base> networkSend;
    private TimerProxy timer;
    private Logger logger;

    private UUID periodicCheck;
    private final Map<ConnIds.ConnId, ClientConn> connections = new HashMap<>();
    private C state;

    public Client(ConnIds.InstanceId clientId, ConnCtrl<C, S> ctrl, ConnConfig config, IdentifierFactory msgIds,
      C state) {
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

    private Consumer<Boolean> periodicCheck() {
      return (_ignore) -> {
        Iterator<ClientConn> it = connections.values().iterator();
        while (it.hasNext()) {
          ClientConn conn = it.next();
          ConnStatus.Decision decision = conn.period();
          if (ConnStatus.Decision.DISCONNECT.equals(decision)) {
            it.remove();
          }
        }
      };
    }

    public void close() {
      connections.values().forEach((conn) -> conn.close());
      if (periodicCheck != null) {
        timer.cancelPeriodicTimer(periodicCheck);
        periodicCheck = null;
      }
    }

    public void connect(ConnIds.InstanceId serverId, KAddress server, Optional<S> serverState) {
      ConnIds.ConnId connId = new ConnIds.ConnId(serverId, clientId);
      ClientConn conn = new ClientConn(ctrl, connId, server, state, msgIds, networkSend, logger, config);
      ConnStatus.Decision decision = conn.connect(serverState);
      if (ConnStatus.Decision.PROCEED.equals(decision)) {
        connections.put(connId, conn);
      }
    }

    public void update(C state) {
      this.state = state;
      Iterator<ClientConn> it = connections.values().iterator();
      while (it.hasNext()) {
        ClientConn conn = it.next();
        ConnStatus.Decision decision = conn.update(state);
        if (ConnStatus.Decision.DISCONNECT.equals(decision)) {
          it.remove();
        }
      }
    }

    public void handleContent(KAddress serverAdr, ConnMsgs.Server<S> content) {
      ConnIds.ConnId connId = content.connId;
      ClientConn conn = connections.get(connId);
      if (conn == null) {
        if (!ConnStatus.BaseServer.DISCONNECT.equals(content.getStatus())) {
          ConnMsgs.Client resp = ConnMsgs.clientDisconnect(msgIds.randomId(), connId);
          networkSend.accept(serverAdr, resp);
        }
        return;
      }
      ConnStatus.Decision decision = conn.handleContent(content);
    }
  }

  static class ClientConn<S extends ConnState, C extends ConnState> {

    final IdentifierFactory msgIds;
    final TupleHelper.PairConsumer<KAddress, ConnMsgs.Base> networkSend;
    final ConnConfig config;
    final Logger logger;
    final ConnCtrl<C, S> ctrl;
    final ConnIds.ConnId connId;
    final KAddress serverAdr;
    C selfState;
    S serverState;
    ConnStatus.System connStatus;
    int missedHeartbeatAck = 0;

    ClientConn(ConnCtrl<C, S> ctrl, ConnIds.ConnId connId, KAddress serverAddres, C selfState,
      IdentifierFactory msgIds, TupleHelper.PairConsumer<KAddress, ConnMsgs.Base> networkSend, Logger logger,
      ConnConfig config) {
      this.ctrl = ctrl;
      this.connId = connId;
      this.serverAdr = serverAddres;
      this.selfState = selfState;
      this.msgIds = msgIds;
      this.networkSend = networkSend;
      this.logger = logger;
      this.config = config;
      this.connStatus = ConnStatus.System.EMPTY;
    }

    ConnStatus.Decision connect(Optional<S> serverState) {
      ConnStatus.Decision decision = ctrl.connect(connId, serverAdr, selfState, serverState);
      if (ConnStatus.Decision.PROCEED.equals(decision)) {
        if (serverState.isPresent()) {
          this.serverState = serverState.get();
        }
        connect();
      }
      return decision;
    }

    void close() {
      if (ConnStatus.System.EMPTY.equals(connStatus)) {
        //nothing to do;
        return;
      }
      disconnectAll();
    }

    ConnStatus.Decision update(C selfState) {
      if (!ConnStatus.System.READY.equals(connStatus)) {
        //nothing to do;
        return ConnStatus.Decision.PROCEED;
      }
      this.selfState = selfState;
      ConnStatus.Decision decision = ctrl.selfUpdate(connId, selfState, serverState);
      switch (decision) {
        case PROCEED: {
          ConnMsgs.Client content = ConnMsgs.clientState(msgIds.randomId(), connId, selfState);
          networkSend.accept(serverAdr, content);
        }
        break;
        case DISCONNECT: {
          disconnectAll();
        }
        break;
        case NOTHING: {
        }
        break;
        default:
          throw new UnsupportedOperationException();
      }
      return decision;
    }

    ConnStatus.Decision period() {
      if (!ConnStatus.System.READY.equals(connStatus)) {
        //nothing to do;
        return ConnStatus.Decision.PROCEED;
      }
      missedHeartbeatAck++;
      if (missedHeartbeatAck > config.missedHeartbeats) {
        disconnectAll();
        return ConnStatus.Decision.DISCONNECT;
      } else {
        ConnMsgs.Client content = ConnMsgs.clientHeartbeat(msgIds.randomId(), connId);
        networkSend.accept(serverAdr, content);
        return ConnStatus.Decision.PROCEED;
      }
    }

    public ConnStatus.Decision handleContent(ConnMsgs.Server<S> content) {
      logger.debug("{} server:{}", connId, content.getStatus());
      ConnStatus.Decision decision;
      switch (connStatus) {
        case EMPTY: {
          //ignore all for the moment;
          decision = ConnStatus.Decision.PROCEED;
        }
        break;
        case SETUP: {
          switch (content.getStatus()) {
            case CONNECT: {
              decision = ctrl.connected(connId, selfState, content.state.get());
              switch (decision) {
                case NOTHING:
                case PROCEED: {
                  connectAck(content);
                }
                break;
                case DISCONNECT: {
                  disconnectAll();
                }
                break;
                default:
                  throw new UnsupportedOperationException();
              }
            }
            break;
            case DISCONNECT: {
              disconnectBase();
              decision = ConnStatus.Decision.DISCONNECT;
            }
            break;
            case HEARTBEAT:
            case STATE: {
              //ignore for the moment;
              decision = ConnStatus.Decision.PROCEED;
            }
            default:
              throw new UnsupportedOperationException();
          }
        }
        break;
        case READY: {
          switch (content.getStatus()) {
            case CONNECT: {
              connectAckAgain(content);
              decision = ConnStatus.Decision.PROCEED;
            }
            break;
            case HEARTBEAT: {
              missedHeartbeatAck = 0;
              decision = ConnStatus.Decision.PROCEED;
            }
            break;
            case STATE: {
              decision = ctrl.serverUpdate(connId, selfState, content.state.get());
              switch (decision) {
                case PROCEED: {
                  serverState = content.state.get();
                }
                break;
                case NOTHING: {
                }
                break;
                case DISCONNECT: {
                  disconnectAll();
                }
                break;
                default:
                  throw new UnsupportedOperationException();
              }
            }
            break;
            default:
              throw new UnsupportedOperationException();
          }
        }
        break;
        default:
          throw new UnsupportedOperationException();
      }
      return decision;
    }

    private void connect() {
      connStatus = ConnStatus.System.SETUP;
      ConnMsgs.Client msg = ConnMsgs.clientConnect(msgIds.randomId(), connId, selfState);
      networkSend.accept(serverAdr, msg);
    }

    private void connectAck(ConnMsgs.Server<S> content) {
      connStatus = ConnStatus.System.READY;
      connectAckAgain(content);
    }

    private void connectAckAgain(ConnMsgs.Server<S> content) {
      serverState = content.state.get();
      ConnMsgs.Client resp = ConnMsgs.clientConnectedAck(content.msgId, connId);
      networkSend.accept(serverAdr, resp);
    }

    private void disconnectAll() {
      disconnectBase();
      ConnMsgs.Client content = ConnMsgs.clientDisconnect(msgIds.randomId(), connId);
      networkSend.accept(serverAdr, content);
    }

    private void disconnectBase() {
      connStatus = ConnStatus.System.EMPTY;
      ctrl.close(connId);
    }
  }

  public static class Server<S extends ConnState, C extends ConnState> {

    public final ConnIds.InstanceId serverId;
    private final ConnCtrl<S, C> ctrl;
    private final ConnConfig config;

    private TupleHelper.PairConsumer<KAddress, ConnMsgs.Base> networkSend;
    private TimerProxy timer;
    private Logger logger;
    private IdentifierFactory msgIds;
    private UUID periodicCheck;

    private S state;
    private final Map<ConnIds.ConnId, ServerConn> connections = new HashMap<>();

    public Server(ConnIds.InstanceId serverId, ConnCtrl ctrl, ConnConfig config, S state) {
      this.serverId = serverId;
      this.ctrl = ctrl;
      this.config = config;
      this.state = state;
    }

    public Server setup(TimerProxy timer, TupleHelper.PairConsumer<KAddress, ConnMsgs.Base> networkSend,
      Logger logger, IdentifierFactory msgIds) {
      this.timer = timer;
      this.networkSend = networkSend;
      this.logger = logger;
      this.msgIds = msgIds;
      periodicCheck = timer.schedulePeriodicTimer(config.checkPeriod, config.checkPeriod, periodicCheck());
      return this;
    }

    public void update(S state) {
      this.state = state;
      Iterator<ServerConn> it = connections.values().iterator();
      while (it.hasNext()) {
        ServerConn conn = it.next();
        ConnStatus.Decision decision = conn.update(state);
        if (ConnStatus.Decision.DISCONNECT.equals(decision)) {
          it.remove();
        }
      }
    }

    private Consumer<Boolean> periodicCheck() {
      return (_ignore) -> {
        Iterator<ServerConn> it = connections.values().iterator();
        while (it.hasNext()) {
          ServerConn conn = it.next();
          ConnStatus.Decision decision = conn.period();
          if (ConnStatus.Decision.DISCONNECT.equals(decision)) {
            it.remove();
          }
        }
      };
    }

    public void close() {
      connections.values().forEach((conn) -> conn.close());
      if (periodicCheck != null) {
        timer.cancelPeriodicTimer(periodicCheck);
        periodicCheck = null;
      }
    }

    public void handleContent(KAddress serverAdr, ConnMsgs.Client<C> content) {
      ConnIds.ConnId connId = content.connId;
      ServerConn conn = connections.get(connId);
      if (conn == null) {
        conn = new ServerConn(ctrl, connId, serverAdr, state, msgIds, networkSend, logger, config);
        connections.put(connId, conn);
      }
      ConnStatus.Decision decision = conn.handleContent(content);
      if(ConnStatus.Decision.DISCONNECT.equals(decision)) {
        connections.remove(connId);
      }
    }
  }

  public static class ServerConn<S extends ConnState, C extends ConnState> {

    final IdentifierFactory msgIds;
    final TupleHelper.PairConsumer<KAddress, ConnMsgs.Base> networkSend;
    final ConnConfig config;
    final Logger logger;

    final ConnCtrl<S, C> ctrl;
    final ConnIds.ConnId connId;
    final KAddress clientAdr;
    S selfState;
    C clientState;
    ConnStatus.System connStatus;
    int missedHeartbeatAck = 0;

    ServerConn(ConnCtrl<S, C> ctrl, ConnIds.ConnId connId, KAddress clientAdr, S selfState,
      IdentifierFactory msgIds, TupleHelper.PairConsumer<KAddress, ConnMsgs.Base> networkSend, Logger logger,
      ConnConfig config) {
      this.ctrl = ctrl;
      this.connId = connId;
      this.clientAdr = clientAdr;
      this.selfState = selfState;
      this.msgIds = msgIds;
      this.networkSend = networkSend;
      this.logger = logger;
      this.config = config;
      this.connStatus = ConnStatus.System.EMPTY;
    }

    void close() {
      if (ConnStatus.System.EMPTY.equals(connStatus)) {
        //nothing to do;
        return;
      }
      disconnectAll(msgIds.randomId());
    }

    ConnStatus.Decision update(S selfState) {
      if (!ConnStatus.System.READY.equals(connStatus)) {
        //nothing to do;
        return ConnStatus.Decision.PROCEED;
      }
      this.selfState = selfState;
      ConnStatus.Decision decision = ctrl.selfUpdate(connId, selfState, clientState);
      switch (decision) {
        case PROCEED: {
          ConnMsgs.Server content = ConnMsgs.serverState(msgIds.randomId(), connId, selfState);
          networkSend.accept(clientAdr, content);
        }
        break;
        case NOTHING: {
        }
        break;
        case DISCONNECT: {
          disconnectAll(msgIds.randomId());
        }
        break;
      }
      return decision;
    }

    ConnStatus.Decision period() {
      //period goes through all system states. If you do not get to ready state until your heartbeat times out, you are too slow.
      missedHeartbeatAck++;
      if (missedHeartbeatAck > config.missedHeartbeats) {
        disconnectAll(msgIds.randomId());
        return ConnStatus.Decision.DISCONNECT;
      }
      return ConnStatus.Decision.PROCEED;
    }

    public ConnStatus.Decision handleContent(ConnMsgs.Client<C> content) {
      logger.debug("{} client:{}", connId, content.getStatus());
      missedHeartbeatAck = 0;
      ConnStatus.Decision decision;
      switch (connStatus) {
        case EMPTY: {
          switch (content.getStatus()) {
            case CONNECT: {
              clientState = content.state.get();
              decision = ctrl.connect(connId, clientAdr, selfState, Optional.of(clientState));
              switch (decision) {
                case NOTHING:
                case PROCEED: {
                  connected(content);
                }
                break;
                case DISCONNECT: {
                  disconnectAll(content.msgId);
                }
                break;
                default:
                  throw new UnsupportedOperationException();
              }
            }
            break;
            case DISCONNECT: {
              disconnectBase();
              decision = ConnStatus.Decision.DISCONNECT;
            }
            break;
            case CONNECT_ACK:
            case HEARTBEAT:
            case STATE: {
              //ignore for the moment;
              decision = ConnStatus.Decision.PROCEED;
            }
            break;
            default:
              throw new UnsupportedOperationException();
          }
        }
        break;
        case SETUP: {
          switch (content.getStatus()) {
            case CONNECT: {
              connectedAgain(content);
              decision = ConnStatus.Decision.PROCEED;
            }
            break;
            case CONNECT_ACK: {
              connStatus = ConnStatus.System.READY;
              decision = ctrl.connected(connId, selfState, clientState);
              if (ConnStatus.Decision.DISCONNECT.equals(decision)) {
                disconnectAll(content.msgId);
              }
            }
            break;
            case DISCONNECT: {
              disconnectBase();
              decision = ConnStatus.Decision.DISCONNECT;
            }
            break;
            case HEARTBEAT:
            case STATE: {
              //ignore for the moment;
              decision = ConnStatus.Decision.PROCEED;
            }
            break;
            default:
              throw new UnsupportedOperationException();
          }
        }
        break;
        case READY: {
          switch (content.getStatus()) {
            case CONNECT: {
              connectedAgain(content);
              decision = ConnStatus.Decision.PROCEED;
            }
            break;
            case HEARTBEAT: {
              heartbeat(content);
              decision = ConnStatus.Decision.PROCEED;
            }
            break;
            case STATE: {
              decision = ctrl.serverUpdate(connId, selfState, content.state.get());
              switch (decision) {
                case NOTHING:
                case PROCEED: {
                  state(content);
                }
                break;
                case DISCONNECT: {
                  disconnectAll(content.msgId);
                }
                break;
                default:
                  throw new UnsupportedOperationException();
              }
            }
            break;
            case DISCONNECT: {
              disconnectBase();
              decision = ConnStatus.Decision.DISCONNECT;
            }
            break;
            case CONNECT_ACK: {
              //ignore;
              decision = ConnStatus.Decision.PROCEED;
            }
            default:
              throw new UnsupportedOperationException();
          }
        }
        break;
        default:
          throw new UnsupportedOperationException();
      }
      return decision;
    }

    private void heartbeat(ConnMsgs.Client<C> req) {
      ConnMsgs.Server resp = ConnMsgs.serverHeartbeat(req.msgId, connId);
      networkSend.accept(clientAdr, resp);
    }

    private void state(ConnMsgs.Client<C> req) {
      clientState = req.state.get();
      ConnMsgs.Server resp = ConnMsgs.serverHeartbeat(req.msgId, connId);
      networkSend.accept(clientAdr, resp);
    }

    private void connected(ConnMsgs.Client<C> req) {
      connStatus = ConnStatus.System.SETUP;
      connectedAgain(req);
    }

    private void connectedAgain(ConnMsgs.Client<C> req) {
      clientState = req.state.get();
      ConnMsgs.Server resp = ConnMsgs.serverConnected(req.msgId, connId, selfState);
      networkSend.accept(clientAdr, resp);
    }

    private void disconnectAll(Identifier msgId) {
      disconnectBase();
      ConnMsgs.Server content = ConnMsgs.serverDisconnect(msgId, connId);
      networkSend.accept(clientAdr, content);
    }

    private void disconnectBase() {
      connStatus = ConnStatus.System.EMPTY;
      ctrl.close(connId);
    }
  }
}

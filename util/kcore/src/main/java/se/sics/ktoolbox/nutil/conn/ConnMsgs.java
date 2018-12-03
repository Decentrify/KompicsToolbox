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
package se.sics.ktoolbox.nutil.conn;

import java.util.Optional;
import se.sics.kompics.util.Identifiable;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.conn.ConnIds.ConnId;
import se.sics.ktoolbox.nutil.network.portsv2.SelectableMsgV2;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ConnMsgs {

  public static final String CONNECTION = "CONNECTION";

  public static abstract class Base<O extends ConnState> implements SelectableMsgV2, Identifiable {

    public final Identifier msgId;
    public final ConnId connId;
    public final ConnStatus status;
    public final Optional<O> state;

    protected Base(Identifier msgId, ConnId connId, ConnStatus status, Optional<O> state) {
      this.msgId = msgId;
      this.connId = connId;
      this.state = state;
      this.status = status;
    }

    @Override
    public Identifier getId() {
      return msgId;
    }

    @Override
    public String eventType() {
      return CONNECTION;
    }
  }

  public static class Client<C extends ConnState> extends Base<C> {

    public Client(Identifier msgId, ConnId connId, ConnStatus status, C state) {
      super(msgId, connId, status, Optional.of(state));
    }

    public Client(Identifier msgId, ConnId connId, ConnStatus status) {
      super(msgId, connId, status, Optional.empty());
    }

    public <S extends ConnState> Server<S> reply(ConnStatus status, S state) {
      return new Server(msgId, connId, status, state);
    }
    
    public <S extends ConnState> Server<S> reply(ConnStatus status) {
      return new Server(msgId, connId, status);
    }

    @Override
    public String toString() {
      return "Client{" + "status=" + status + '}';
    }
  }

  public static class Server<S extends ConnState> extends Base<S> {

    public Server(Identifier msgId, ConnId connId, ConnStatus status, S state) {
      super(msgId, connId, status, Optional.of(state));
    }
    
    public Server(Identifier msgId, ConnId connId, ConnStatus status) {
      super(msgId, connId, status, Optional.empty());
    }

    @Override
    public String toString() {
      return "Server{" + "status=" + status + '}';
    }
  }
}

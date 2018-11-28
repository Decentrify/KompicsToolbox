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

import se.sics.kompics.KompicsEvent;
import se.sics.kompics.util.Identifiable;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.conn.ConnIds.ConnId;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ConnMsgs {
  private static abstract class Base implements KompicsEvent, Identifiable {
    public final Identifier msgId;
    public final ConnId connId;
    public final ConnState state;
    
    protected Base(Identifier msgId, ConnId connId, ConnState state) {
      this.msgId = msgId;
      this.connId = connId;
      this.state = state;
    }

    @Override
    public Identifier getId() {
      return msgId;
    }
  }

  public static class Client extends Base {
    
    public final ConnStatus status;
    public Client(Identifier msgId, ConnId connId, ConnState state, 
      ConnStatus status) {
      super(msgId, connId, state);
      this.status = status;
    }
    
    public Server reply(ConnState state, ConnStatus status) {
      return new Server(msgId, connId, state, status);
    }
  }
  
  public static class Server extends Base {
    public final ConnStatus status;
    public Server(Identifier msgId, ConnId connId, ConnState state, 
      ConnStatus status) {
      super(msgId, connId, state);
      this.status = status;
    }
  }
}
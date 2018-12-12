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

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ConnStatus {
  public static enum System {
    EMPTY,
    SETUP,
    READY
  }
  
  public static enum Decision {
    PROCEED,
    DISCONNECT,
    NOTHING
  }
  public static enum BaseClient {
    CONNECT(1),
    CONNECT_ACK(2),
    DISCONNECT(3),
    HEARTBEAT(4),
    STATE(5);
    
    public final int ord;

    private BaseClient(int ord) {
      this.ord = ord;
    }
  }
  
  public static enum BaseServer {
    CONNECT(1),
    DISCONNECT(2),
    HEARTBEAT(3),
    STATE(4);
    
    public final int ord;

    private BaseServer(int ord) {
      this.ord = ord;
    }
  }
}

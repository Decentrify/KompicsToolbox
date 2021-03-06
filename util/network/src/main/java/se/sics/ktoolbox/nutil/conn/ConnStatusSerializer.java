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

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import se.sics.kompics.network.netty.serialization.Serializer;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ConnStatusSerializer {

  public static class BaseClient implements Serializer {

    private final int id;

    public BaseClient(int id) {
      this.id = id;
    }

    @Override
    public int identifier() {
      return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
      ConnStatus.BaseClient obj = (ConnStatus.BaseClient) o;
      buf.writeInt(obj.ord);
    }

    @Override
    public ConnStatus.BaseClient fromBinary(ByteBuf buf, Optional<Object> hint) {
      int ord = buf.readInt();
      ConnStatus.BaseClient status;
      switch (ord) {
        case 1:
          status = ConnStatus.BaseClient.CONNECT;
          break;
        case 2:
          status = ConnStatus.BaseClient.CONNECT_ACK;
          break;
        case 3:
          status = ConnStatus.BaseClient.DISCONNECT;
          break;
        case 4:
          status = ConnStatus.BaseClient.HEARTBEAT;
          break;
        case 5:
          status = ConnStatus.BaseClient.STATE;
          break;
        default:
          throw new RuntimeException("unknown");
      }
      return status;
    }
  }
  
  public static class BaseServer implements Serializer {

    private final int id;

    public BaseServer(int id) {
      this.id = id;
    }

    @Override
    public int identifier() {
      return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
      ConnStatus.BaseServer obj = (ConnStatus.BaseServer) o;
      buf.writeInt(obj.ord);
    }

    @Override
    public ConnStatus.BaseServer fromBinary(ByteBuf buf, Optional<Object> hint) {
      int ord = buf.readInt();
      ConnStatus.BaseServer status;
      switch (ord) {
        case 1:
          status = ConnStatus.BaseServer.CONNECT;
          break;
        case 2:
          status = ConnStatus.BaseServer.DISCONNECT;
          break;
        case 3:
          status = ConnStatus.BaseServer.HEARTBEAT;
          break;
        case 4:
          status = ConnStatus.BaseServer.STATE;
          break;
        default:
          throw new RuntimeException("unknown");
      }
      return status;
    }
  }
}

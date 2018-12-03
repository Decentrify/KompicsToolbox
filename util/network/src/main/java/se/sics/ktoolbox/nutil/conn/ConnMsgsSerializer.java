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
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.kompics.util.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ConnMsgsSerializer {

  public static class Client implements Serializer {

    private final int id;

    public Client(int id) {
      this.id = id;
    }

    @Override
    public int identifier() {
      return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
      ConnMsgs.Client obj = (ConnMsgs.Client) o;
      Serializers.toBinary(obj.msgId, buf);
      Serializers.lookupSerializer(ConnIds.ConnId.class).toBinary(obj.connId, buf);
      Serializers.lookupSerializer(ConnStatus.Base.class).toBinary(obj.status, buf);
      buf.writeBoolean(obj.state.isPresent());
      if (obj.state.isPresent()) {
        Serializers.toBinary(obj.state, buf);
      }
    }

    @Override
    public ConnMsgs.Client fromBinary(ByteBuf buf, Optional<Object> hint) {
      Identifier msgId = (Identifier) Serializers.fromBinary(buf, hint);
      ConnIds.ConnId connId = (ConnIds.ConnId) Serializers.lookupSerializer(ConnIds.ConnId.class)
        .fromBinary(buf, hint);
      ConnStatus.Base status = (ConnStatus.Base) Serializers.lookupSerializer(ConnStatus.Base.class)
        .fromBinary(buf, hint);
      boolean hasState = buf.readBoolean();
      if (hasState) {
        ConnState state = (ConnState) Serializers.fromBinary(buf, hint);
        return new ConnMsgs.Client(msgId, connId, status, state);
      } else {
        return new ConnMsgs.Client(msgId, connId, status);
      }
    }
  }

  public static class Server implements Serializer {

    private final int id;

    public Server(int id) {
      this.id = id;
    }

    @Override
    public int identifier() {
      return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
      ConnMsgs.Server obj = (ConnMsgs.Server) o;
      Serializers.toBinary(obj.msgId, buf);
      Serializers.lookupSerializer(ConnIds.ConnId.class).toBinary(obj.connId, buf);
      Serializers.lookupSerializer(ConnStatus.Base.class).toBinary(obj.status, buf);
      buf.writeBoolean(obj.state.isPresent());
      if (obj.state.isPresent()) {
        Serializers.toBinary(obj.state, buf);
      }
    }

    @Override
    public ConnMsgs.Server fromBinary(ByteBuf buf, Optional<Object> hint) {
      Identifier msgId = (Identifier) Serializers.fromBinary(buf, hint);
      ConnIds.ConnId connId = (ConnIds.ConnId) Serializers.lookupSerializer(ConnIds.ConnId.class)
        .fromBinary(buf, hint);
      ConnStatus.Base status = (ConnStatus.Base) Serializers.lookupSerializer(ConnStatus.Base.class)
        .fromBinary(buf, hint);
      boolean hasState = buf.readBoolean();
      if (hasState) {
        ConnState state = (ConnState) Serializers.fromBinary(buf, hint);
        return new ConnMsgs.Server(msgId, connId, status, state);
      } else {
        return new ConnMsgs.Server(msgId, connId, status);
      }
    }
  }
}

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
public class ConnIdsSerializer {

  public static class InstanceId implements Serializer {

    private final int id;

    public InstanceId(int id) {
      this.id = id;
    }

    @Override
    public int identifier() {
      return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
      ConnIds.InstanceId obj = (ConnIds.InstanceId) o;
      Serializers.toBinary(obj.nodeId, buf);
      Serializers.toBinary(obj.batchId, buf);
      Serializers.toBinary(obj.instanceId, buf);
      buf.writeBoolean(obj.server);
    }

    @Override
    public ConnIds.InstanceId fromBinary(ByteBuf buf, Optional<Object> hint) {
      Identifier nodeId = (Identifier) Serializers.fromBinary(buf, hint);
      Identifier batchId = (Identifier) Serializers.fromBinary(buf, hint);
      Identifier instanceId = (Identifier) Serializers.fromBinary(buf, hint);
      boolean server = buf.readBoolean();
      return new ConnIds.InstanceId(nodeId, batchId, instanceId, server);
    }
  }

  public static class ConnId implements Serializer {

    private final int id;

    public ConnId(int id) {
      this.id = id;
    }

    @Override
    public int identifier() {
      return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
      ConnIds.ConnId obj = (ConnIds.ConnId) o;
      Serializers.lookupSerializer(ConnIds.InstanceId.class).toBinary(obj.serverId, buf);
      Serializers.lookupSerializer(ConnIds.InstanceId.class).toBinary(obj.clientId, buf);
    }

    @Override
    public ConnIds.ConnId fromBinary(ByteBuf buf, Optional<Object> hint) {
      ConnIds.InstanceId serverId = (ConnIds.InstanceId)Serializers.lookupSerializer(ConnIds.InstanceId.class)
        .fromBinary(buf, hint);
      ConnIds.InstanceId clientId = (ConnIds.InstanceId)Serializers.lookupSerializer(ConnIds.InstanceId.class)
        .fromBinary(buf, hint);
      return new ConnIds.ConnId(serverId, clientId);
    }
  }
}

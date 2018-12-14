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
package se.sics.ktoolbox.nutil.conn.workers;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.conn.ConnIds;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class WorkMsgsSerializer {

  public static class NewTask implements Serializer {

    private final int id;

    public NewTask(int id) {
      this.id = id;
    }

    @Override
    public int identifier() {
      return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
      WorkMsgs.NewTask obj = (WorkMsgs.NewTask) o;
      Serializers.toBinary(obj.msgId, buf);
      Serializers.lookupSerializer(ConnIds.ConnId.class).toBinary(obj.connId, buf);
      Serializers.toBinary(obj.taskId, buf);
      Serializers.toBinary(obj.task, buf);
    }

    @Override
    public WorkMsgs.NewTask fromBinary(ByteBuf buf, Optional<Object> hint) {
      Identifier msgId = (Identifier) Serializers.fromBinary(buf, hint);
      ConnIds.ConnId connId = (ConnIds.ConnId) Serializers.lookupSerializer(ConnIds.ConnId.class)
        .fromBinary(buf, hint);
      Identifier taskId = (Identifier) Serializers.fromBinary(buf, hint);
      WorkTask.Request req = (WorkTask.Request) Serializers.fromBinary(buf, hint);
      return new WorkMsgs.NewTask(msgId, connId, req);
    }
  }

  public static class StatusTask implements Serializer {

    private final int id;

    public StatusTask(int id) {
      this.id = id;
    }

    @Override
    public int identifier() {
      return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
      WorkMsgs.StatusTask obj = (WorkMsgs.StatusTask) o;
      Serializers.toBinary(obj.msgId, buf);
      Serializers.lookupSerializer(ConnIds.ConnId.class).toBinary(obj.connId, buf);
      Serializers.toBinary(obj.taskId, buf);
      Serializers.toBinary(obj.status, buf);
    }

    @Override
    public WorkMsgs.StatusTask fromBinary(ByteBuf buf, Optional<Object> hint) {
      Identifier msgId = (Identifier) Serializers.fromBinary(buf, hint);
      ConnIds.ConnId connId = (ConnIds.ConnId) Serializers.lookupSerializer(ConnIds.ConnId.class)
        .fromBinary(buf, hint);
      Identifier taskId = (Identifier) Serializers.fromBinary(buf, hint);
      WorkTask.Status status = (WorkTask.Status) Serializers.fromBinary(buf, hint);
      return new WorkMsgs.StatusTask(msgId, connId, status);
    }
  }

  public static class CompletedTask implements Serializer {

    private final int id;

    public CompletedTask(int id) {
      this.id = id;
    }

    @Override
    public int identifier() {
      return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
      WorkMsgs.CompletedTask obj = (WorkMsgs.CompletedTask) o;
      Serializers.toBinary(obj.msgId, buf);
      Serializers.lookupSerializer(ConnIds.ConnId.class).toBinary(obj.connId, buf);
      Serializers.toBinary(obj.taskId, buf);
      Serializers.toBinary(obj.result, buf);
    }

    @Override
    public WorkMsgs.CompletedTask fromBinary(ByteBuf buf, Optional<Object> hint) {
      Identifier msgId = (Identifier) Serializers.fromBinary(buf, hint);
      ConnIds.ConnId connId = (ConnIds.ConnId) Serializers.lookupSerializer(ConnIds.ConnId.class)
        .fromBinary(buf, hint);
      Identifier taskId = (Identifier) Serializers.fromBinary(buf, hint);
      WorkTask.Result result = (WorkTask.Result) Serializers.fromBinary(buf, hint);
      return new WorkMsgs.CompletedTask(msgId, connId, result);
    }
  }
  
  public static class CancelTask implements Serializer {

    private final int id;

    public CancelTask(int id) {
      this.id = id;
    }

    @Override
    public int identifier() {
      return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
      WorkMsgs.CancelTask obj = (WorkMsgs.CancelTask) o;
      Serializers.toBinary(obj.msgId, buf);
      Serializers.lookupSerializer(ConnIds.ConnId.class).toBinary(obj.connId, buf);
      Serializers.toBinary(obj.taskId, buf);
    }

    @Override
    public WorkMsgs.CancelTask fromBinary(ByteBuf buf, Optional<Object> hint) {
      Identifier msgId = (Identifier) Serializers.fromBinary(buf, hint);
      ConnIds.ConnId connId = (ConnIds.ConnId) Serializers.lookupSerializer(ConnIds.ConnId.class)
        .fromBinary(buf, hint);
      Identifier taskId = (Identifier) Serializers.fromBinary(buf, hint);
      return new WorkMsgs.CancelTask(msgId, connId, taskId);
    }
  }
}

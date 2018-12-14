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

import se.sics.kompics.util.Identifiable;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.conn.ConnIds;
import se.sics.ktoolbox.nutil.network.portsv2.SelectableMsgV2;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class WorkMsgs {
  public static final String MSG_TYPE = "WORK_CENTER";
  
  public static abstract class Base implements SelectableMsgV2, Identifiable {

    public final Identifier msgId;
    public final ConnIds.ConnId connId;
    public final Identifier taskId;

    public Base(Identifier msgId, ConnIds.ConnId connId, Identifier taskId) {
      this.msgId = msgId;
      this.connId = connId;
      this.taskId = taskId;
    }

    @Override
    public Identifier getId() {
      return msgId;
    }

    @Override
    public String eventType() {
      return MSG_TYPE;
    }
  }

  public static class NewTask extends Base {
    public final WorkTask.Request task;
    public NewTask(Identifier msgId, ConnIds.ConnId connId, WorkTask.Request task) {
      super(msgId, connId, task.taskId());
      this.task = task;
    }

    @Override
    public String toString() {
      return "NewTask";
    }
  }
  
  public static class CancelTask extends Base {

    public CancelTask(Identifier msgId, ConnIds.ConnId connId, Identifier taskId) {
      super(msgId, connId, taskId);
    }
    
    @Override
    public String toString() {
      return "CancelTask";
    }
  }
  
  public static class StatusTask extends Base {
    public final WorkTask.Status status;
    public StatusTask(Identifier msgId, ConnIds.ConnId connId, WorkTask.Status status) {
      super(msgId, connId, status.taskId());
      this.status = status;
    }
    
    @Override
    public String toString() {
      return "TaskStatus";
    }
  }

  public static class CompletedTask extends Base {
    public final WorkTask.Result result;
    public CompletedTask(Identifier msgId, ConnIds.ConnId connId, WorkTask.Result result) {
      super(msgId, connId, result.taskId());
      this.result = result;
    }

    @Override
    public String toString() {
      return "CompletedTask";
    }
  }
}

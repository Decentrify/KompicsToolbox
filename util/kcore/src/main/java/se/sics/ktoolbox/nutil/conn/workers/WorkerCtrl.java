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

import java.util.HashMap;
import java.util.Map;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.conn.ConnIds;
import se.sics.ktoolbox.util.TupleHelper;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class WorkerCtrl {

  private TupleHelper.PairConsumer<KAddress, WorkMsgs.Base> networkSend;
  private TupleHelper.PairConsumer<ConnIds.ConnId, WorkCenterEvents.NewTask> workDriverSend;
  private IdentifierFactory msgIds;
  private IdentifierFactory eventIds;

  private final Map<Identifier, TaskState> tasks = new HashMap<>();

  public void setup(IdentifierFactory msgIds, IdentifierFactory eventIds,
    TupleHelper.PairConsumer<KAddress, WorkMsgs.Base> networkSend,
    TupleHelper.PairConsumer<ConnIds.ConnId, WorkCenterEvents.NewTask> workDriverSend) {
    this.msgIds = msgIds;
    this.eventIds = eventIds;
    this.networkSend = networkSend;
    this.workDriverSend = workDriverSend;
  }

  public void taskNew(ConnIds.ConnId connId, KAddress partner, WorkTask.Request task) {
    tasks.put(task.taskId(), new TaskState(task.taskId(), partner, connId));
    workDriverSend.accept(connId, new WorkCenterEvents.NewTask(eventIds.randomId(), task));
  }

  public void taskStatus(Identifier taskId, WorkTask.Status update) {
    TaskState task = tasks.get(taskId);
    networkSend.accept(task.partner, new WorkMsgs.StatusTask(msgIds.randomId(), task.connId, update));
  }

  public void taskCompleted(Identifier taskId, WorkTask.Result result) {
    TaskState task = tasks.remove(taskId);
    networkSend.accept(task.partner, new WorkMsgs.CompletedTask(msgIds.randomId(), task.connId, result));
  }

  public void close() {
    throw new UnsupportedOperationException();
  }

  public static class TaskState {

    public final Identifier taskId;
    public final KAddress partner;
    public final ConnIds.ConnId connId;

    public TaskState(Identifier taskId, KAddress partner, ConnIds.ConnId connId) {
      this.taskId = taskId;
      this.partner = partner;
      this.connId = connId;
    }
  }
}

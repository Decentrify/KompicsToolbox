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

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.javatuples.Pair;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.conn.ConnIds;
import se.sics.ktoolbox.util.TupleHelper;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class Mngr {

  private TupleHelper.PairConsumer<KAddress, WorkMsgs.Base> networkSend;
  private IdentifierFactory msgIds;

  private final Map<ConnIds.ConnId, LocalWorkerState> pendingWorkers = new HashMap<>();
  private final Map<ConnIds.ConnId, LocalWorkerState> workers = new HashMap<>();
  private final Map<Identifier, Pair<WorkTask.Request, Consumer<WorkTask.Result>>> tasks = new HashMap<>();

  public Mngr() {
  }

  public void setup(IdentifierFactory msgIds, TupleHelper.PairConsumer<KAddress, WorkMsgs.Base> networkSend) {
    this.msgIds = msgIds;
    this.networkSend = networkSend;
  }

  public void connect(ConnIds.ConnId connId, KAddress workerAdr, WorkerState workerState) {
    pendingWorkers.put(connId, new LocalWorkerState(connId, workerAdr, workerState));
  }

  public void connected(ConnIds.ConnId connId) {
    LocalWorkerState worker = pendingWorkers.remove(connId);
    worker.connected();
    workers.put(connId, worker);
  }

  public void update(ConnIds.ConnId connId, WorkerState workerState) {
    workers.get(connId).updateState(workerState);
  }

  public int readyWorkers() {
    return workers.size();
  }

  public void taskNew(WorkTask.Request task, Consumer<WorkTask.Status> status, Consumer<WorkTask.Result> result) {
    //TODO Alex - slow... fix later
    ArrayList<LocalWorkerState> aux = new ArrayList<>(workers.values());
    Collections.sort(aux, (w1, w2) -> {
      int compR = Doubles.compare(w1.state.load, w2.state.load);
      if (compR == 0) {
        compR = Ints.compare(w1.state.ongoingTasks, w2.state.ongoingTasks);
      }
      return compR;
    });
    //if the method was called - we have at least 1 worker
    LocalWorkerState worker = aux.iterator().next();
    worker.allocateTask(task.taskId(), status, result);
    networkSend.accept(worker.address, new WorkMsgs.NewTask(msgIds.randomId(), worker.connId, task));
  }

  public void taskCompleted(ConnIds.ConnId connId, WorkTask.Result result) {
    LocalWorkerState worker = workers.get(connId);
    worker.completed(result.taskId()).accept(result);
  }

  public void taskStatus(ConnIds.ConnId connId, WorkTask.Status status) {
    LocalWorkerState worker = workers.get(connId);
    worker.update(status.taskId()).accept(status);
  }

  public void close() {
    throw new UnsupportedOperationException();
  }

  public void close(ConnIds.ConnId connId) {
    throw new UnsupportedOperationException();
  }

  static class LocalWorkerState {

    final ConnIds.ConnId connId;
    final KAddress address;
    WorkerStatus status;
    WorkerState state;
    final Map<Identifier, Pair<Consumer<WorkTask.Status>, Consumer<WorkTask.Result>>> allocatedTasks = new HashMap<>();

    public LocalWorkerState(ConnIds.ConnId connId, KAddress workerAdr, WorkerState state) {
      this.connId = connId;
      this.address = workerAdr;
      this.status = WorkerStatus.CONNECT;
      this.state = state;
    }

    public void connected() {
      status = WorkerStatus.READY;
    }

    public void updateState(WorkerState state) {
      this.state = state;
    }

    public void allocateTask(Identifier taskId, Consumer<WorkTask.Status> status, Consumer<WorkTask.Result> result) {
      allocatedTasks.put(taskId, Pair.with(status, result));
    }

    public Consumer<WorkTask.Status> update(Identifier taskId) {
      return allocatedTasks.get(taskId).getValue0();
    }

    public Consumer<WorkTask.Result> completed(Identifier taskId) {
      return allocatedTasks.remove(taskId).getValue1();
    }
  }

  public static enum WorkerStatus {
    CONNECT,
    READY
  }
}

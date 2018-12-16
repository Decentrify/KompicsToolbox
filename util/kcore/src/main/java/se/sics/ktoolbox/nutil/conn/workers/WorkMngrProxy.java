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

import org.slf4j.Logger;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.ktoolbox.nutil.conn.ConnIds;
import se.sics.ktoolbox.util.TupleHelper;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class WorkMngrProxy {

  private final KAddress selfAdr;

  private Logger logger;
  private ComponentProxy proxy;
  private Positive<Network> network;
  private Negative<WorkMngrCenterPort> mngrDriver;

  private WorkMngr workerMngr;
  private IdentifierFactory eventIds;

  public WorkMngrProxy(KAddress selfAdr) {
    this.selfAdr = selfAdr;
    this.workerMngr = new WorkMngr();
  }

  public WorkMngrProxy setup(ComponentProxy proxy, Logger logger, IdentifierFactory msgIds, IdentifierFactory eventIds) {
    this.proxy = proxy;
    this.logger = logger;

    this.eventIds = eventIds;

    network = proxy.getNegative(Network.class).getPair();
    mngrDriver = proxy.getPositive(WorkMngrCenterPort.class).getPair();

    workerMngr.setup(msgIds, networkSend());
    proxy.subscribe(handleNewTask, mngrDriver);
    proxy.subscribe(handleTaskStatus, network);
    proxy.subscribe(handleTaskCompleted, network);
    return this;
  }

  public void close() {
    workerMngr.close();
  }

  public void close(ConnIds.ConnId connId) {
    workerMngr.close(connId);
    if (workerMngr.readyWorkers() == 0) {
      proxy.trigger(new WorkMngrCenterEvents.NoWorkers(eventIds.randomId()), mngrDriver);
    }
  }

  public void connect(ConnIds.ConnId connId, KAddress workerAdr, WorkCtrlState workerState) {
    workerMngr.connect(connId, workerAdr, workerState);
  }

  public void connected(ConnIds.ConnId connId) {
    workerMngr.connected(connId);
    if (workerMngr.readyWorkers() == 1) {
      proxy.trigger(new WorkMngrCenterEvents.Ready(eventIds.randomId()), mngrDriver);
    }
  }

  public void update(ConnIds.ConnId connId, WorkCtrlState workerState) {
    workerMngr.update(connId, workerState);
  }

  public int readyWorkers() {
    return workerMngr.readyWorkers();
  }

  TupleHelper.PairConsumer<KAddress, WorkMsgs.Base> networkSend() {
    return TupleHelper.pairConsumer((client) -> (content) -> {
      KHeader header = new BasicHeader<>(selfAdr, client, Transport.UDP);
      KContentMsg msg = new BasicContentMsg(header, content);
      logger.trace("n:{} c:{} worker mngr send:{} to:{}",
        new Object[]{selfAdr.getId(), content.connId, content, client});
      proxy.trigger(msg, network);
    });
  }

  Handler handleNewTask = new Handler<WorkMngrCenterEvents.TaskNew>() {
    @Override
    public void handle(WorkMngrCenterEvents.TaskNew req) {
      logger.debug("new task:{}", req.task.taskId());
      if (workerMngr.readyWorkers() > 0) {
        workerMngr.taskNew(req.task,
          (status) -> {
          logger.trace("n:{} worker mngr task:{} status", new Object[]{selfAdr.getId(), req.task.taskId()});
          proxy.trigger(req.status(status), mngrDriver);
        },
          (result) -> {
          logger.trace("n:{} worker mngr task:{} completed", new Object[]{selfAdr.getId(), req.task.taskId()});
          proxy.trigger(req.completed(result), mngrDriver);
        });
      } else {
        proxy.trigger(new WorkMngrCenterEvents.NoWorkers(eventIds.randomId()), mngrDriver);
      }
    }
  };

  ClassMatchedHandler handleTaskCompleted
    = new ClassMatchedHandler<WorkMsgs.CompletedTask, KContentMsg<KAddress, ?, WorkMsgs.CompletedTask>>() {

    @Override
    public void handle(WorkMsgs.CompletedTask content, KContentMsg<KAddress, ?, WorkMsgs.CompletedTask> container) {
      KAddress serverAddress = container.getHeader().getSource();
      logger.debug("n:{} c:{} worker mngr rec:{} from:{}",
        new Object[]{selfAdr.getId(), content.connId, content, serverAddress});
      workerMngr.taskCompleted(content.connId, content.result);
    }
  };
  
  ClassMatchedHandler handleTaskStatus
    = new ClassMatchedHandler<WorkMsgs.StatusTask, KContentMsg<KAddress, ?, WorkMsgs.StatusTask>>() {

    @Override
    public void handle(WorkMsgs.StatusTask content, KContentMsg<KAddress, ?, WorkMsgs.StatusTask> container) {
      KAddress serverAddress = container.getHeader().getSource();
      logger.debug("n:{} c:{} worker mngr rec:{} from:{}",
        new Object[]{selfAdr.getId(), content.connId, content, serverAddress});
      workerMngr.taskStatus(content.connId, content.status);
    }
  };
}

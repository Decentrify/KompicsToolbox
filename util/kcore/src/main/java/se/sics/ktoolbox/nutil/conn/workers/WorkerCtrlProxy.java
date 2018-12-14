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
public class WorkerCtrlProxy {
  public final KAddress selfAdr;
  public WorkerCtrl workerCtrl;
  
  private Logger logger;
  private ComponentProxy proxy;
  private Positive<Network> network;
  private Positive<WorkCenterPort> workDriver;  
  
  public WorkerCtrlProxy(KAddress selfAdr) {
    this.selfAdr = selfAdr;
    this.workerCtrl = new WorkerCtrl();
  }
  
  public WorkerCtrlProxy setup(ComponentProxy proxy, Logger logger, IdentifierFactory msgIds, IdentifierFactory eventIds) {
    this.proxy = proxy;
    this.logger = logger;

    network = proxy.getNegative(Network.class).getPair();
    workDriver = proxy.getPositive(WorkCenterPort.class).getPair();

    workerCtrl.setup(msgIds, eventIds, networkSend(), workDriverSend());
    proxy.subscribe(handleNewTask, network);
    proxy.subscribe(handleTaskStatus, workDriver);
    proxy.subscribe(handleTaskCompleted, workDriver);
    return this;
  }
  
  public void close() {
    workerCtrl.close();
  }
  
  TupleHelper.PairConsumer<KAddress, WorkMsgs.Base> networkSend() {
    return TupleHelper.pairConsumer((server) -> (content) -> {
      KHeader header = new BasicHeader<>(selfAdr, server, Transport.UDP);
      KContentMsg msg = new BasicContentMsg(header, content);
      logger.trace("n:{} c:{} worker ctrl send:{} to:{}",
        new Object[]{selfAdr.getId(), content.connId, content, server});
      proxy.trigger(msg, network);
    });
  }
  
  TupleHelper.PairConsumer<ConnIds.ConnId, WorkCenterEvents.NewTask> workDriverSend() {
    return TupleHelper.pairConsumer((connId) -> (content) -> {
      logger.trace("n:{} c:{} worker ctrl send:{} to driver",
        new Object[]{selfAdr.getId(), connId, content});
      proxy.trigger(content, workDriver);
    });
  }
  
  ClassMatchedHandler handleNewTask
      = new ClassMatchedHandler<WorkMsgs.NewTask, KContentMsg<KAddress, ?, WorkMsgs.NewTask>>() {

      @Override
      public void handle(WorkMsgs.NewTask content, KContentMsg<KAddress, ?, WorkMsgs.NewTask> container) {
        KAddress serverAddress = container.getHeader().getSource();
        logger.debug("n:{} c:{} worker ctrl rec:{} from:{}", 
          new Object[]{selfAdr.getId(), content.connId, content, serverAddress});
        workerCtrl.taskNew(content.connId, serverAddress, content.task);
      }
    };
  
  Handler handleTaskStatus = new Handler<WorkCenterEvents.TaskStatus>() {
    @Override
    public void handle(WorkCenterEvents.TaskStatus event) {
      workerCtrl.taskStatus(event.task.taskId(), event.status);
    }
  };
  
  Handler handleTaskCompleted = new Handler<WorkCenterEvents.TaskCompleted>() {
    @Override
    public void handle(WorkCenterEvents.TaskCompleted event) {
      workerCtrl.taskCompleted(event.task.taskId(), event.result);
    }
  };
}

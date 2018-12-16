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
package se.sics.ktoolbox.nutil.conn.workers.simple;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.conn.workers.WorkCtrlCenterEvents;
import se.sics.ktoolbox.nutil.conn.workers.WorkCtrlCenterPort;
import se.sics.ktoolbox.nutil.conn.workers.WorkTask;
import se.sics.ktoolbox.nutil.timer.TimerProxy;
import se.sics.ktoolbox.nutil.timer.TimerProxyImpl;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.network.KAddress;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class WorkCenterDriverComp extends ComponentDefinition {

  Positive<WorkCtrlCenterPort> appPort = requires(WorkCtrlCenterPort.class);
  Positive<Timer> timerPort = requires(Timer.class);
  TimerProxy timer;

  private final Init init;

  private final IdentifierFactory eventIds;

  public WorkCenterDriverComp(Init init) {
    this.init = init;
    eventIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.EVENT, Optional.of(1234l));
    timer = new TimerProxyImpl().setup(proxy, logger);
    subscribe(handleStart, control);
    subscribe(handleNewTask, appPort);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
    }
  };

  Handler handleNewTask = new Handler<WorkCtrlCenterEvents.NewTask>() {
    @Override
    public void handle(WorkCtrlCenterEvents.NewTask req) {
      logger.info("task:{} new", req.task.taskId());
      counter = 0;
      taskTId = timer.schedulePeriodicTimer(1000, 1000, taskUpdate(req));
    }
  };

  int counter = 0;
  UUID taskTId;

  private Consumer<Boolean> taskUpdate(WorkCtrlCenterEvents.NewTask req) {
    return (_ignore) -> {
      if (counter++ < 5) {
        WorkTask.Status status = new TestWorkTask.Status(req.task.taskId());
        trigger(req.update(status), appPort);
      } else {
        WorkTask.Result result = new TestWorkTask.Result(req.task.taskId());
        trigger(req.completed(result), appPort);
        timer.cancelPeriodicTimer(taskTId);
      }
    };
  }

  public static class Init extends se.sics.kompics.Init<WorkCenterDriverComp> {

    public final KAddress self;

    public Init(KAddress self) {
      this.self = self;
    }
  }
}

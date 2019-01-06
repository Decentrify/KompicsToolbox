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
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.ktoolbox.nutil.conn.workers.WorkMngrCenterEvents;
import se.sics.ktoolbox.nutil.conn.workers.WorkMngrCenterPort;
import se.sics.ktoolbox.nutil.conn.workers.WorkTask;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.network.KAddress;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class WorkMngrDriverComp extends ComponentDefinition {

  Positive<WorkMngrCenterPort> appPort = requires(WorkMngrCenterPort.class);

  private final Init init;

  private IdentifierFactory eventIds;

  private boolean requested = true;

  public WorkMngrDriverComp(Init init) {
    this.init = init;
    eventIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.EVENT, Optional.of(1234l));
    subscribe(handleStart, control);
    subscribe(handleReady, appPort);
    subscribe(handleNotReady, appPort);
    subscribe(handleStatus, appPort);
    subscribe(handleResult, appPort);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      test();
    }
  };

  Handler handleNotReady = new Handler<WorkMngrCenterEvents.NoWorkers>() {
    @Override
    public void handle(WorkMngrCenterEvents.NoWorkers event) {
      logger.info("not ready");
      requested = false;
    }
  };

  Handler handleReady = new Handler<WorkMngrCenterEvents.Ready>() {
    @Override
    public void handle(WorkMngrCenterEvents.Ready event) {
      logger.info("ready");
      if (!requested) {
        requested = true;
        test();
      }
    }
  };
  
  Handler handleStatus = new Handler<WorkMngrCenterEvents.TaskStatus>() {
    @Override
    public void handle(WorkMngrCenterEvents.TaskStatus event) {
      logger.info("task:{} status", event.task.taskId());
    }
  };

  Handler handleResult = new Handler<WorkMngrCenterEvents.TaskCompleted>() {
    @Override
    public void handle(WorkMngrCenterEvents.TaskCompleted event) {
      logger.info("task:{} completed", event.task.taskId());
    }
  };

  private void test() {
    WorkTask.Request task = new TestWorkTask.Request(eventIds.randomId());
    logger.info("task:{}", task.taskId());
    trigger(new WorkMngrCenterEvents.TaskNew(eventIds.randomId(), task), appPort);
  }

  public static class Init extends se.sics.kompics.Init<WorkMngrDriverComp> {

    public final KAddress self;

    public Init(KAddress self) {
      this.self = self;
    }
  }
}

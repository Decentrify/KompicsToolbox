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

import se.sics.ktoolbox.nutil.conn.workers.WorkCtrl;
import se.sics.ktoolbox.nutil.conn.workers.MngrState;
import se.sics.ktoolbox.nutil.conn.workers.WorkState;
import java.util.Optional;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.conn.ConnConfig;
import se.sics.ktoolbox.nutil.conn.ConnIds;
import se.sics.ktoolbox.nutil.timer.TimerProxy;
import se.sics.ktoolbox.nutil.timer.TimerProxyImpl;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class WorkCenterComp extends ComponentDefinition {

  private final Positive<Network> networkPort = requires(Network.class);
  private final Positive<Timer> timerPort = requires(Timer.class);
  private final TimerProxy timer;
  private WorkCenter workCenter;
  private final Init init;

  public WorkCenterComp(Init init) {
    this.init = init;
    timer = new TimerProxyImpl();
    workCenter = new WorkCenter(init.selfAdr, init.overlayId, init.ctrlBatchId, init.workBatchId, init.baseId, init.workServerC);
    subscribe(handleStart, control);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      timer.setup(proxy, logger);
      IdentifierFactory msgIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.MSG, Optional.of(1234l));
      WorkState.Server serverInitState = new WorkState.Server() {
      };
      MngrState.Client clientInitState = new MngrState.Client() {
      };
      workCenter.setup(proxy, logger, init.connConfig, msgIds, clientInitState, serverInitState);
      timer.scheduleTimer(1000, (_ignore) -> {
        workCenter.connect(init.ctrlCenterAdr);
      });
    }
  };

  public static class Init extends se.sics.kompics.Init<WorkCenterComp> {

    public final KAddress selfAdr;
    private final Identifier overlayId;
    private final Identifier ctrlBatchId;
    private final Identifier workBatchId;
    private final Identifier baseId;
    public final ConnConfig connConfig;
    public final KAddress ctrlCenterAdr;
    public final WorkCtrl.Server<WorkState.Client> workServerC;

    public Init(KAddress selfAdr, Identifier overlayId, Identifier ctrlBatchId, Identifier workBatchId,
      Identifier baseId, ConnConfig connConfig, KAddress ctrlCenterAdr, WorkCtrl.Server workServerC) {
      this.selfAdr = selfAdr;
      this.overlayId = overlayId;
      this.ctrlBatchId = ctrlBatchId;
      this.workBatchId = workBatchId;
      this.baseId = baseId;
      this.connConfig = connConfig;
      this.ctrlCenterAdr = ctrlCenterAdr;
      this.workServerC = workServerC;
    }
  }
}

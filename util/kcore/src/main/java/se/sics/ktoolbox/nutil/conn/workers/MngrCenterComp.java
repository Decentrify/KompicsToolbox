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

import se.sics.ktoolbox.nutil.conn.workers.MngrState;
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
import se.sics.ktoolbox.nutil.conn.workers.MngrCtrl;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class MngrCenterComp extends ComponentDefinition {

  private final Positive<Network> network = requires(Network.class);
  private final Positive<Timer> timer = requires(Timer.class);
  private MngrCenter ctrlCenter;
  private final Init init;

  public MngrCenterComp(Init init) {
    this.init = init;
    ctrlCenter = new MngrCenter(init.selfAdr, init.overlayId, init.ctrlBatchId, init.workBatchId, init.baseId, init.ctrlServerC);
    subscribe(handleStart, control);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      IdentifierFactory msgIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.MSG, Optional.of(1234l));
      MngrState.Server initState = new MngrState.Server() {
      };
      ctrlCenter.setup(proxy, logger, init.connConfig, msgIds, initState);
    }
  };

  public static class Init extends se.sics.kompics.Init<MngrCenterComp> {

    public final KAddress selfAdr;
    public final Identifier overlayId;
    public final Identifier ctrlBatchId;
    public final Identifier workBatchId;
    public final Identifier baseId;
    public final ConnConfig connConfig;
    public final MngrCtrl.Server<MngrState.Client> ctrlServerC;

    public Init(KAddress selfAdr, Identifier overlayId, Identifier ctrlBatchId, Identifier workBatchId,
      Identifier baseId, ConnConfig connConfig, MngrCtrl.Server<MngrState.Client> ctrlServerC) {
      this.selfAdr = selfAdr;
      this.overlayId = overlayId;
      this.ctrlBatchId = ctrlBatchId;
      this.workBatchId = workBatchId;
      this.baseId = baseId;
      this.connConfig = connConfig;
      this.ctrlServerC = ctrlServerC;
    }
  }
}

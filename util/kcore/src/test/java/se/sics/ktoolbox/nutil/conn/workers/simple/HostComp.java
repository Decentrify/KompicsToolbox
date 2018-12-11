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

import java.util.HashMap;
import java.util.Map;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.conn.ConnConfig;
import se.sics.ktoolbox.nutil.conn.ConnIds;
import se.sics.ktoolbox.nutil.conn.ConnMsgs;
import se.sics.ktoolbox.nutil.conn.util.NetworkEmulator;
import se.sics.ktoolbox.nutil.conn.workers.MngrCtrl;
import se.sics.ktoolbox.nutil.conn.workers.MngrState;
import se.sics.ktoolbox.nutil.conn.workers.MngrCenterComp;
import se.sics.ktoolbox.nutil.conn.workers.WorkCtrl;
import se.sics.ktoolbox.nutil.conn.workers.WorkCenterComp;
import se.sics.ktoolbox.nutil.conn.workers.WorkState;
import se.sics.ktoolbox.nutil.network.portsv2.MsgIdExtractorV2;
import se.sics.ktoolbox.nutil.network.portsv2.MsgIdExtractorsV2;
import se.sics.ktoolbox.nutil.network.portsv2.MsgTypeExtractorsV2;
import se.sics.ktoolbox.nutil.network.portsv2.OutgoingOne2NMsgChannelV2;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class HostComp extends ComponentDefinition {
  private final Init init;
  public HostComp(Init init) {
    this.init = init;
    subscribe(handleStart, control);
  }
  
  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      Component timer = create(JavaTimer.class, Init.NONE);
      Component networkEmulator = create(NetworkEmulator.class, Init.NONE);

      Map<String, MsgIdExtractorV2> channelSelectors = new HashMap<>();
      channelSelectors.put(ConnMsgs.CONNECTION, new MsgIdExtractorsV2.Destination<>());
      OutgoingOne2NMsgChannelV2 channel = OutgoingOne2NMsgChannelV2.getChannel("test-host-channel", logger,
        networkEmulator.getPositive(Network.class), new MsgTypeExtractorsV2.Base(), channelSelectors);

      ConnConfig connConfig = new ConnConfig(1000);
      Component ctrlCenter = create(MngrCenterComp.class, new MngrCenterComp.Init(init.ctrlCenterAdr, init.overlayId, 
        init.ctrlBatchId, init.workBatchId, init.baseId, connConfig, init.ctrlServerC));
      Component workCenter = create(WorkCenterComp.class, new WorkCenterComp.Init(init.workCenterAdr, init.overlayId, 
        init.ctrlBatchId, init.workBatchId, init.baseId, connConfig, init.ctrlCenterAdr, init.workServerC));
      
      channel.addChannel(init.ctrlCenterAdr.getId(), ctrlCenter.getNegative(Network.class));
      channel.addChannel(init.workCenterAdr.getId(), workCenter.getNegative(Network.class));
      
      connect(timer.getPositive(Timer.class), ctrlCenter.getNegative(Timer.class), Channel.TWO_WAY);
      connect(timer.getPositive(Timer.class), workCenter.getNegative(Timer.class), Channel.TWO_WAY);
      
      trigger(Start.event, timer.control());
      trigger(Start.event, networkEmulator.control());
      trigger(Start.event, ctrlCenter.control());
      trigger(Start.event, workCenter.control());
    }
  };
  
  public static class Init extends se.sics.kompics.Init<HostComp> {
    public final KAddress ctrlCenterAdr;
    public final KAddress workCenterAdr;
    public final Identifier overlayId;
    public final Identifier ctrlBatchId;
    public final Identifier workBatchId;
    public final Identifier baseId;
    public final MngrCtrl.Server<MngrState.Client> ctrlServerC;
    public final WorkCtrl.Server<WorkState.Client> workServerC;
    
    public Init(KAddress ctrlCenterAdr, KAddress workCenterAdr, Identifier overlayId, 
      Identifier ctrlBatchId, Identifier workBatchId, Identifier baseId,
      MngrCtrl.Server<MngrState.Client> ctrlServerC, WorkCtrl.Server<WorkState.Client> workServerC) {
      this.ctrlCenterAdr = ctrlCenterAdr;
      this.workCenterAdr = workCenterAdr;
      this.overlayId = overlayId;
      this.ctrlBatchId = ctrlBatchId;
      this.workBatchId = workBatchId;
      this.baseId = baseId;
      this.ctrlServerC = ctrlServerC;
      this.workServerC = workServerC;
    }
  }
}

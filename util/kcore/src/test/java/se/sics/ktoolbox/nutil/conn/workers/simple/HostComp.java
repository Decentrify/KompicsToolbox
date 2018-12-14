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
import se.sics.ktoolbox.nutil.conn.ConnMsgs;
import se.sics.ktoolbox.nutil.conn.util.NetworkEmulator;
import se.sics.ktoolbox.nutil.conn.workers.MngrCenterComp;
import se.sics.ktoolbox.nutil.conn.workers.WorkCenterComp;
import se.sics.ktoolbox.nutil.conn.workers.MngrCenterPort;
import se.sics.ktoolbox.nutil.conn.workers.WorkCenterPort;
import se.sics.ktoolbox.nutil.conn.workers.WorkMsgs;
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
      channelSelectors.put(ConnMsgs.MSG_TYPE, new MsgIdExtractorsV2.Destination<>());
      channelSelectors.put(WorkMsgs.MSG_TYPE, new MsgIdExtractorsV2.Destination<>());
      OutgoingOne2NMsgChannelV2 channel = OutgoingOne2NMsgChannelV2.getChannel("test-host-channel", logger,
        networkEmulator.getPositive(Network.class), new MsgTypeExtractorsV2.Base(), channelSelectors);

      ConnConfig connConfig = new ConnConfig(1000);
      Component workMngr = create(MngrCenterComp.class, new MngrCenterComp.Init(init.workMngrAdr, init.overlayId,
        init.batchId, init.baseId, connConfig));
      Component workCenter = create(WorkCenterComp.class, new WorkCenterComp.Init(init.workCenterAdr, init.overlayId,
        init.batchId, init.baseId, connConfig, init.workMngrAdr));

      channel.addChannel(init.workMngrAdr.getId(), workMngr.getNegative(Network.class));
      channel.addChannel(init.workCenterAdr.getId(), workCenter.getNegative(Network.class));

      connect(timer.getPositive(Timer.class), workMngr.getNegative(Timer.class), Channel.TWO_WAY);
      connect(timer.getPositive(Timer.class), workCenter.getNegative(Timer.class), Channel.TWO_WAY);

      Component workMngrDriver = create(WorkMngrDriverComp.class, new WorkMngrDriverComp.Init(init.workMngrAdr));
      connect(workMngr.getPositive(MngrCenterPort.class), workMngrDriver.getNegative(MngrCenterPort.class),
        Channel.TWO_WAY);

      Component workCenterDriver = create(WorkCenterDriverComp.class, new WorkCenterDriverComp.Init(init.workMngrAdr));
      connect(workCenter.getPositive(WorkCenterPort.class), workCenterDriver.getNegative(WorkCenterPort.class),
        Channel.TWO_WAY);
      connect(timer.getPositive(Timer.class), workCenterDriver.getNegative(Timer.class), Channel.TWO_WAY);

      trigger(Start.event, timer.control());
      trigger(Start.event, networkEmulator.control());
      trigger(Start.event, workMngr.control());
      trigger(Start.event, workCenter.control());
      trigger(Start.event, workMngrDriver.control());
      trigger(Start.event, workCenterDriver.control());
    }
  };

  public static class Init extends se.sics.kompics.Init<HostComp> {

    public final KAddress workMngrAdr;
    public final KAddress workCenterAdr;
    public final Identifier overlayId;
    public final Identifier batchId;
    public final Identifier baseId;

    public Init(KAddress workMngrAdr, KAddress workCenterAdr, Identifier overlayId,
      Identifier batchId, Identifier baseId) {
      this.workMngrAdr = workMngrAdr;
      this.workCenterAdr = workCenterAdr;
      this.overlayId = overlayId;
      this.batchId = batchId;
      this.baseId = baseId;
    }
  }
}

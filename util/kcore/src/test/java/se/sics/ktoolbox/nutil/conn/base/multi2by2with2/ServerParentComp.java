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
package se.sics.ktoolbox.nutil.conn.base.multi2by2with2;

import se.sics.ktoolbox.nutil.conn.base.util.ConnProxyMngrServerComp;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.conn.ConnIds;
import se.sics.ktoolbox.nutil.conn.base.util.BatchIdExtractors;
import se.sics.ktoolbox.nutil.conn.ConnMsgs;
import se.sics.ktoolbox.nutil.network.portsv2.MsgIdExtractorV2;
import se.sics.ktoolbox.nutil.network.portsv2.MsgTypeExtractorsV2;
import se.sics.ktoolbox.nutil.network.portsv2.OutgoingOne2NMsgChannelV2;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ServerParentComp extends ComponentDefinition {

  private Positive<Network> network = requires(Network.class);
  private Positive<Timer> timer = requires(Timer.class);

  private final Init init;

  public ServerParentComp(Init init) {
    this.init = init;
    subscribe(handleStart, control);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      Map<String, MsgIdExtractorV2> channelSelectors = new HashMap<>();
      channelSelectors.put(ConnMsgs.CONNECTION, new BatchIdExtractors.Server());
      OutgoingOne2NMsgChannelV2 channel = OutgoingOne2NMsgChannelV2.getChannel("test-server-channel", logger,
        network, new MsgTypeExtractorsV2.Base(), channelSelectors);

      ConnIds.InstanceId serverId1 
        = new ConnIds.InstanceId(init.overlayId, init.selfAddress.getId(), init.batchId1, init.baseId1, true);
      Component comp1 = create(ConnProxyMngrServerComp.class, new ConnProxyMngrServerComp.Init(init.selfAddress, Optional.of(serverId1)));
      Component comp2 = create(ConnProxyMngrServerComp.class, new ConnProxyMngrServerComp.Init(init.selfAddress, Optional.empty()));

      channel.addChannel(init.batchId1, comp1.getNegative(Network.class));
      channel.addChannel(init.batchId2, comp2.getNegative(Network.class));

      connect(timer, comp1.getNegative(Timer.class), Channel.TWO_WAY);
      connect(timer, comp2.getNegative(Timer.class), Channel.TWO_WAY);

      trigger(Start.event, comp1.control());
      trigger(Start.event, comp2.control());
    }
  };

  public static class Init extends se.sics.kompics.Init<ServerParentComp> {
    public final Identifier overlayId;
    public final KAddress selfAddress;
    public final Identifier batchId1;
    public final Identifier baseId1;
    public final Identifier batchId2;
    public final Identifier baseId2;

    public Init(Identifier overlayId, KAddress selfAddress,
      Identifier batchId1, Identifier baseId1,
      Identifier batchId2, Identifier baseId2) {
      this.overlayId = overlayId;
      this.selfAddress = selfAddress;
      this.batchId1 = batchId1;
      this.baseId1 = baseId1;
      this.batchId2 = batchId2;
      this.baseId2 = baseId2;
    }
  }
}

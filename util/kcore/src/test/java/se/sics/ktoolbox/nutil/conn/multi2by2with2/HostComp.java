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
package se.sics.ktoolbox.nutil.conn.multi2by2with2;

import se.sics.ktoolbox.nutil.conn.util.NetworkEmulator;
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
import se.sics.ktoolbox.nutil.conn.ConnIds;
import se.sics.ktoolbox.nutil.conn.ConnIds.InstanceId;
import se.sics.ktoolbox.nutil.conn.ConnMsgs;
import se.sics.ktoolbox.nutil.network.portsv2.MsgIdExtractorV2;
import se.sics.ktoolbox.nutil.network.portsv2.MsgIdExtractorsV2;
import se.sics.ktoolbox.nutil.network.portsv2.MsgTypeExtractorsV2;
import se.sics.ktoolbox.nutil.network.portsv2.OutgoingOne2NMsgChannelV2;
import se.sics.ktoolbox.util.network.KAddress;

/**
 *
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

      Component server1 = create(ServerParentComp.class, new ServerParentComp.Init(init.overlayId, init.serverAddress1, 
          init.serverBatch1, init.baseServerId1, init.serverBatch2, init.baseServerId2));
      Component server2 = create(ServerParentComp.class, new ServerParentComp.Init(init.overlayId, init.serverAddress2, 
          init.serverBatch3, init.baseServerId3, init.serverBatch4, init.baseServerId4));
      
      channel.addChannel(init.serverAddress1.getId(), server1.getNegative(Network.class));
      channel.addChannel(init.serverAddress2.getId(), server2.getNegative(Network.class));
      
      connect(timer.getPositive(Timer.class), server1.getNegative(Timer.class), Channel.TWO_WAY);
      connect(timer.getPositive(Timer.class), server2.getNegative(Timer.class), Channel.TWO_WAY);
      
      InstanceId serverId1 = new ConnIds.InstanceId(init.overlayId, init.serverAddress1.getId(), 
        init.serverBatch1, init.baseServerId1, true);
      InstanceId serverId2 = new ConnIds.InstanceId(init.overlayId, init.serverAddress1.getId(), 
        init.serverBatch2, init.baseServerId2, true);
      InstanceId serverId3 = new ConnIds.InstanceId(init.overlayId, init.serverAddress2.getId(), 
        init.serverBatch3, init.baseServerId3, true);
      InstanceId serverId4 = new ConnIds.InstanceId(init.overlayId, init.serverAddress2.getId(), 
        init.serverBatch4, init.baseServerId4, true);
      
      Component client1 = create(ClientParentComp.class, new ClientParentComp.Init(init.overlayId, init.clientAddress1,
          serverId1, init.serverAddress1, serverId3, init.serverAddress2));
      Component client2 = create(ClientParentComp.class, new ClientParentComp.Init(init.overlayId, init.clientAddress2,
          serverId2, init.serverAddress1, serverId4, init.serverAddress2));
      
      channel.addChannel(init.clientAddress1.getId(), client1.getNegative(Network.class));
      channel.addChannel(init.clientAddress2.getId(), client2.getNegative(Network.class));
      
      connect(timer.getPositive(Timer.class), client1.getNegative(Timer.class), Channel.TWO_WAY);
      connect(timer.getPositive(Timer.class), client2.getNegative(Timer.class), Channel.TWO_WAY);

      trigger(Start.event, timer.control());
      trigger(Start.event, networkEmulator.control());
      trigger(Start.event, server1.control());
      trigger(Start.event, server2.control());
      trigger(Start.event, client1.control());
      trigger(Start.event, client2.control());
    }
  };
  
  public static class Init extends se.sics.kompics.Init<HostComp> {
    public final Identifier overlayId;
    public final KAddress serverAddress1;
    public final KAddress serverAddress2;
    public final Identifier serverBatch1;
    public final Identifier baseServerId1;
    public final Identifier serverBatch2;
    public final Identifier baseServerId2;
    public final Identifier serverBatch3;
    public final Identifier baseServerId3;
    public final Identifier serverBatch4;
    public final Identifier baseServerId4;
    
    public final KAddress clientAddress1;
    public final KAddress clientAddress2;
    
    public Init(Identifier overlayId, KAddress serverAddress1, KAddress serverAddress2, 
      Identifier serverBatch1, Identifier baseServerId1, 
      Identifier serverBatch2, Identifier baseServerId2,
      Identifier serverBatch3, Identifier baseServerId3,
      Identifier serverBatch4, Identifier baseServerId4,
      KAddress clientAddress1, KAddress clientAddress2) {
      this.overlayId = overlayId;
      this.serverAddress1 = serverAddress1;
      this.serverAddress2 = serverAddress2;
      this.serverBatch1 = serverBatch1;
      this.baseServerId1 = baseServerId1;
      this.serverBatch2 = serverBatch2;
      this.baseServerId2 = baseServerId2;
      this.serverBatch3 = serverBatch3;
      this.baseServerId3 = baseServerId3;
      this.serverBatch4 = serverBatch4;
      this.baseServerId4 = baseServerId4;
      this.clientAddress1 = clientAddress1;
      this.clientAddress2 = clientAddress2;
    }
  }
}

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
package se.sics.ktoolbox.nutil.conn.base.simpleProxy;

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
import se.sics.ktoolbox.nutil.conn.base.util.ConnProxyClientComp;
import se.sics.ktoolbox.nutil.conn.base.util.ConnProxyServerComp;
import se.sics.ktoolbox.nutil.network.portsv2.MsgIdExtractorV2;
import se.sics.ktoolbox.nutil.network.portsv2.MsgIdExtractorsV2;
import se.sics.ktoolbox.nutil.network.portsv2.MsgTypeExtractorsV2;
import se.sics.ktoolbox.nutil.network.portsv2.OutgoingOne2NMsgChannelV2;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
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
      channelSelectors.put(ConnMsgs.MSG_TYPE, new MsgIdExtractorsV2.Destination<>());
      OutgoingOne2NMsgChannelV2 channel = OutgoingOne2NMsgChannelV2.getChannel("test-host-channel", logger,
        networkEmulator.getPositive(Network.class), new MsgTypeExtractorsV2.Base(), channelSelectors);

      InstanceId serverId = new ConnIds.InstanceId(init.overlayId, init.serverAddress.getId(), 
        init.serverBatch, init.serverBaseId, true);
      
      Component server = create(ConnProxyServerComp.class, new ConnProxyServerComp.Init(init.serverAddress, serverId));
      channel.addChannel(init.serverAddress.getId(), server.getNegative(Network.class));
      connect(timer.getPositive(Timer.class), server.getNegative(Timer.class), Channel.TWO_WAY);
      
      Component client = create(ConnProxyClientComp.class, new ConnProxyClientComp.Init(init.overlayId, 
        IdentifierRegistryV2.connBatchId(), init.clientAddress, 
        serverId, init.serverAddress));
      channel.addChannel(init.clientAddress.getId(), client.getNegative(Network.class));
      connect(timer.getPositive(Timer.class), client.getNegative(Timer.class), Channel.TWO_WAY);

      trigger(Start.event, timer.control());
      trigger(Start.event, networkEmulator.control());
      trigger(Start.event, server.control());
      trigger(Start.event, client.control());
    }
  };
  
  public static class Init extends se.sics.kompics.Init<HostComp> {
    public final Identifier overlayId;
    public final KAddress serverAddress;
    public final Identifier serverBatch;
    public final Identifier serverBaseId;
    
    public final KAddress clientAddress;
    
    public Init(Identifier overlayId, KAddress serverAddress, KAddress clientAddress,
      Identifier serverBatch, Identifier serverBaseId) { 
      this.overlayId = overlayId;
      this.serverAddress = serverAddress;
      this.clientAddress = clientAddress;
      this.serverBatch = serverBatch;
      this.serverBaseId = serverBaseId;
    }
  }
}

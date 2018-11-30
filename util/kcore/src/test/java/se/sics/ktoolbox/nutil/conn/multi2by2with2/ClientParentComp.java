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

import se.sics.ktoolbox.nutil.conn.util.ClientComp;
import java.util.HashMap;
import java.util.Map;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.conn.util.BatchIdExtractors;
import se.sics.ktoolbox.nutil.conn.ConnIds.InstanceId;
import se.sics.ktoolbox.nutil.conn.ConnMsgs;
import se.sics.ktoolbox.nutil.network.portsv2.MsgIdExtractorV2;
import se.sics.ktoolbox.nutil.network.portsv2.MsgTypeExtractorsV2;
import se.sics.ktoolbox.nutil.network.portsv2.OutgoingOne2NMsgChannelV2;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ClientParentComp extends ComponentDefinition {

  private Positive<Network> network = requires(Network.class);
  private Positive<Timer> timer = requires(Timer.class);

  private final Init init;

  public ClientParentComp(Init init) {
    this.init = init;
    subscribe(handleStart, control);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      
      
      Map<String, MsgIdExtractorV2> channelSelectors = new HashMap<>();
      channelSelectors.put(ConnMsgs.CONNECTION, new BatchIdExtractors.Client());
      OutgoingOne2NMsgChannelV2 channel = OutgoingOne2NMsgChannelV2.getChannel("test-client-channel", logger,
        network, new MsgTypeExtractorsV2.Base(), channelSelectors);

      Identifier batchId1 = IdentifierRegistryV2.connBatchId();
      Identifier batchId2 = IdentifierRegistryV2.connBatchId();
      Component comp1 = create(ClientComp.class, new ClientComp.Init(batchId1,init.selfAddress, 
        init.serverId1, init.serverAddress1));
      Component comp2 = create(ClientComp.class, new ClientComp.Init(batchId2, init.selfAddress, 
        init.serverId2, init.serverAddress2));

      channel.addChannel(batchId1, comp1.getNegative(Network.class));
      channel.addChannel(batchId2, comp2.getNegative(Network.class));
      
      connect(timer, comp1.getNegative(Timer.class), Channel.TWO_WAY);
      connect(timer, comp2.getNegative(Timer.class), Channel.TWO_WAY);

      trigger(Start.event, comp1.control());
      trigger(Start.event, comp2.control());
    }
  };

  public static class Init extends se.sics.kompics.Init<ClientParentComp> {

    public final KAddress selfAddress;

    public final InstanceId serverId1;
    public final KAddress serverAddress1;
    public final InstanceId serverId2;
    public final KAddress serverAddress2;

    public Init(KAddress selfAddress,
      InstanceId serverId1, KAddress serverAddress1,
      InstanceId serverId2, KAddress serverAddress2) {
      this.selfAddress = selfAddress;
      this.serverId1 = serverId1;
      this.serverAddress1 = serverAddress1;
      this.serverId2 = serverId2;
      this.serverAddress2 = serverAddress2;
    }
  }
}

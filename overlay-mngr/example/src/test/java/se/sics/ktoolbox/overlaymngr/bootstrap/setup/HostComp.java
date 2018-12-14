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
package se.sics.ktoolbox.overlaymngr.bootstrap.setup;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.conn.ConnMsgs;
import se.sics.ktoolbox.nutil.network.portsv2.MsgIdExtractorV2;
import se.sics.ktoolbox.nutil.network.portsv2.MsgIdExtractorsV2;
import se.sics.ktoolbox.nutil.network.portsv2.MsgTypeExtractorsV2;
import se.sics.ktoolbox.nutil.network.portsv2.OutgoingOne2NMsgChannelV2;
import se.sics.ktoolbox.omngr.bootstrap.BootstrapClientComp;
import se.sics.ktoolbox.omngr.bootstrap.BootstrapClientPort;
import se.sics.ktoolbox.omngr.bootstrap.BootstrapServerComp;
import se.sics.ktoolbox.overlaymngr.bootstrap.util.NetworkEmulator;
import se.sics.ktoolbox.util.identifiable.BasicBuilders;
import se.sics.ktoolbox.util.identifiable.basic.IntIdFactory;
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

      IntIdFactory intIdFactory = new IntIdFactory(Optional.of(1234l));
      Identifier overlayBootstrapConnBatchId = intIdFactory.id(new BasicBuilders.IntBuilder(0));
      Identifier overlayBootstrapConnBaseId = intIdFactory.id(new BasicBuilders.IntBuilder(1));
      Component server = create(BootstrapServerComp.class, 
        new BootstrapServerComp.Init(init.serverAddress, overlayBootstrapConnBatchId, overlayBootstrapConnBaseId));
      channel.addChannel(init.serverAddress.getId(), server.getNegative(Network.class));
      connect(timer.getPositive(Timer.class), server.getNegative(Timer.class), Channel.TWO_WAY);

      Component client = create(BootstrapClientComp.class, new BootstrapClientComp.Init(init.clientAddress,
        overlayBootstrapConnBatchId, overlayBootstrapConnBaseId));
      channel.addChannel(init.clientAddress.getId(), client.getNegative(Network.class));
      connect(timer.getPositive(Timer.class), client.getNegative(Timer.class), Channel.TWO_WAY);

      Component driver = create(DriverComp.class, new DriverComp.Init(init.overlayId1, init.overlayId2));
      connect(timer.getPositive(Timer.class), driver.getNegative(Timer.class), Channel.TWO_WAY);
      connect(client.getPositive(BootstrapClientPort.class), driver.getNegative(BootstrapClientPort.class), 
        Channel.TWO_WAY);
      
      trigger(Start.event, timer.control());
      trigger(Start.event, networkEmulator.control());
      trigger(Start.event, server.control());
      trigger(Start.event, client.control());
      trigger(Start.event, driver.control());
    }
  };

  public static class Init extends se.sics.kompics.Init<HostComp> {

    public final KAddress serverAddress;
    public final KAddress clientAddress;
    public final Identifier overlayId1;
    public final Identifier overlayId2;

    public Init(KAddress serverAddress, KAddress clientAddress, Identifier overlayId1, Identifier overlayId2) {
      this.serverAddress = serverAddress;
      this.clientAddress = clientAddress;
      this.overlayId1 = overlayId1;
      this.overlayId2 = overlayId2;
    }
  }
}

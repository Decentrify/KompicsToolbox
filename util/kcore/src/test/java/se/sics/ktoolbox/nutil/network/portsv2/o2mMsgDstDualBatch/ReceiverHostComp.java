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
package se.sics.ktoolbox.nutil.network.portsv2.o2mMsgDstDualBatch;

import java.util.HashMap;
import java.util.Map;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.ktoolbox.nutil.network.portsv2.MsgIdExtractorV2;
import se.sics.ktoolbox.nutil.network.portsv2.MsgIdExtractorsV2;
import se.sics.ktoolbox.nutil.network.portsv2.MsgTypeExtractorsV2;
import se.sics.ktoolbox.nutil.network.portsv2.OutgoingOne2NMsgChannelV2;
import se.sics.ktoolbox.nutil.network.portsv2.util.ReceiverComp;
import se.sics.ktoolbox.nutil.network.portsv2.util.TestMsgs;
import se.sics.ktoolbox.util.network.KAddress;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ReceiverHostComp extends ComponentDefinition {

  private final Init init;

  public ReceiverHostComp(Init init) {
    this.init = init;
    subscribe(handleStart, control);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      Component driver = create(DriverComp.class, new DriverComp.Init(init.self, init.dst1, init.dst2));

      Map<String, MsgIdExtractorV2> channelSelectors1 = new HashMap<>();
      channelSelectors1.put(TestMsgs.MSG1, new MsgIdExtractorsV2.Destination<>());
      OutgoingOne2NMsgChannelV2 channel1 = OutgoingOne2NMsgChannelV2.getChannel("test-channel1", logger,
        driver.getPositive(Network.class), new MsgTypeExtractorsV2.Base(), channelSelectors1);

      Component receiver1 = create(ReceiverComp.class, new ReceiverComp.Init(init.dst1.getId(), "1"));
      channel1.addChannel(init.dst1.getId(), receiver1.getNegative(Network.class));
      Component receiver2 = create(ReceiverComp.class, new ReceiverComp.Init(init.dst2.getId(), "2"));
      channel1.addChannel(init.dst2.getId(), receiver2.getNegative(Network.class));
      
      Component receiver3 = create(ReceiverComp.class, new ReceiverComp.Init(init.dst1.getId(), "3"));
      channel1.addChannel(init.dst1.getId(), receiver3.getNegative(Network.class));
      Component receiver4 = create(ReceiverComp.class, new ReceiverComp.Init(init.dst2.getId(), "4"));
      channel1.addChannel(init.dst2.getId(), receiver4.getNegative(Network.class));

      trigger(Start.event, driver.control());
      trigger(Start.event, receiver1.control());
      trigger(Start.event, receiver2.control());
      trigger(Start.event, receiver3.control());
      trigger(Start.event, receiver4.control());
    }
  };

  public static class Init extends se.sics.kompics.Init<ReceiverHostComp> {

    public final KAddress self;
    public final KAddress dst1;
    public final KAddress dst2;

    public Init(KAddress self, KAddress dst1, KAddress dst2) {
      this.self = self;
      this.dst1 = dst1;
      this.dst2 = dst2;
    }
  }
}

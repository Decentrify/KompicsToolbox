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
package se.sics.ktoolbox.nutil.network.portsv2.oMsgFilter;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.Start;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.ktoolbox.nutil.network.portsv2.util.TestMsgs;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class DriverComp extends ComponentDefinition {

  Negative<Network> network = provides(Network.class);
  private final Init init;
  public DriverComp(Init init) {
    this.init = init;
    subscribe(handleStart, control);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      test();
    }
  };

  private void test() {
    trigger(msg(new TestMsgs.Msg1()), network);
    trigger(msg(new TestMsgs.Msg2()), network);
    trigger(msg(new TestMsgs.Msg1()), network);
    trigger(msg(new TestMsgs.Msg2()), network);
  }

  private Msg msg(KompicsEvent content) {
    KHeader header = new BasicHeader(init.self, init.self, Transport.UDP);
    Msg msg = new BasicContentMsg(header, content);
    return msg;
  }

  public static class Init extends se.sics.kompics.Init<DriverComp> {

    public final KAddress self;
    
    public Init(KAddress self) {
      this.self = self;
    }
  }
}

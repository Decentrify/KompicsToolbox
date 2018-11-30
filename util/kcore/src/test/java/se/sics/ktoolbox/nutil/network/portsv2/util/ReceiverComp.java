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
package se.sics.ktoolbox.nutil.network.portsv2.util;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Network;
import se.sics.kompics.util.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ReceiverComp extends ComponentDefinition {
  Positive<Network> network = requires(Network.class);
  
  private final Init init;
  public ReceiverComp(Init init) {
    this.init = init;
    subscribe(handleMsg, network);
  }
  
  Handler handleMsg = new Handler<Msg>() {
    @Override
    public void handle(Msg msg) {
      logger.info("{} expected dst:{} received:{}", new Object[]{init.receiver, init.expectedDst,msg});
    }
  };
  
  public static class Init extends se.sics.kompics.Init<ReceiverComp> {
    public final Identifier expectedDst;
    public final String receiver;
    public Init(Identifier expectedDst, String receiver) {
      this.expectedDst = expectedDst;
      this.receiver = receiver;
    }
  }
}

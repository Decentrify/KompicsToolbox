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
package se.sics.ktoolbox.nledbat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nledbat.event.internal.NLedbatMsg;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NLedbatReceiverComp extends ComponentDefinition {

  private static final Logger LOG = LoggerFactory.getLogger(NLedbatReceiverComp.class);
  private String logPrefix = "";

  Negative<Network> incomingNetworkPort = provides(Network.class);
  Positive<Network> outgoingNetworkPort = requires(Network.class);

  private final Identifier dataId;
  private final Identifier senderId;
  private final Identifier receiverId;

  public NLedbatReceiverComp(NLedbatSenderComp.Init init) {
    dataId = init.dataId;
    senderId = init.senderId;
    receiverId = init.receiverId;
    logPrefix = "<" + dataId + "," + senderId + "," + receiverId + ">";

    subscribe(handleStart, control);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      LOG.info("{}starting...", logPrefix);
    }
  };

  @Override
  public void tearDown() {
  }

  ClassMatchedHandler handleIncomingMsg
    = new ClassMatchedHandler<NLedbatMsg.Data, BasicContentMsg<?, ?, NLedbatMsg.Data>>() {

      @Override
      public void handle(NLedbatMsg.Data payload, BasicContentMsg<?, ?, NLedbatMsg.Data> msg) {
        BasicContentMsg baseMsg = new BasicContentMsg(msg.getHeader(), payload.data);
        trigger(baseMsg, incomingNetworkPort);
        trigger(msg.answer(payload.answer()), outgoingNetworkPort);
      }
    };

  public static class Init extends se.sics.kompics.Init<NLedbatReceiverComp> {

    public final Identifier dataId;
    public final Identifier senderId;
    public final Identifier receiverId;

    public Init(Identifier dataId, Identifier senderId, Identifier receiverId) {
      this.dataId = dataId;
      this.senderId = senderId;
      this.receiverId = receiverId;
    }
  }
}

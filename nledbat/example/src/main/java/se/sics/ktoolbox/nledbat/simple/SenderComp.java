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
package se.sics.ktoolbox.nledbat.simple;

import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.nledbat.NLedbat;
import se.sics.ktoolbox.nledbat.NLedbatSenderComp;
import se.sics.ktoolbox.nledbat.NLedbatSenderComp.HardCodedConfig;
import se.sics.ktoolbox.nledbat.NLedbatSenderCtrl;
import se.sics.ktoolbox.nledbat.event.external.NLedbatEvents;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SenderComp extends ComponentDefinition {

  private Logger LOG = LoggerFactory.getLogger(SenderComp.class);
  private String logPrefix = "";

  private Positive<Network> network = requires(Network.class);
  private Positive<NLedbatSenderCtrl> ledbatCtrl = requires(NLedbatSenderCtrl.class);
  private KAddress self;
  private KAddress receiver;
  private Random rand = new Random();
  
  private int send = 1024*1024; //1GB

  public SenderComp(Init init) {
    this.self = init.self;
    this.receiver = init.receiver;

    subscribe(handleStart, control);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
    }
  };

  Handler handleLedbatStatus = new Handler<NLedbatEvents.SenderStatus>() {
    @Override
    public void handle(NLedbatEvents.SenderStatus event) {
      double aux = HardCodedConfig.statusPeriod / event.rto;
      long dataSize = (long)(aux*(event.cwndSize - 1024 * event.bufferedMsgs));
      LOG.info("rto:{} cwnd:{} buffered:{} aux:{} dataSize:{}", 
        new Object[]{event.rto, event.cwndSize, event.bufferedMsgs, aux, dataSize});
    }
  };

  private void sendData(long dataSize) {
    while (dataSize > 0 && send > 0) {
      byte[] content = new byte[1024];
      rand.nextBytes(content);
      sendNetwork(new SimpleContent(content));
      dataSize -= 1024;
      send--;
    }
    LOG.info("{}time:{}, left:{}", new Object[]{logPrefix, System.currentTimeMillis(), send});
  }

  private void sendNetwork(SimpleContent content) {
    KHeader header = new BasicHeader(self, receiver, Transport.TCP);
    BasicContentMsg msg = new BasicContentMsg(header, content);
    trigger(msg, network);
  }

  public static class Init extends se.sics.kompics.Init<SenderComp> {

    public final KAddress self;
    public final KAddress receiver;

    public Init(KAddress self, KAddress receiver) {
      this.self = self;
      this.receiver = receiver;
    }
  }

  public static class SendTimeout extends Timeout {

    public SendTimeout(SchedulePeriodicTimeout spt) {
      super(spt);
    }
  }
}

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
package se.sics.ktoolbox.netmngr.core;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ktoolbox.util.network.other.Chunkable;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TestComp extends ComponentDefinition {
  
  private static final Logger LOG = LoggerFactory.getLogger(TestComp.class);
  private String logPrefix = " ";

  //*****************************CONNECTIONS**********************************
  private final Positive<Network> networkPort = requires(Network.class);
  private final Positive<Timer> timerPort = requires(Timer.class);
  //*****************************CONFIGURATION********************************
  private SystemKCWrapper systemConfig;
  //****************************EXTERNAL_STATE********************************
  private KAddress selfAdr;
  private final KAddress partnerAdr;
  private final OverlayId overlayId;
  //****************************INTERNAL_STATE********************************
  private int counter = 0;
  //********************************AUX***************************************
  private UUID periodicDataTId;
  private final IdentifierFactory msgIds;
  
  public TestComp(Init init) {
    systemConfig = new SystemKCWrapper(config());
    overlayId = init.overlayId;
    logPrefix = "<nid:" + systemConfig.id + ", oid:" + overlayId + "> ";
    LOG.info("{}initiating...", logPrefix);
    
    partnerAdr = init.partnerAdr;
    this.msgIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.MSG, Optional.of(systemConfig.seed));
    subscribe(handleStart, control);
    subscribe(handleTimeout, timerPort);
    subscribe(handleData, networkPort);
    subscribe(handleChunkableData, networkPort);
    subscribe(handleAck, networkPort);
  }
  
  private boolean ready() {
    return true;
  }
  //****************************CONTROL***************************************
  private Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      LOG.info("{}initiating...", logPrefix);
      scheduleDataTimeout(selfAdr);
    }
  };

  //**************************************************************************
  private Handler handleTimeout = new Handler<DataTimeout>() {
    @Override
    public void handle(DataTimeout timeout) {
      LOG.trace("{}{}", new Object[]{logPrefix, timeout});
      KHeader header;
      KContentMsg msg;
      
      counter++;
      header = new BasicHeader(selfAdr, partnerAdr, Transport.UDP);
      msg = new BasicContentMsg(header, new Data(msgIds.randomId(), overlayId, counter));
      LOG.trace("{}sending nr:{} {}", new Object[]{logPrefix, counter, msg});
      trigger(msg, networkPort);
      
      Random rand = new Random();
      byte[] data = new byte[2000];
      rand.nextBytes(data);
      header = new BasicHeader(selfAdr, partnerAdr, Transport.UDP);
      msg = new BasicContentMsg(header, new ChunkableData(msgIds.randomId(), overlayId, counter, ByteBuffer.wrap(data)));
      LOG.trace("{}sending:{}", new Object[]{logPrefix, msg});
      trigger(msg, networkPort);
    }
  };
  
  private ClassMatchedHandler handleData = new ClassMatchedHandler<Data, KContentMsg<?, ?, Data>>() {
    @Override
    public void handle(Data content, KContentMsg msg) {
      LOG.trace("{}received:{}", new Object[]{logPrefix, msg});
      KContentMsg reply = msg.answer(content.answer());
      LOG.trace("{}sending nr:{} {}", new Object[]{logPrefix, content.counter, reply});
      trigger(reply, networkPort);
    }
  };
  
  private ClassMatchedHandler handleChunkableData = new ClassMatchedHandler<Chunkable, KContentMsg<?, ?, Chunkable>>() {
    @Override
    public void handle(Chunkable content, KContentMsg msg) {
      ChunkableData myContent = (ChunkableData) content;
      LOG.trace("{}received:{} {}", new Object[]{logPrefix, myContent.counter, msg});
      KContentMsg reply = msg.answer(myContent.answer());
      LOG.trace("{}sending:{} {}", new Object[]{logPrefix, myContent.counter, reply});
      trigger(reply, networkPort);
    }
  };
  
  private ClassMatchedHandler handleAck = new ClassMatchedHandler<Ack, KContentMsg<?, ?, Ack>>() {
    @Override
    public void handle(Ack content, KContentMsg<?, ?, Ack> msg) {
      LOG.trace("{}received:{} {}", new Object[]{logPrefix, content.counter, msg});
    }
  };
  
  public static class Init extends se.sics.kompics.Init<TestComp> {
    
    public final KAddress partnerAdr;
    public final OverlayId overlayId;
    
    public Init(KAddress partner, OverlayId overlayId) {
      this.partnerAdr = partner;
      this.overlayId = overlayId;
    }
  }
  
  private void scheduleDataTimeout(KAddress dest) {
    if (periodicDataTId != null) {
      LOG.warn("{} double starting shuffle timeout", logPrefix);
      return;
    }
    SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(1000, 1000);
    DataTimeout sc = new DataTimeout(spt, dest);
    spt.setTimeoutEvent(sc);
    periodicDataTId = sc.getTimeoutId();
    trigger(spt, timerPort);
  }
  
  private void cancelDataTimeout() {
    if (periodicDataTId == null) {
      return;
    }
    CancelTimeout cpt = new CancelTimeout(periodicDataTId);
    periodicDataTId = null;
    trigger(cpt, timerPort);
    
  }
  
  private static class DataTimeout extends Timeout {
    
    public final KAddress dest;
    
    DataTimeout(SchedulePeriodicTimeout request, KAddress dest) {
      super(request);
      this.dest = dest;
    }
    
    @Override
    public String toString() {
      return "DataTimeout<" + getTimeoutId() + ">";
    }
  }
}

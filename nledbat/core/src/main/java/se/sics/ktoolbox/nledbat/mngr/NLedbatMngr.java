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
package se.sics.ktoolbox.nledbat.mngr;

import java.util.Optional;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nledbat.NLedbat;
import se.sics.ktoolbox.nledbat.NLedbatSenderComp;
import se.sics.ktoolbox.nledbat.NLedbatSenderCtrl;
import se.sics.ktoolbox.util.mngrcomp.util.CompSetup;
import se.sics.ktoolbox.util.mngrcomp.util.PortsSetup;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.ports.ChannelIdExtractor;
import se.sics.ktoolbox.util.network.ports.One2NChannel;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NLedbatMngr {

  public static class Ports implements PortsSetup {
    public Negative<NLedbatSenderCtrl> senderCtrl;
    public One2NChannel senderTimer;
    public One2NChannel senderProxyNet;
    public One2NChannel senderNet;
    public One2NChannel receiverProxyNet;
    public One2NChannel receiverNet;
    
    @Override
    public void setup(ComponentProxy proxy) {
      Positive<Timer> timer = proxy.requires(Timer.class);
      senderTimer = One2NChannel.getChannel("nledbat mngr sender timer", timer, new SenderTimerIdExtractor());
      Positive<Network> network = proxy.requires(Network.class);
      senderNet = One2NChannel.getChannel("nledbat mngr sender net", network, new SenderNetIdExtractor());
      receiverNet = One2NChannel.getChannel("nledbat mngr receiver net", network, new ReceiverNetIdExtractor());
      Negative<Network> proxyNetwork = proxy.provides(Network.class);
      senderProxyNet = One2NChannel.getChannel("nledbat mngr sender proxy net", proxyNetwork, 
        new SenderProxyNetIdExtractor());
      senderProxyNet = One2NChannel.getChannel("nledbat mngr sender proxy net", proxyNetwork, 
        new SenderProxyNetIdExtractor());
      senderCtrl = proxy.provides(NLedbatSenderCtrl.class);
    }
  }

  public static class SenderComp implements CompSetup<Ports> {

    @Override
    public Component createAndStart(ComponentProxy proxy, Ports ports, Identifier transferId, Init init) {
      Component comp = proxy.create(NLedbatSenderComp.class, init);
      ports.senderTimer.addChannel(transferId, comp.getNegative(Timer.class));
      ports.senderNet.addChannel(transferId, comp.getNegative(Network.class));
      ports.senderProxyNet.addChannel(transferId, comp.getPositive(Network.class));
      proxy.connect(comp.getPositive(NLedbatSenderCtrl.class), ports.senderCtrl, Channel.TWO_WAY);
      proxy.trigger(Start.event, comp.control());
      return comp;
    }

    @Override
    public void destroyAndKill(ComponentProxy proxy, Ports ports, Identifier transferIf, Component comp) {
      
      ports.senderTimer.removeChannel(null, null);
    }
  }
  
  public static class SenderTimerIdExtractor extends ChannelIdExtractor<Timeout, Identifier> {

    public SenderTimerIdExtractor() {
      super(Timeout.class);
    }
    
    @Override
    public Identifier getValue(Timeout timeout) {
      if(timeout instanceof NLedbatSenderComp.PeriodicTimeout) {
        return ((NLedbatSenderComp.PeriodicTimeout)timeout).transferId();
      } else {
        return null;
      }
    }
  }
  
  public static abstract class TransferIdExtractor extends ChannelIdExtractor<BasicContentMsg, Identifier> {
    public TransferIdExtractor() {
      super(BasicContentMsg.class);
    }
    
    @Override
    public Identifier getValue(BasicContentMsg msg) {
      Optional<Identifier> dataId = NLedbat.dataId(msg);
      if(dataId.isPresent()) {
        return getTransferId(dataId.get(), msg.getSource().getId(), msg.getDestination().getId());
      } else {
        return null;
      }
    }
    
    public abstract Identifier getTransferId(Identifier dataId, Identifier srcId, Identifier dstId);
  }
  
  public static class SenderNetIdExtractor extends TransferIdExtractor {

    @Override
    public Identifier getTransferId(Identifier dataId, Identifier srcId, Identifier dstId) {
      return NLedbat.senderTransferId(dataId, dstId, srcId);
    }
  }
  
  public static class SenderProxyNetIdExtractor extends TransferIdExtractor {

    @Override
    public Identifier getTransferId(Identifier dataId, Identifier srcId, Identifier dstId) {
      return NLedbat.senderTransferId(dataId, srcId, dstId);
    }
  }
  
  public static class ReceiverNetIdExtractor extends TransferIdExtractor {

    @Override
    public Identifier getTransferId(Identifier dataId, Identifier srcId, Identifier dstId) {
      return NLedbat.senderTransferId(dataId, srcId, dstId);
    }
  }
  
  public static class ReceiverProxyNetIdExtractor extends TransferIdExtractor {

    @Override
    public Identifier getTransferId(Identifier dataId, Identifier srcId, Identifier dstId) {
      return NLedbat.senderTransferId(dataId, dstId, srcId);
    }
  }
}

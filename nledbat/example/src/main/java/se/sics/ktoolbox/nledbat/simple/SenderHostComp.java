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

import com.google.common.base.Optional;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Channel;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Kompics;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.config.Config;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.netmngr.NetworkMngrSerializerSetup;
import se.sics.ktoolbox.netmngr.event.NetMngrReady;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;
import se.sics.ktoolbox.util.status.Status;
import se.sics.nat.stun.StunSerializerSetup;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nledbat.NLedbatReceiverComp;
import se.sics.ktoolbox.nledbat.NLedbatSenderComp;
import se.sics.ktoolbox.nledbat.NLedbatSenderCtrl;
import se.sics.ktoolbox.util.identifiable.BasicBuilders;
import se.sics.ktoolbox.util.identifiable.basic.IntIdFactory;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicAddress;
import se.sics.ktoolbox.util.status.StatusPort;
import se.sics.nat.mngr.SimpleNatMngrComp;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SenderHostComp extends ComponentDefinition {

  private Logger LOG = LoggerFactory.getLogger(SenderHostComp.class);
  private String logPrefix = "";

  private Positive<StatusPort> networkMngrStatus = requires(StatusPort.class);
  private Component timerComp;
  private Component networkMngrComp;
  private Component ledbatSenderComp;
  private Component appComp;
  private KAddress selfAdr;
  private KAddress receiverAdr;
  private Identifier senderId;
  private Identifier receiverId;
  private Identifier dataId;

  public SenderHostComp() {
    readConfig();
    
    subscribe(handleStart, control);
    subscribe(handleNetReady, networkMngrStatus);
  }
  
  private void readConfig() {
    IntIdFactory ids = new IntIdFactory(new Random());
    dataId = ids.id(new BasicBuilders.IntBuilder(config().getValue("transfer.dataId", Integer.class)));
    senderId = ids.id(new BasicBuilders.IntBuilder(config().getValue("system.id", Integer.class)));
    receiverId = ids.id(new BasicBuilders.IntBuilder(config().getValue("transfer.receiverId", Integer.class)));
    int receiverPort = config().getValue("transfer.receiverPort", Integer.class);
    InetAddress receiverIp;
    try {
      receiverIp = InetAddress.getByName(config().getValue("transfer.receiverIp", String.class));
    } catch (UnknownHostException ex) {
      throw new RuntimeException(ex);
    }
    receiverAdr = new BasicAddress(receiverIp, receiverPort, receiverId);
  }
  
  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      LOG.info("{}starting", logPrefix);

      timerComp = create(JavaTimer.class, Init.NONE);
      setNetworkMngr();

      trigger(Start.event, timerComp.control());
      trigger(Start.event, networkMngrComp.control());
    }
  };

  private void setNetworkMngr() {
    LOG.info("{}setting up network mngr", logPrefix);
    SimpleNatMngrComp.ExtPort netExtPorts = new SimpleNatMngrComp.ExtPort(timerComp.getPositive(Timer.class));
    networkMngrComp = create(SimpleNatMngrComp.class, new SimpleNatMngrComp.Init(netExtPorts));
    connect(networkMngrComp.getPositive(StatusPort.class), networkMngrStatus.getPair(), Channel.TWO_WAY);
  }

  ClassMatchedHandler handleNetReady
    = new ClassMatchedHandler<NetMngrReady, Status.Internal<NetMngrReady>>() {
    @Override
    public void handle(NetMngrReady content,
      Status.Internal<NetMngrReady> container) {
      LOG.info("{}network mngr ready", logPrefix);
      selfAdr = content.systemAdr;

      setNetworkStack();
      setApp();
      LOG.info("{} sender:{} receiver:{}", new Object[]{logPrefix, selfAdr, receiverAdr});
    }
  };

  private void setNetworkStack() {
    NLedbatSenderComp.Init init = new NLedbatSenderComp.Init(dataId, senderId, receiverId);
    ledbatSenderComp = create(NLedbatSenderComp.class, init);
    connect(ledbatSenderComp.getNegative(Network.class), networkMngrComp.getPositive(Network.class), Channel.TWO_WAY);
    trigger(Start.event, ledbatSenderComp.control());
  }
  
  private void setApp() {
    SenderComp.Init init = new SenderComp.Init(selfAdr, receiverAdr);
    appComp = create(SenderComp.class, init);
    connect(appComp.getNegative(Network.class), ledbatSenderComp.getPositive(Network.class), Channel.TWO_WAY);
    connect(appComp.getNegative(NLedbatSenderCtrl.class), ledbatSenderComp.getPositive(NLedbatSenderCtrl.class), 
      Channel.TWO_WAY);
    trigger(Start.event, appComp.control());
  }

  private static void setupSerializers() {
    int serializerId = 0;
    serializerId = BasicSerializerSetup.registerBasicSerializers(serializerId);
    serializerId = NetworkMngrSerializerSetup.registerSerializers(serializerId);
    serializerId = StunSerializerSetup.registerSerializers(serializerId);
  }

  private static void setupBasic(Config.Builder builder) {
    Random rand = new Random();
    Long seed = builder.getValue("system.seed", Long.class);
    if (seed == null) {
      builder.setValue("system.seed", rand.nextLong());
    }
  }
  
  private static void setupApp(Config.Builder builder, String receiverIp, String receiverPort) {
    builder.setValue("transfer.receiverIp", receiverIp);
    builder.setValue("transfer.receiverPort", receiverPort);
  }

  private static void setupSystem(String[] args) throws URISyntaxException {
    Config.Impl config = (Config.Impl) Kompics.getConfig();
    Config.Builder builder = Kompics.getConfig().modify(UUID.randomUUID());
    setupApp(builder, args[0], args[1]);
    setupBasic(builder);
    setupSerializers();
    config.apply(builder.finalise(), (Optional) Optional.absent());
    Kompics.setConfig(config);
  }

  public static void main(String[] args) throws IOException, URISyntaxException {
    if (Kompics.isOn()) {
      Kompics.shutdown();
    }
    setupSystem(args);
    Kompics.createAndStart(SenderHostComp.class, Runtime.getRuntime().
      availableProcessors(), 20); // Yes 20 is totally arbitrary
    try {
      Kompics.waitForTermination();
    } catch (InterruptedException ex) {
      System.exit(1);
    }
  }
}

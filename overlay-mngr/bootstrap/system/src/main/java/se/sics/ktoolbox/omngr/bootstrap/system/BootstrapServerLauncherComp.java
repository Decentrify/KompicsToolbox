/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * NatTraverser is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.omngr.bootstrap.system;

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
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.ktoolbox.croupier.CroupierSerializerSetup;
import se.sics.ktoolbox.gradient.GradientSerializerSetup;
import se.sics.ktoolbox.netmngr.NetworkMngrComp;
import se.sics.ktoolbox.netmngr.NetworkMngrSerializerSetup;
import se.sics.ktoolbox.netmngr.event.NetMngrReady;
import se.sics.ktoolbox.omngr.bootstrap.BootstrapServerComp;
import se.sics.ktoolbox.omngr.OMngrSerializerSetup;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayRegistry;
import se.sics.ktoolbox.util.network.nat.NatAwareAddress;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;
import se.sics.ktoolbox.util.status.Status;
import se.sics.ktoolbox.util.status.StatusPort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BootstrapServerLauncherComp extends ComponentDefinition {

  //******************************CONNECTIONS*********************************
  //*************************INTERNAL_NO_CONNECTION***************************
  private Positive<StatusPort> externalStatusPort = requires(StatusPort.class);
  //****************************EXTRENAL_STATE********************************
  private NatAwareAddress systemAdr;
  //********************************CLEANUP***********************************
  private Component timerComp;
  private Component netMngrComp;
  private Component bootstrapServerComp;

  public BootstrapServerLauncherComp() {
    subscribe(handleStart, control);
    subscribe(handleNetReady, externalStatusPort);
  }

  private Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {

      timerComp = create(JavaTimer.class, Init.NONE);
      setNetMngr();
      trigger(Start.event, timerComp.control());
      trigger(Start.event, netMngrComp.control());
    }
  };

  private void setNetMngr() {
    logger.info("setting up network mngr");
    NetworkMngrComp.ExtPort netExtPorts = new NetworkMngrComp.ExtPort(timerComp.getPositive(Timer.class));
    netMngrComp = create(NetworkMngrComp.class, new NetworkMngrComp.Init(netExtPorts));
    connect(netMngrComp.getPositive(StatusPort.class), externalStatusPort.getPair(), Channel.TWO_WAY);
  }

  ClassMatchedHandler handleNetReady
    = new ClassMatchedHandler<NetMngrReady, Status.Internal<NetMngrReady>>() {
    @Override
    public void handle(NetMngrReady content, Status.Internal<NetMngrReady> container) {
      logger.info("network mngr ready");
      systemAdr = content.systemAdr;
      setBootstrapServer();
      trigger(Start.event, bootstrapServerComp.control());
    }
  };

  private void setBootstrapServer() {
    logger.info("setting up bootstrap server");
    bootstrapServerComp = create(BootstrapServerComp.class, new BootstrapServerComp.Init(systemAdr));
    connect(bootstrapServerComp.getNegative(Network.class), netMngrComp.getPositive(Network.class), Channel.TWO_WAY);
  }

  private static void systemSetup() {
    BasicIdentifiers.registerDefaults2(1234l);
    OverlayRegistry.initiate(new OverlayId.BasicTypeFactory((byte) 0), new OverlayId.BasicTypeComparator());

    serializerSetup();
  }

  private static void serializerSetup() {
    //serializers setup
    int serializerId = 128;
    serializerId = BasicSerializerSetup.registerBasicSerializers(serializerId);
    serializerId = CroupierSerializerSetup.registerSerializers(serializerId);
    serializerId = GradientSerializerSetup.registerSerializers(serializerId);
    serializerId = OMngrSerializerSetup.registerSerializers(serializerId);
    serializerId = NetworkMngrSerializerSetup.registerSerializers(serializerId);

    if (serializerId > 255) {
      throw new RuntimeException("switch to bigger serializerIds, last serializerId:" + serializerId);
    }

    //hooks setup
    //no hooks needed
  }

  public static void main(String[] args) {
    systemSetup();
    start();
    try {
      Kompics.waitForTermination();
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex.getMessage());
    }
  }

  public static void start() {
    if (Kompics.isOn()) {
      Kompics.shutdown();
    }
    Kompics.createAndStart(BootstrapServerLauncherComp.class, Runtime.getRuntime().availableProcessors(), 20); // Yes 20 is totally arbitrary
  }

  public static void stop() {
    Kompics.shutdown();
  }

}

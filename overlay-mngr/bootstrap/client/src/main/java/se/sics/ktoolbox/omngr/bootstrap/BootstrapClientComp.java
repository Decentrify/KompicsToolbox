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
package se.sics.ktoolbox.omngr.bootstrap;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.nutil.timer.TimerProxy;
import se.sics.ktoolbox.nutil.timer.TimerProxyImpl;
import se.sics.ktoolbox.omngr.bootstrap.msg.Heartbeat;
import se.sics.ktoolbox.omngr.bootstrap.msg.Sample;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;

/**
 * TODO removed caracal - fix CC bootstrap
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BootstrapClientComp extends ComponentDefinition {

  //******************************CONNECTIONS*********************************
  private final Negative<BootstrapClientPort> bootstrapPort;
  private final Positive<Network> networkPort;
  private final Positive<Timer> timerPort;
  private final TimerProxy timer;
  //*****************************EXTERNAL_STATE*******************************
  private final KAddress selfAdr;
  private final SystemKCWrapper systemConfig;
  private final BootstrapClientConfig clientConfig;
  //*****************************INTERNAL_STATE*******************************
  private final Random rand;
  private final Map<OverlayId, BootstrapClientEvent.Start> heartbeats = new HashMap<>();
  //********************************AUX_STATE*********************************
  private UUID heartbeatTimeout;

  public BootstrapClientComp(Init init) {
    bootstrapPort = provides(BootstrapClientPort.class);
    networkPort = requires(Network.class);
    timerPort = requires(Timer.class);
    
    systemConfig = new SystemKCWrapper(config());
    clientConfig = BootstrapClientConfig.instance(config()).get();
    selfAdr = init.selfAdr;
    loggingCtxPutAlways("nId", init.selfAdr.getId().toString());
    logger.info("initiating with seed:{}, bootstrap server:{}", systemConfig.seed, clientConfig.server);

    timer = new TimerProxyImpl().setup(proxy);
    rand = new Random(systemConfig.seed);

    subscribe(handleStart, control);
    subscribe(handleHeartbeatStart, bootstrapPort);
    subscribe(handleHeartbeatStop, bootstrapPort);
    subscribe(handleSample, networkPort);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      heartbeatTimeout = timer.schedulePeriodicTimer(0, clientConfig.heartbeatPeriod, heartbeatTimeout());
    }
  };

  @Override
  public void tearDown() {
    timer.cancelPeriodicTimer(heartbeatTimeout);
    heartbeatTimeout = null;
  }

  private Consumer<Boolean> heartbeatTimeout() {
    return (_in) -> {
      logger.info("heartbeat overlays:{}", heartbeats.size());
      heartbeats.keySet().forEach((overlayId) -> {
        sendHeartbeat(overlayId);
        sampleRequest(overlayId);
      });
    };
  }

  private void sendHeartbeat(OverlayId overlayId) {
    logger.debug("heartbeat id:{}", overlayId);
    Heartbeat content = new Heartbeat(overlayId, rand.nextInt(clientConfig.heartbeatPositions));
    KContentMsg container = new BasicContentMsg(new BasicHeader(selfAdr, clientConfig.server, Transport.UDP), content);
    trigger(container, networkPort);
  }

  private void sampleRequest(OverlayId overlayId) {
    logger.debug("sample request for:{}", overlayId);
    Sample.Request content = new Sample.Request(overlayId);
    KContentMsg container = new BasicContentMsg(new BasicHeader(selfAdr, clientConfig.server, Transport.UDP), content);
    trigger(container, networkPort);
  }

  Handler handleHeartbeatStart = new Handler<BootstrapClientEvent.Start>() {
    @Override
    public void handle(BootstrapClientEvent.Start req) {
      logger.info("heartbeat start:{}", req.overlay);
      heartbeats.put(req.overlay, req);
      sendHeartbeat(req.overlay);
      sampleRequest(req.overlay);
    }
  };

  Handler handleHeartbeatStop = new Handler<BootstrapClientEvent.Stop>() {
    @Override
    public void handle(BootstrapClientEvent.Stop req) {
      logger.info("heartbeat stop:{}", req.overlay);
      heartbeats.remove(req.overlay);
    }
  };

  ClassMatchedHandler handleSample
    = new ClassMatchedHandler<Sample.Response, KContentMsg<?, ?, Sample.Response>>() {

    @Override
    public void handle(Sample.Response content, KContentMsg<?, ?, Sample.Response> msg) {
      logger.debug("received:{}", content);
      BootstrapClientEvent.Start req = heartbeats.get(content.overlayId);
      if (req != null) {
        answer(req, req.sample(content.sample));
      }
    }
  };

  //**************************************************************************
  public static class Init extends se.sics.kompics.Init<BootstrapClientComp> {

    public final KAddress selfAdr;

    public Init(KAddress selfAdr) {
      this.selfAdr = selfAdr;
    }
  }
}

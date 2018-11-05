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
package se.sics.ktoolbox.overlaymngr.bootstrap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.util.Identifier;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.croupier.CroupierControlPort;
import se.sics.ktoolbox.croupier.event.CroupierDisconnected;
import se.sics.ktoolbox.croupier.event.CroupierJoin;
import se.sics.ktoolbox.omngr.bootstrap.BootstrapClientEvent;
import se.sics.ktoolbox.omngr.bootstrap.BootstrapClientPort;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * TODO - Alex - fix commented out bootstrapping CC
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CroupierBootstrapComp extends ComponentDefinition {

  //*******************************CONNECTIONS********************************
  private final Positive timerPort = requires(Timer.class);
  private final Positive heartbeatPort = requires(BootstrapClientPort.class);
  private final Positive croupierStatusPort = requires(CroupierControlPort.class);
  private final Negative bootstrapPort = provides(CroupierBootstrapPort.class);

  private Map<Identifier, List<KAddress>> samples = new HashMap<>();

  public CroupierBootstrapComp(Init init) {
    SystemKCWrapper systemConfig = new SystemKCWrapper(config());
    loggingCtxPutAlways("nId", systemConfig.id.toString());

    subscribe(handleStart, control);
    subscribe(handleCroupierBootstrap, bootstrapPort);
    subscribe(handleExternalSample, heartbeatPort);
    subscribe(handleJoin, croupierStatusPort);
    subscribe(handleDisconnected, croupierStatusPort);
  }

  //******************************CONTROL*************************************
  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
    }
  };

  Handler handleCroupierBootstrap = new Handler<OMCroupierBootstrap>() {
    @Override
    public void handle(OMCroupierBootstrap req) {
      logger.trace("{}", req);
      trigger(new BootstrapClientEvent.Start(req.overlayId), heartbeatPort);
    }
  };

  Handler handleExternalSample = new Handler<BootstrapClientEvent.Sample>() {
    @Override
    public void handle(BootstrapClientEvent.Sample sample) {
      logger.trace("{}", sample);
      samples.put(sample.req.overlay, sample.sample);
      trigger(new CroupierJoin(sample.req.overlay, sample.sample), croupierStatusPort);
    }
  };

  Handler handleJoin = new Handler<CroupierJoin>() {
    @Override
    public void handle(CroupierJoin req) {
      logger.trace("{}", req);
      trigger(new BootstrapClientEvent.Start(req.overlayId), heartbeatPort);
    }
  };

  Handler handleDisconnected = new Handler<CroupierDisconnected>() {
    @Override
    public void handle(CroupierDisconnected event) {
      List<KAddress> sample = samples.get(event.overlayId);
      if (sample != null && !sample.isEmpty()) {
        trigger(new CroupierJoin(event.overlayId, sample), croupierStatusPort);
      }
    }
  };

  public static class Init extends se.sics.kompics.Init<CroupierBootstrapComp> {
  }
}

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
package se.sics.ktoolbox.netmngr.nxnet;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kill;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.config.Config;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.identifiable.basic.IntIdFactory;
import se.sics.ktoolbox.util.idextractor.SourcePortIdExtractor;
import se.sics.ktoolbox.nutil.network.ledbat.LedbatNetwork;
import se.sics.ktoolbox.nutil.network.ledbat.LedbatStatus;
import se.sics.ktoolbox.util.network.ports.One2NChannel;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NxNetComp extends ComponentDefinition {

  private String logPrefix = "";

   //*****************************CONNECTIONS**********************************
  //***************************EXTERNAL_CONNECT*******************************
  Negative<NxNetPort> nxNetPort = provides(NxNetPort.class);
  Negative<Network> networkPort = provides(Network.class);
  Negative<LedbatStatus.Port> ledbatStatusPort = provides(LedbatStatus.Port.class);
  Positive<Timer> timerPort = requires(Timer.class);
  //*******************************CONFIG*************************************
  private SystemKCWrapper systemConfig;
  //*************************INTERNAL_NO_CONNECT******************************
  private One2NChannel<Network> networkEnd;
  //***************************INTERNAL_STATE*********************************
  private Map<Integer, Component> networks = new HashMap<>();
  private final IntIdFactory portIds;

  public NxNetComp(Init init) {
    systemConfig = new SystemKCWrapper(config());
    logPrefix = "<nid:" + systemConfig.id + "> ";

    networkEnd = One2NChannel.getChannel("nxnet", networkPort, new SourcePortIdExtractor());
    this.portIds = new IntIdFactory(Optional.of(systemConfig.seed));
    
    subscribe(handleStart, control);
    subscribe(handleBindReq, nxNetPort);
    subscribe(handleUnbindReq, nxNetPort);
    subscribe(handleBindLedbatReq, nxNetPort);
    subscribe(handleUnbindLedbatReq, nxNetPort);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      logger.info("{}starting", logPrefix);
    }
  };

  Handler handleBindReq = new Handler<NxNetBind.Request>() {
    @Override
    public void handle(NxNetBind.Request req) {
      logger.trace("{}received:{}", logPrefix, req);
      if (networks.containsKey(req.adr.getPort())) {
        logger.warn("{}port:{} already bound - will not bind again", logPrefix, req.adr.getPort());
        answer(req, req.answer());
        return;
      }
      Config.Builder c = config().modify(id());
      if (req.bindAdr.isPresent()) {
        c.setValue("netty.bindInterface", req.bindAdr.get());
      }
      Component network = create(NettyNetwork.class, new NettyInit(req.adr), c.finalise());
      networkEnd.addChannel(portIds.rawId(req.adr.getPort()), network.getPositive(Network.class));
      trigger(Start.event, network.control());
      networks.put(req.adr.getPort(), network);
      logger.info("{}binding port:{}", new Object[]{logPrefix, req.adr.getPort()});
      answer(req, req.answer());
    }
  };

  Handler handleUnbindReq = new Handler<NxNetUnbind.Request>() {

    @Override
    public void handle(NxNetUnbind.Request req) {
      logger.trace("{}received:{}", logPrefix, req);
      Component network = networks.remove(req.port);
      if (network == null) {
        logger.warn("{}port:{} not bound", logPrefix, req.port);
        answer(req, req.answer());
        return;
      }
      networkEnd.removeChannel(portIds.rawId(req.port), network.getPositive(Network.class));
      trigger(Kill.event, network.control());
      logger.info("{}unbinding port:{}", new Object[]{logPrefix, req.port});
      answer(req, req.answer());
    }
  };
  
  Handler handleBindLedbatReq = new Handler<NxNetBind.LedbatRequest>() {
    @Override
    public void handle(NxNetBind.LedbatRequest req) {
      logger.trace("{}received:{}", logPrefix, req);
      if (networks.containsKey(req.adr.getPort())) {
        logger.warn("{}port:{} already bound - will not bind again", logPrefix, req.adr.getPort());
        answer(req, req.answer());
        return;
      }
      Config.Builder c = config().modify(id());
      if (req.bindAdr.isPresent()) {
        c.setValue("netty.bindInterface", req.bindAdr.get());
      }
      Component network = create(LedbatNetwork.class, new LedbatNetwork.Init(req.adr), c.finalise());
      networkEnd.addChannel(portIds.rawId(req.adr.getPort()), network.getPositive(Network.class));
      connect(network.getNegative(Timer.class), timerPort, Channel.TWO_WAY);
      //TODO Alex - create channel
      connect(network.getPositive(LedbatStatus.Port.class), ledbatStatusPort, Channel.TWO_WAY);
      trigger(Start.event, network.control());
      networks.put(req.adr.getPort(), network);
      logger.info("{}binding port:{}", new Object[]{logPrefix, req.adr.getPort()});
      answer(req, req.answer());
    }
  };

  Handler handleUnbindLedbatReq = new Handler<NxNetUnbind.LedbatRequest>() {

    @Override
    public void handle(NxNetUnbind.LedbatRequest req) {
      logger.trace("{}received:{}", logPrefix, req);
      Component network = networks.remove(req.port);
      if (network == null) {
        logger.warn("{}port:{} not bound", logPrefix, req.port);
        answer(req, req.answer());
        return;
      }
      networkEnd.removeChannel(portIds.rawId(req.port), network.getPositive(Network.class));
      disconnect(network.getNegative(Timer.class), timerPort);
      //TODO Alex disconnect channel
      disconnect(network.getPositive(LedbatStatus.Port.class), ledbatStatusPort);
      trigger(Kill.event, network.control());
      logger.info("{}unbinding port:{}", new Object[]{logPrefix, req.port});
      answer(req, req.answer());
    }
  };

  public static class Init extends se.sics.kompics.Init<NxNetComp> {
  }
}

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
package se.sics.ktoolbox.netmngr.driver;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.slf4j.Logger;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.config.Config;
import se.sics.kompics.network.Network;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.netmngr.NetworkConfig;
import se.sics.ktoolbox.netmngr.ipsolver.IpSolve;
import se.sics.ktoolbox.netmngr.ipsolver.IpSolverComp;
import se.sics.ktoolbox.netmngr.ipsolver.IpSolverPort;
import se.sics.ktoolbox.netmngr.nxnet.NxNetBind;
import se.sics.ktoolbox.netmngr.nxnet.NxNetComp;
import se.sics.ktoolbox.netmngr.nxnet.NxNetPort;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NxNetProxy {

  private ComponentProxy proxy;
  private Logger logger;
  private NetworkConfig networkConfig;
  private IdentifierFactory eventIds;

  private Positive<IpSolverPort> ipSolverPort;
  private Positive<NxNetPort> nxNetPort;

  private Component ipSolverComp;
  private Component nxNetComp;

  private InetAddress privateIp;

  private Consumer<InetAddress> privateIpDetected;
  private Map<Identifier, Consumer<Boolean>> pendingNetworkReady = new HashMap<>();

  public NxNetProxy() {
  }

  public NxNetProxy setup(ComponentProxy proxy, Logger logger, NetworkConfig networkConfig,
    IdentifierFactory eventIds, Consumer<InetAddress> privateIpDetected) {
    this.proxy = proxy;
    this.logger = logger;
    this.eventIds = eventIds;
    this.privateIpDetected = privateIpDetected;
    this.networkConfig = networkConfig;
    setIpSolver();
    setNxNet();

    proxy.subscribe(handlePrivateIpDetected, ipSolverPort);
    proxy.subscribe(handleBindResp, nxNetPort);
    return this;
  }

  private void setIpSolver() {
    ipSolverPort = proxy.requires(IpSolverPort.class);
    ipSolverComp = proxy.create(IpSolverComp.class, new IpSolverComp.IpSolverInit());
    proxy.connect(ipSolverComp.getPositive(IpSolverPort.class), ipSolverPort.getPair(), Channel.TWO_WAY);
  }

  private void setNxNet() {
    nxNetPort = proxy.requires(NxNetPort.class);
    nxNetComp = proxy.create(NxNetComp.class, new NxNetComp.Init());
    proxy.connect(nxNetComp.getPositive(NxNetPort.class), nxNetPort.getPair(), Channel.TWO_WAY);
  }

  public NxNetProxy start() {
    proxy.trigger(Start.event, ipSolverComp.control());
    proxy.trigger(Start.event, nxNetComp.control());
    proxy.trigger(new IpSolve.Request(networkConfig.ipTypes), ipSolverPort);
    return this;
  }
  
  public void bind(KAddress address, Consumer<Boolean> networkReady) {
    Identifier eventId = eventIds.randomId();
    pendingNetworkReady.put(eventId, networkReady);
    NxNetBind.Request req = NxNetBind.Request.providedAdr(eventId, address, privateIp);
    proxy.trigger(req, nxNetPort);
    logger.debug("sending:{}", req);
  }
  
  public void connect(Component comp) {
    proxy.connect(nxNetComp.getPositive(Network.class), comp.getNegative(Network.class), Channel.TWO_WAY);
  }
  
  public void disconnect(Component comp) {
    proxy.disconnect(nxNetComp.getPositive(Network.class), comp.getNegative(Network.class));
  }

  private final Handler handlePrivateIpDetected = new Handler<IpSolve.Response>() {
    @Override
    public void handle(IpSolve.Response resp) {
      logger.info("ips of type:{} detected:{} bound ip:{}", new Object[]{resp.netInterfaces, resp.addrs, resp.boundIp});
      if (resp.boundIp == null) {
        throw new RuntimeException("no bound ip");
      }
      privateIp = resp.boundIp;
      privateIpDetected.accept(privateIp);
    }
  };

  private final Handler handleBindResp = new Handler<NxNetBind.Response>() {
    @Override
    public void handle(NxNetBind.Response resp) {
      logger.debug("received:{}", resp);
      pendingNetworkReady.remove(resp.getId()).accept(true);
    }
  };
}

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
package se.sics.ktoolbox.nutil.nxcomp.v2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Handler;
import se.sics.kompics.Kill;
import se.sics.kompics.Negative;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.network.portsv2.EventIdExtractorV2;
import se.sics.ktoolbox.nutil.network.portsv2.EventTypeExtractorsV2;
import se.sics.ktoolbox.nutil.network.portsv2.MsgIdExtractorV2;
import se.sics.ktoolbox.nutil.network.portsv2.MsgTypeExtractorsV2;
import se.sics.ktoolbox.nutil.network.portsv2.One2NEventChannelV2;
import se.sics.ktoolbox.nutil.network.portsv2.OutgoingOne2NMsgChannelV2;
import se.sics.ktoolbox.nutil.nxcomp.NxMngrEvents;
import se.sics.ktoolbox.nutil.nxcomp.NxMngrPort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NxMngrProxyV2 {

  //setup only
  private final List<Class<PortType>> negativePorts;
  private final List<Map<String, EventIdExtractorV2>> negativeIdSelectors;
  private final List<Class<PortType>> positivePorts;
  private final List<Map<String, EventIdExtractorV2>> positiveIdSelectors;
//  private final Optional<NetworkPort> negativeNetwork;
  private final Optional<NxStackDefinitionV2.NetworkPort> positiveNetwork;
  //
  private final List<One2NEventChannelV2> negativeChannels = new LinkedList<>();
  private final List<One2NEventChannelV2> positiveChannels = new LinkedList<>();
//  private Optional<OutgoingOne2NMsgChannelV2> negativeNetChannel = Optional.empty();
  private Optional<OutgoingOne2NMsgChannelV2> positiveNetChannel = Optional.empty();
  //
  private final NxStackDefinitionV2 stackDefinition;
  private Negative<NxMngrPort> mngrPort;
  private ComponentProxy proxy;
  private Logger logger;

  private Map<Identifier, Instance> instances = new HashMap<>();

  public NxMngrProxyV2(NxStackDefinitionV2 stackDefinition,
    List<Class<PortType>> negativePorts, List<Map<String, EventIdExtractorV2>> negativeIdSelectors,
    List<Class<PortType>> positivePorts, List<Map<String, EventIdExtractorV2>> positiveIdSelectors) {
    this(stackDefinition, negativePorts, negativeIdSelectors, positivePorts, positiveIdSelectors,
      //      Optional.empty(), 
      Optional.empty());
  }

  public NxMngrProxyV2(NxStackDefinitionV2 stackDefinition,
    List<Class<PortType>> negativePorts, List<Map<String, EventIdExtractorV2>> negativeIdSelectors,
    List<Class<PortType>> positivePorts, List<Map<String, EventIdExtractorV2>> positiveIdSelectors,
    //    Optional<NetworkPort> negativeNetwork,
    Optional<NxStackDefinitionV2.NetworkPort> positiveNetwork) {
    this.stackDefinition = stackDefinition;
    this.negativePorts = negativePorts;
    this.negativeIdSelectors = negativeIdSelectors;
    this.positivePorts = positivePorts;
    this.positiveIdSelectors = positiveIdSelectors;
//    this.negativeNetwork = negativeNetwork;
    this.positiveNetwork = positiveNetwork;
  }

  public NxMngrProxyV2 setup(String mngrName, ComponentProxy proxy, Logger logger) {
    this.proxy = proxy;
    this.logger = logger;

    mngrPort = proxy.provides(NxMngrPort.class);

    Iterator<Class<PortType>> negPortIt = negativePorts.iterator();
    Iterator<Map<String, EventIdExtractorV2>> negIdSelectorsIt = negativeIdSelectors.iterator();
    while (negPortIt.hasNext()) {
      Class<PortType> portType = negPortIt.next();
      Map<String, EventIdExtractorV2> channelIdSelector = negIdSelectorsIt.next();
      Negative negativePort = proxy.provides(portType);
      One2NEventChannelV2 channel = One2NEventChannelV2.getChannel(mngrName + " negative", logger, negativePort,
        new EventTypeExtractorsV2.Base(), channelIdSelector);
      negativeChannels.add(channel);
    }

    Iterator<Class<PortType>> posPortIt = positivePorts.iterator();
    Iterator<Map<String, EventIdExtractorV2>> posIdSelectorsIt = positiveIdSelectors.iterator();
    while (posPortIt.hasNext()) {
      Class<PortType> portType = posPortIt.next();
      Map<String, EventIdExtractorV2> channelIdSelector = posIdSelectorsIt.next();
      Positive positivePort = proxy.requires(portType);
      One2NEventChannelV2 channel = One2NEventChannelV2.getChannel(mngrName + " positive", logger, positivePort,
        new EventTypeExtractorsV2.Base(), channelIdSelector);
      positiveChannels.add(channel);
    }

    if (positiveNetwork.isPresent()) {
      Map<String, MsgIdExtractorV2> channelIdSelector = positiveNetwork.get().idSelectors;
      Positive positivePort = proxy.requires(Network.class);
      OutgoingOne2NMsgChannelV2 channel = OutgoingOne2NMsgChannelV2.getChannel(mngrName + " net positive", logger,
        positivePort, new MsgTypeExtractorsV2.Base(), channelIdSelector);
      positiveNetChannel = Optional.of(channel);
    }
    proxy.subscribe(handleCreateReq, mngrPort);
    proxy.subscribe(handleKillReq, mngrPort);
    return this;
  }

  public void close() {
    if (!instances.isEmpty()) {
      logger.warn("kill components before nxmngr shutdown");
      instances.values().forEach((instance) -> kill(instance));
    }
    negativeChannels.forEach((channel) -> channel.disconnect());
    positiveChannels.forEach((channel) -> channel.disconnect());
    if (positiveNetChannel.isPresent()) {
      positiveNetChannel.get().disconnect();
    }
  }

  Handler handleCreateReq = new Handler<NxMngrEvents.CreateReq>() {
    @Override
    public void handle(NxMngrEvents.CreateReq req) {
      if (!instances.containsKey(req.stackId)) {
        NxStackInstanceV2 stackInstance = stackDefinition.setup(proxy, req.stackId, req.stackInit,
          negativePorts, positivePorts, positiveNetwork.isPresent());
        Instance instance = new Instance(req.stackId, stackInstance);
        instance.use(req.getId());
        instances.put(req.stackId, instance);
        if (negativePorts.size() != stackInstance.positivePorts.size()
          || positivePorts.size() != stackInstance.negativePorts.size()) {
          throw new RuntimeException("bad logic - ports definition/instance");
        }

        Iterator<One2NEventChannelV2> negChannelIt = negativeChannels.iterator();
        Iterator<Positive> stackPosIt = stackInstance.positivePorts.iterator();
        while (negChannelIt.hasNext()) {
          One2NEventChannelV2 channel = negChannelIt.next();
          Positive stackPort = stackPosIt.next();
          channel.addChannel(req.stackId, stackPort);
        }

        Iterator<One2NEventChannelV2> posChannelIt = positiveChannels.iterator();
        Iterator<Negative> stackNegIt = stackInstance.negativePorts.iterator();
        while (posChannelIt.hasNext()) {
          One2NEventChannelV2 channel = posChannelIt.next();
          Negative stackPort = stackNegIt.next();
          channel.addChannel(req.stackId, stackPort);
        }
        if(positiveNetChannel.isPresent()) {
          OutgoingOne2NMsgChannelV2 channel = positiveNetChannel.get();
          channel.addChannel(req.stackId, stackInstance.negativeNetwork.get());
        }
        stackInstance.components.forEach((comp) -> proxy.trigger(Start.event, comp.getControl()));
      } else {
        instances.get(req.stackId).use(req.getId());
      }
      proxy.answer(req, req.ack());
    }
  };

  Handler handleKillReq = new Handler<NxMngrEvents.KillReq>() {
    @Override
    public void handle(NxMngrEvents.KillReq req) {
      if (instances.containsKey(req.stackId())) {
        Instance instance = instances.get(req.stackId());
        instance.endUse(req.getId());
        if (instance.users.isEmpty()) {
          kill(instance);
          instances.remove(req.stackId());
        }
      }
      proxy.answer(req, req.ack());
    }
  };

  private void kill(Instance instance) {
    NxStackInstanceV2 nxInstance = instance.instance;
    Iterator<One2NEventChannelV2> negChannelIt = negativeChannels.iterator();
    Iterator<Positive> stackPosIt = nxInstance.positivePorts.iterator();
    while (negChannelIt.hasNext()) {
      One2NEventChannelV2 channel = negChannelIt.next();
      Positive stackPort = stackPosIt.next();
      channel.removeChannel(instance.stackId, stackPort);
    }

    Iterator<One2NEventChannelV2> posChannelIt = positiveChannels.iterator();
    Iterator<Negative> stackNegIt = nxInstance.negativePorts.iterator();
    while (posChannelIt.hasNext()) {
      One2NEventChannelV2 channel = posChannelIt.next();
      Negative stackPort = stackNegIt.next();
      channel.removeChannel(instance.stackId, stackPort);
    }
    if (positiveNetChannel.isPresent()) {
      positiveNetChannel.get().removeChannel(instance.stackId, nxInstance.negativeNetwork.get());
    }
    nxInstance.components.forEach((comp) -> proxy.trigger(Kill.event, comp.getControl()));
  }

  static class Instance {

    final Identifier stackId;
    final NxStackInstanceV2 instance;
    Set<Identifier> users = new HashSet<>();

    public Instance(Identifier stackId, NxStackInstanceV2 instance) {
      this.stackId = stackId;
      this.instance = instance;
    }

    public void use(Identifier user) {
      users.add(user);
    }

    public void endUse(Identifier user) {
      users.remove(user);
    }
  }
}

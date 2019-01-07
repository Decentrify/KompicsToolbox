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
package se.sics.ktoolbox.nutil.nxcomp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Handler;
import se.sics.kompics.Kill;
import se.sics.kompics.Negative;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.util.network.ports.ChannelIdExtractor;
import se.sics.ktoolbox.util.network.ports.One2NChannel;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NxMngrProxy {

  //setup only
  private final List<Class<PortType>> negativePorts;
  private final List<ChannelIdExtractor> negativeIdExtractors;
  private final List<Class<PortType>> positivePorts;
  private final List<ChannelIdExtractor> positiveIdExtractors;
  //
  private final List<One2NChannel> negativeChannels = new LinkedList<>();
  private final List<One2NChannel> positiveChannels = new LinkedList<>();
  
  //
  private final NxStackDefinition stackDefinition;
  private Negative<NxMngrPort> mngrPort;
  private ComponentProxy proxy;
  private Logger logger;

  private Map<Identifier, NxStackInstance> instances = new HashMap<>();

  public NxMngrProxy(NxStackDefinition stackDefinition,
    List<Class<PortType>> negativePorts, List<ChannelIdExtractor> negativeIdExtractors,
    List<Class<PortType>> positivePorts, List<ChannelIdExtractor> positiveIdExtractors) {
    this.stackDefinition = stackDefinition;
    this.negativePorts = negativePorts;
    this.negativeIdExtractors = negativeIdExtractors;
    this.positivePorts = positivePorts;
    this.positiveIdExtractors = positiveIdExtractors;
  }

  public NxMngrProxy setup(ComponentProxy proxy, Logger logger) {
    this.proxy = proxy;
    this.logger = logger;
    
    mngrPort = proxy.provides(NxMngrPort.class);
    
    Iterator<Class<PortType>> negPortIt = negativePorts.iterator();
    Iterator<ChannelIdExtractor> negIdExtractorsIt = negativeIdExtractors.iterator();
    while(negPortIt.hasNext()) {
      Class<PortType> portType = negPortIt.next();
      ChannelIdExtractor channelIdExtractor = negIdExtractorsIt.next();
      Negative negativePort = proxy.provides(portType);
      One2NChannel channel
        = One2NChannel.getChannel("nxmngr negative port:" + portType, negativePort, channelIdExtractor);
      negativeChannels.add(channel);
    }
    
    Iterator<Class<PortType>> posPortIt = positivePorts.iterator();
    Iterator<ChannelIdExtractor> posIdExtractorsIt = positiveIdExtractors.iterator();
    while(posPortIt.hasNext()) {
      Class<PortType> portType = posPortIt.next();
      ChannelIdExtractor channelIdExtractor = posIdExtractorsIt.next();
      Positive positivePort = proxy.requires(portType);
      One2NChannel channel
        = One2NChannel.getChannel("nxmngr positive port:" + portType, positivePort, channelIdExtractor);
      positiveChannels.add(channel);
    }
    
    proxy.subscribe(handleCreateReq, mngrPort);
    proxy.subscribe(handleKillReq, mngrPort);
    return this;
  }

  public void close() {
    if (!instances.isEmpty()) {
      logger.warn("kill components before nxmngr shutdown");
      instances.keySet().forEach((compId) -> kill(compId));
    }
    negativeChannels.forEach((channel) -> channel.disconnect());
    positiveChannels.forEach((channel) -> channel.disconnect());
  }

  Handler handleCreateReq = new Handler<NxMngrEvents.CreateReq>() {
    @Override
    public void handle(NxMngrEvents.CreateReq req) {
      if (!instances.containsKey(req.stackId)) {
        NxStackInstance instance = stackDefinition.setup(proxy, req.stackId, req.stackInit, 
          negativePorts, positivePorts);
        instances.put(req.stackId, instance);
        if (negativePorts.size() != instance.positivePorts.size()
          || positivePorts.size() != instance.negativePorts.size()) {
          throw new RuntimeException("bad logic - ports definition/instance");
        }

        Iterator<One2NChannel> negChannelIt = negativeChannels.iterator();
        Iterator<Positive> stackPosIt = instance.positivePorts.iterator();
        while (negChannelIt.hasNext()) {
          One2NChannel channel = negChannelIt.next();
          Positive stackPort = stackPosIt.next();
          channel.addChannel(req.stackId, stackPort);
        }

        Iterator<One2NChannel> posChannelIt = positiveChannels.iterator();
        Iterator<Negative> stackNegIt = instance.negativePorts.iterator();
        while (posChannelIt.hasNext()) {
          One2NChannel channel = posChannelIt.next();
          Negative stackPort = stackNegIt.next();
          channel.addChannel(req.stackId, stackPort);
        }
        instance.components.forEach((comp) -> proxy.trigger(Start.event, comp.getControl()));
      }
      proxy.answer(req, req.ack());
    }
  };

  Handler handleKillReq = new Handler<NxMngrEvents.KillReq>() {
    @Override
    public void handle(NxMngrEvents.KillReq req) {
      if (instances.containsKey(req.stackId)) {
        kill(req.stackId);
      }
      proxy.answer(req, req.ack());
    }
  };

  private void kill(Identifier stackId) {
    NxStackInstance instance = instances.remove(stackId);
    Iterator<One2NChannel> negChannelIt = negativeChannels.iterator();
    Iterator<Positive> stackPosIt = instance.positivePorts.iterator();
    while (negChannelIt.hasNext()) {
      One2NChannel channel = negChannelIt.next();
      Positive stackPort = stackPosIt.next();
      channel.removeChannel(stackId, stackPort);
    }

    Iterator<One2NChannel> posChannelIt = positiveChannels.iterator();
    Iterator<Negative> stackNegIt = instance.negativePorts.iterator();
    while (posChannelIt.hasNext()) {
      One2NChannel channel = posChannelIt.next();
      Negative stackPort = stackNegIt.next();
      channel.removeChannel(stackId, stackPort);
    }
    instance.components.forEach((comp) -> proxy.trigger(Kill.event, comp.getControl()));
  }
}

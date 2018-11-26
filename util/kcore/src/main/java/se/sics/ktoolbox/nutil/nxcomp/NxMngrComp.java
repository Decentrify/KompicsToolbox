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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.javatuples.Pair;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kill;
import se.sics.kompics.KompicsEvent;
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
public class NxMngrComp<D extends ComponentDefinition> extends ComponentDefinition {

  private final Negative<NxMngrPort> mngrPort = provides(NxMngrPort.class);

  private Map<Identifier, Component> components = new HashMap<>();

  public final Class<D> compDefinition;
  public final List<Pair<Class<PortType>, One2NChannel>> negativePorts = new LinkedList<>();
  public final List<Pair<Class<PortType>, One2NChannel>> positivePorts = new LinkedList<>();

  public NxMngrComp(Init<D> init) {
    this.compDefinition = init.compDefinition;

    init.negativePorts.forEach((portDefinition) -> {
      Class<PortType> portType = portDefinition.getValue0();
      NxChannelIdExtractor channelIdExtractor = portDefinition.getValue1();
      Negative negativePort = provides(portType);
      One2NChannel channel 
        = One2NChannel.getChannel("nxmngr negative port:" + portType, negativePort, channelIdExtractor);
      negativePorts.add(Pair.with(portType, channel));
    });
    init.positivePorts.forEach((portDefinition) -> {
      Class<PortType> portType = portDefinition.getValue0();
      NxChannelIdExtractor channelIdExtractor = portDefinition.getValue1();
      Positive positivePort = requires(portType);
      One2NChannel channel 
        = One2NChannel.getChannel("nxmngr positive port:" + portType, positivePort, channelIdExtractor);
      positivePorts.add(Pair.with(portType, channel));
    });
    subscribe(handleCreateReq, mngrPort);
    subscribe(handleKillReq, mngrPort);
  }

  @Override
  public void tearDown() {
    if (!components.isEmpty()) {
      logger.warn("kill components before nxmngr shutdown");
      components.keySet().forEach((compId) -> killComp(compId));
    }
    negativePorts.forEach((aux) -> aux.getValue1().disconnect());
    positivePorts.forEach((aux) -> aux.getValue1().disconnect());
  }

  Handler handleCreateReq = new Handler<NxMngrEvents.CreateReq>() {
    @Override
    public void handle(NxMngrEvents.CreateReq req) {
      if (!components.containsKey(req.compId)) {
        Component comp = create(compDefinition, req.compInit);
        components.put(req.compId, comp);
        negativePorts.forEach((aux) -> {
          Class<PortType> portType = aux.getValue0();
          One2NChannel channel = aux.getValue1();
          Positive port = comp.getPositive(portType);
          channel.addChannel(req.compId, port);
        });
        positivePorts.forEach((aux) -> {
          Class<PortType> portType = aux.getValue0();
          One2NChannel channel = aux.getValue1();
          Negative port = comp.getNegative(portType);
          channel.addChannel(req.compId, port);
        });
        trigger(Start.event, comp.getControl());
      }
      answer(req, req.ack());
    }
  };

  Handler handleKillReq = new Handler<NxMngrEvents.KillReq>() {
    @Override
    public void handle(NxMngrEvents.KillReq req) {
      if (components.containsKey(req.compId)) {
        killComp(req.compId);
      }
      answer(req, req.ack());
    }
  };

  private void killComp(Identifier compId) {
    Component comp = components.remove(compId);
    negativePorts.forEach((aux) -> {
      Class<PortType> portType = aux.getValue0();
      One2NChannel channel = aux.getValue1();
      Positive port = comp.getPositive(portType);
      channel.removeChannel(compId, port);
    });
    positivePorts.forEach((aux) -> {
      Class<PortType> portType = aux.getValue0();
      One2NChannel channel = aux.getValue1();
      Positive port = comp.getPositive(portType);
      channel.removeChannel(compId, port);
    });
    trigger(Kill.event, comp.control());
  }

  public static class Init<D extends ComponentDefinition> extends se.sics.kompics.Init<NxMngrComp> {

    public final Class<D> compDefinition;
    public final List<Pair<Class<PortType>, NxChannelIdExtractor>> negativePorts;
    public final List<Pair<Class<PortType>, NxChannelIdExtractor>> positivePorts;

    public Init(Class<D> compDefinition, List<Pair<Class<PortType>, NxChannelIdExtractor>> negativePorts,
      List<Pair<Class<PortType>, NxChannelIdExtractor>> positivePorts) {
      this.compDefinition = compDefinition;
      this.negativePorts = negativePorts;
      this.positivePorts = positivePorts;
    }
  }

  public static abstract class NxChannelIdExtractor<E extends KompicsEvent> extends ChannelIdExtractor<E, Identifier> {

    protected NxChannelIdExtractor(Class<E> eventType) {
      super(eventType);
    }
  }
}

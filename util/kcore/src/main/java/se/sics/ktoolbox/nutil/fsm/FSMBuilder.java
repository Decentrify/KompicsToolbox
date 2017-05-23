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
package se.sics.ktoolbox.nutil.fsm;

import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import se.sics.ktoolbox.nutil.fsm.api.FSMBasicStateNames;
import se.sics.ktoolbox.nutil.fsm.api.FSMException;
import se.sics.ktoolbox.nutil.fsm.api.FSMExternalState;
import se.sics.ktoolbox.nutil.fsm.api.FSMIdExtractor;
import se.sics.ktoolbox.nutil.fsm.api.FSMInternalStateBuilder;
import se.sics.ktoolbox.nutil.fsm.api.FSMInternalStateBuilders;
import se.sics.ktoolbox.nutil.fsm.api.FSMStateName;
import se.sics.ktoolbox.nutil.fsm.genericsetup.OnFSMExceptionAction;
import se.sics.ktoolbox.nutil.fsm.handler.FSMEventHandler;
import se.sics.ktoolbox.nutil.fsm.handler.FSMMsgHandler;
import se.sics.ktoolbox.nutil.fsm.handler.FSMStateChangeHandler;
import se.sics.ktoolbox.nutil.fsm.ids.FSMDefId;
import se.sics.ktoolbox.nutil.fsm.ids.FSMIds;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FSMBuilder {

  public static class MultiMachine {

    private Map<Class, Set<Class>> positivePorts = new HashMap<>();
    private Map<Class, Set<Class>> negativePorts = new HashMap<>();
    private Set<Class> events = new HashSet<>();
    private Set<Class> positiveNetworkMsgs = new HashSet<>();
    private Set<Class> negativeNetworkMsgs = new HashSet<>();

    private MultiMachine() {
    }

    public MultiMachine setPositivePorts(Map<Class, Set<Class>> portEvents) {
      positivePorts = portEvents;
      for (Set<Class> e : portEvents.values()) {
        events.addAll(e);
      }
      return this;
    }

    public MultiMachine setNegativePorts(Map<Class, Set<Class>> portEvents) {
      negativePorts = portEvents;
      for (Set<Class> e : portEvents.values()) {
        events.addAll(e);
      }
      return this;
    }

    public MultiMachine setPositiveNetworkMsgs(Set<Class> positiveNetworkMsgs) {
      this.positiveNetworkMsgs = positiveNetworkMsgs;
      return this;
    }

    public MultiMachine setNegativeNetworkMsgs(Set<Class> negativeNetworkMsgs) {
      this.negativeNetworkMsgs = negativeNetworkMsgs;
      return this;
    }

    public MultiFSM buildMultiFSM(final FSMachineDef fsmd, OnFSMExceptionAction oexa, FSMExternalState es,
      FSMInternalStateBuilder builder, FSMIdExtractor fsmIdExtractor) throws FSMException {

      FSMInternalStateBuilders builders = new FSMInternalStateBuilders();
      builders.register(fsmd.id, builder);

      fsmIdExtractor.set(events, positiveNetworkMsgs, negativeNetworkMsgs);
      MultiFSM multiFSM = new MultiFSM(fsmd, oexa, fsmIdExtractor, es, builders, positivePorts, negativePorts,
        positiveNetworkMsgs, negativeNetworkMsgs);
      return multiFSM;
    }

    public static MultiMachine instance() {
      return new MultiMachine();
    }
  }

  public static class Machine {

    private final Table<FSMStateName, FSMStateName, Boolean> transitionTable = HashBasedTable.create();
    private final Map<FSMStateName, FSMStateDef> states = new HashMap<>();

    private Machine() {
    }

    public Transition onState(FSMStateName state) throws FSMException {
      if (transitionTable.containsRow(state)) {
        throw new FSMException("state:" + state + " already registered");
      }
      return new Transition(this, state);
    }

    private void buildTransition(FSMStateName from, FSMStateName[] toStates, boolean toFinal,
      Optional<FSMStateChangeHandler> onEntry, Optional<FSMStateChangeHandler> onExit)
      throws FSMException {

      for (FSMStateName to : toStates) {
        if (transitionTable.contains(from, to)) {
          throw new FSMException("transition from:" + from + " to:" + to + " already registered");
        }
        transitionTable.put(from, to, true);
      }
      if (toFinal) {
        transitionTable.put(from, FSMBasicStateNames.FINAL, true);
      }

      if (states.containsKey(from)) {
        throw new FSMException("state:" + from + "already defined");
      }
      FSMStateDef stateDef = FSMStateDef.instance()
        .setOnEntry(onEntry)
        .setOnExit(onExit);
      states.put(from, stateDef);
    }
  }

  public static class Transition {

    private final Machine parent;
    private final FSMStateName from;
    private FSMStateName[] toStates = new FSMStateName[0];
    private boolean toFinal = false;

    private Optional<FSMStateChangeHandler> onEntry = Optional.absent();
    private Optional<FSMStateChangeHandler> onExit = Optional.absent();

    private Transition(Machine parent, FSMStateName from) {
      this.parent = parent;
      this.from = from;
    }

    public Transition onEntry(FSMStateChangeHandler handler) {
      onEntry = Optional.of(handler);
      return this;
    }

    public Transition onExit(FSMStateChangeHandler handler) {
      onExit = Optional.of(handler);
      return this;
    }

    public Transition nextStates(FSMStateName... ids) {
      this.toStates = ids;
      return this;
    }

    public Transition toFinal() {
      this.toFinal = true;
      return this;
    }

    public Machine buildTransition() throws FSMException {
      if (toStates == null) {
        throw new FSMException("to states not registered");
      }
      parent.buildTransition(from, toStates, toFinal, onEntry, onExit);
      return parent;
    }
  }

  public static class Handlers {

    private final Map<Class, Set<Class>> negativePorts = new HashMap<>();
    private final Map<Class, Set<Class>> positivePorts = new HashMap<>();
    private final Set<Class> positiveNetwork = new HashSet<>();
    private final Set<Class> negativeNetwork = new HashSet<>();

    private FSMEventHandler fallbackEventHandler = FSMachine.fallbackEventHandler;
    private FSMMsgHandler fallbackMsgHandler = FSMachine.fallbackMsgHandler;
    private final Table<Class, FSMStateName, FSMEventHandler> positivePortEventHandlers = HashBasedTable.create();
    private final Map<Class, FSMEventHandler> positivePortFallbackHandlers = new HashMap<>();
    private final Table<Class, FSMStateName, FSMEventHandler> negativePortEventHandlers = HashBasedTable.create();
    private final Map<Class, FSMEventHandler> negativePortFallbackHandlers = new HashMap<>();
    private final Table<Class, FSMStateName, FSMMsgHandler> positiveNetworkMsgHandlers = HashBasedTable.create();
    private final Map<Class, FSMMsgHandler> positiveNetworkFallbackHandlers = new HashMap<>();
    private final Table<Class, FSMStateName, FSMMsgHandler> negativeNetworkMsgHandlers = HashBasedTable.create();
    private final Map<Class, FSMMsgHandler> negativeNetworkFallbackHandlers = new HashMap<>();

    public Handlers() {
    }

    public Port positivePort(Class portType) {
      return new Port(this, portType, true);
    }

    public Port negativePort(Class portType) {
      return new Port(this, portType, false);
    }
    
    public NetworkPort positiveNetwork() {
      return new NetworkPort(this, true);
    }

    public NetworkPort negativeNetwork() {
      return new NetworkPort(this, false);
    }

    public Handlers defaultFallback(FSMEventHandler eventHandler, FSMMsgHandler msgHandler) {
      fallbackEventHandler = eventHandler;
      fallbackMsgHandler = msgHandler;
      return this;
    }

    private void buildPort(Class portType, boolean pp, Map<Class, Map<FSMStateName, FSMEventHandler>> eventHandlers,
      Map<Class, FSMEventHandler> fallbackHandlers) throws FSMException {
      if (pp) {
        positivePorts.put(portType, eventHandlers.keySet());
        for (Map.Entry<Class, Map<FSMStateName, FSMEventHandler>> e : eventHandlers.entrySet()) {
          if (positivePortEventHandlers.containsRow(e.getKey())) {
            throw new FSMException("currently we do not allow same event in different ports(besides pos/neg");
          }
          for (Map.Entry<FSMStateName, FSMEventHandler> ee : e.getValue().entrySet()) {
            positivePortEventHandlers.put(e.getKey(), ee.getKey(), ee.getValue());
          }
        }
        positivePortFallbackHandlers.putAll(fallbackHandlers);
      } else {
        negativePorts.put(portType, eventHandlers.keySet());
        for (Map.Entry<Class, Map<FSMStateName, FSMEventHandler>> e : eventHandlers.entrySet()) {
          if (negativePortEventHandlers.containsRow(e.getKey())) {
            throw new FSMException("currently we do not allow same event in different ports(besides pos/neg");
          }
          for (Map.Entry<FSMStateName, FSMEventHandler> ee : e.getValue().entrySet()) {
            negativePortEventHandlers.put(e.getKey(), ee.getKey(), ee.getValue());
          }
        }
        negativePortFallbackHandlers.putAll(fallbackHandlers);
      }
    }

    private void buildNetwork(boolean pp, Map<Class, Map<FSMStateName, FSMMsgHandler>> msgHandlers,
      Map<Class, FSMMsgHandler> fallbackHandlers) throws FSMException {
      if (pp) {
        positiveNetwork.addAll(msgHandlers.keySet());
        for (Map.Entry<Class, Map<FSMStateName, FSMMsgHandler>> e : msgHandlers.entrySet()) {
          if (positiveNetworkMsgHandlers.containsRow(e.getKey())) {
            throw new FSMException("currently we do not allow same event in different ports(besides pos/neg");
          }
          for (Map.Entry<FSMStateName, FSMMsgHandler> ee : e.getValue().entrySet()) {
            positiveNetworkMsgHandlers.put(e.getKey(), ee.getKey(), ee.getValue());
          }
        }
        positiveNetworkFallbackHandlers.putAll(fallbackHandlers);
      } else {
        negativeNetwork.addAll(msgHandlers.keySet());
        for (Map.Entry<Class, Map<FSMStateName, FSMMsgHandler>> e : msgHandlers.entrySet()) {
          if (negativeNetworkMsgHandlers.containsRow(e.getKey())) {
            throw new FSMException("currently we do not allow same event in different ports(besides pos/neg");
          }
          for (Map.Entry<FSMStateName, FSMMsgHandler> ee : e.getValue().entrySet()) {
            negativeNetworkMsgHandlers.put(e.getKey(), ee.getKey(), ee.getValue());
          }
        }
        negativeNetworkFallbackHandlers.putAll(fallbackHandlers);
      }
    }
  }

  public static class Port {

    private final Handlers parent;
    private final Class portType;
    private final boolean pp;

    private final Map<Class, Map<FSMStateName, FSMEventHandler>> eventHandlers = new HashMap<>();
    private final Map<Class, FSMEventHandler> fallbackHandlers = new HashMap<>();

    public Port(Handlers parent, Class portType, boolean pp) {
      this.parent = parent;
      this.portType = portType;
      this.pp = pp;
    }

    public Event onEvent(Class eventType) {
      return new Event(this, eventType);
    }

    private void buildEvent(Class eventType, Map<FSMStateName, FSMEventHandler> handlers,
      Optional<FSMEventHandler> fallback) {
      eventHandlers.put(eventType, handlers);
      if (fallback.isPresent()) {
        fallbackHandlers.put(eventType, fallback.get());
      }
    }

    public Handlers buildPort() throws FSMException {
      parent.buildPort(portType, pp, eventHandlers, fallbackHandlers);
      return parent;
    }
  }

  public static class NetworkPort {

    private final Handlers parent;
    private final boolean pp;

    private final Map<Class, Map<FSMStateName, FSMMsgHandler>> msgHandlers = new HashMap<>();
    private final Map<Class, FSMMsgHandler> fallbackHandlers = new HashMap<>();

    public NetworkPort(Handlers parent, boolean pp) {
      this.parent = parent;
      this.pp = pp;
    }

    public Msg onMsg(Class eventType) {
      return new Msg(this, eventType);
    }

    private void buildMsg(Class eventType, Map<FSMStateName, FSMMsgHandler> handlers, Optional<FSMMsgHandler> fallback) {
      msgHandlers.put(eventType, handlers);
      if (fallback.isPresent()) {
        fallbackHandlers.put(eventType, fallback.get());
      }
    }

    public Handlers buildPort() throws FSMException {
      parent.buildNetwork(pp, msgHandlers, fallbackHandlers);
      return parent;
    }
  }

  public static class Event {

    private final Port parent;
    private final Class eventType;
    private final Map<FSMStateName, FSMEventHandler> handlers = new HashMap<>();
    private Optional<FSMEventHandler> fallback = Optional.absent();

    private Event(Port parent, Class eventType) {
      this.parent = parent;
      this.eventType = eventType;
    }

    public Event subscribe(FSMEventHandler handler, FSMStateName... states) throws FSMException {
      for (FSMStateName state : states) {
        if (handlers.containsKey(state)) {
          throw new FSMException("handler already registered for state:" + state + " event:" + eventType);
        }
        handlers.put(state, handler);
      }
      return this;
    }

    public Event fallback(FSMEventHandler handler) {
      fallback = Optional.of(handler);
      return this;
    }

    public Port buildEvent() {
      parent.buildEvent(eventType, handlers, fallback);
      return parent;
    }

    //*********SHORTCUTS**********
    public Event onEvent(Class eventType) {
      return buildEvent().onEvent(eventType);
    }

    public Handlers buildEvents() throws FSMException {
      parent.buildEvent(eventType, handlers, fallback);
      return parent.buildPort();
    }
  }

  public static class Msg {

    private final NetworkPort parent;
    private final Class eventType;
    private final Map<FSMStateName, FSMMsgHandler> handlers = new HashMap<>();
    private Optional<FSMMsgHandler> fallback = Optional.absent();

    private Msg(NetworkPort parent, Class eventType) {
      this.parent = parent;
      this.eventType = eventType;
    }

    public Msg subscribe(FSMMsgHandler handler, FSMStateName... states) throws FSMException {
      for (FSMStateName state : states) {
        if (handlers.containsKey(state)) {
          throw new FSMException("handler already registered for state:" + state + " event:" + eventType);
        }
        handlers.put(state, handler);
      }
      return this;
    }

    public Msg fallback(FSMMsgHandler handler) {
      fallback = Optional.of(handler);
      return this;
    }

    public NetworkPort buildEvent() {
      parent.buildMsg(eventType, handlers, fallback);
      return parent;
    }

    //*********SHORTCUTS**********
    public Msg onMsg(Class eventType) {
      return buildEvent().onMsg(eventType);
    }

    public Handlers buildMsgs() throws FSMException {
      parent.buildMsg(eventType, handlers, fallback);
      return parent.buildPort();
    }
  }

  public static Machine machine() {
    return new Machine();
  }

  public static Handlers events() {
    return new Handlers();
  }

  public static FSMachineDef fsmDef(String fsmName, Machine m, Handlers h) throws FSMException {
    FSMDefId id = FSMIds.getDefId(fsmName);

    if (!m.transitionTable.containsRow(FSMBasicStateNames.START)) {
      throw new FSMException("START state not defined");
    }
    if (!m.transitionTable.containsColumn(FSMBasicStateNames.FINAL)) {
      throw new FSMException("FINAL state not defined");
    }
    Sets.SetView<FSMStateName> deadState = Sets.
      difference(m.transitionTable.columnKeySet(), m.transitionTable.rowKeySet());
    if (deadState.size() > 1) {
      throw new FSMException("states:" + deadState.toString()
        + "are dead end states. Only FINAL allowed as dead end state.");
    }

    for (Map.Entry<FSMStateName, FSMStateDef> stateDef : m.states.entrySet()) {
      stateDef.getValue().setNegativeHandlers(h.negativePortEventHandlers.column(stateDef.getKey()));
      stateDef.getValue().setPositiveHandlers(h.positivePortEventHandlers.column(stateDef.getKey()));
      stateDef.getValue().setNegativeNetworkHandlers(h.negativeNetworkMsgHandlers.column(stateDef.getKey()));
      stateDef.getValue().setPositiveNetworkHandlers(h.positiveNetworkMsgHandlers.column(stateDef.getKey()));
    }

    FSMachineDef fsmDef = FSMachineDef.instance(id, m.states, m.transitionTable,
      h.fallbackEventHandler, h.fallbackMsgHandler,
      h.positivePortFallbackHandlers, h.negativePortFallbackHandlers,
      h.positiveNetworkFallbackHandlers, h.negativeNetworkFallbackHandlers);

    return fsmDef;
  }

  public static MultiFSM multiFSM(String fsmName, Machine m, Handlers h, FSMExternalState es,
    FSMInternalStateBuilder isb, OnFSMExceptionAction oexa, FSMIdExtractor fsmIdExtractor) throws FSMException {

    FSMachineDef fsmDef = fsmDef(fsmName, m, h);

    MultiFSM multiFSM = MultiMachine.instance()
      .setPositivePorts(h.positivePorts)
      .setNegativePorts(h.negativePorts)
      .setPositiveNetworkMsgs(h.positiveNetwork)
      .setNegativeNetworkMsgs(h.negativeNetwork)
      .buildMultiFSM(fsmDef, oexa, es, isb, fsmIdExtractor);
    return multiFSM;
  }

}

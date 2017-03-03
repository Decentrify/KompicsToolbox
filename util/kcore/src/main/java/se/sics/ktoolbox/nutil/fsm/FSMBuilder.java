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
import com.google.common.collect.Sets.SetView;
import com.google.common.collect.Table;
import java.util.HashMap;
import java.util.Map;
import se.sics.ktoolbox.nutil.fsm.api.FSMBasicStateNames;
import se.sics.ktoolbox.nutil.fsm.api.FSMEvent;
import se.sics.ktoolbox.nutil.fsm.api.FSMException;
import se.sics.ktoolbox.nutil.fsm.api.FSMExternalState;
import se.sics.ktoolbox.nutil.fsm.api.FSMInternalState;
import se.sics.ktoolbox.nutil.fsm.api.FSMStateName;
import se.sics.ktoolbox.nutil.fsm.handler.FSMEventHandler;
import se.sics.ktoolbox.nutil.fsm.handler.FSMStateChangeHandler;
import se.sics.ktoolbox.nutil.fsm.ids.FSMDefId;
import se.sics.ktoolbox.nutil.fsm.ids.FSMIds;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FSMBuilder {

  public static class Machine {

    private final Table<FSMStateName, FSMStateName, Boolean> transitionTable
      = HashBasedTable.create();

    private Machine() {
    }

    public Transition onState(FSMStateName state) throws FSMException {
      if (transitionTable.containsRow(state)) {
        throw new FSMException("state:" + state + " already registered");
      }
      return new Transition(this, state);
    }

    private void buildTransition(FSMStateName from, FSMStateName[] toStates, Optional<FSMStateName> cleanupState,
      boolean toFinal) throws FSMException {

      if (toFinal && cleanupState.isPresent()) {
        throw new FSMException("if cleanup is present, final should not - go cleanup and that will lead to final");
      }
      for (FSMStateName to : toStates) {
        if (transitionTable.contains(from, to)) {
          throw new FSMException("transition from:" + from + " to:" + to
            + " already registered");
        }
        transitionTable.put(from, to, true);
      }
      if (cleanupState.isPresent()) {
        if (transitionTable.contains(from, cleanupState.get())) {
          throw new FSMException("transition from:" + from + " to:"
            + cleanupState.get() + " already registered");
        }
        transitionTable.put(from, cleanupState.get(), true);
      }
      if (toFinal) {
        transitionTable.put(from, FSMBasicStateNames.FINAL, true);
      }
    }

    public FSMachineDef complete(String fsmName, Handlers handlers) throws FSMException {
      FSMDefId id = FSMIds.getDefId(fsmName);

      if (!transitionTable.containsRow(FSMBasicStateNames.START)) {
        throw new FSMException("START state not defined");
      }
      if (!transitionTable.containsColumn(FSMBasicStateNames.FINAL)) {
        throw new FSMException("FINAL state not defined");
      }
      SetView<FSMStateName> deadState = Sets.difference(transitionTable.columnKeySet(), transitionTable.rowKeySet());
      if (deadState.size() > 1) {
        throw new FSMException("states:" + deadState.toString()
          + "are dead end states. Only FINAL allowed as dead end state.");
      }

      Map<FSMStateName, FSMStateDef> stateDefs = handlers.getStateDefs();
      SetView<FSMStateName> malformedStates = Sets.difference(stateDefs.keySet(), transitionTable.rowKeySet());
      if (!malformedStates.isEmpty()) {
        throw new FSMException("states:" + malformedStates.toString() + " have no event handlers");
      }
      return new FSMachineDef(id, stateDefs, transitionTable);
    }
  }

  public static class Transition {

    private final Machine parent;
    private final FSMStateName from;
    private FSMStateName[] toStates = new FSMStateName[0];
    private FSMStateName cleanupState = null;
    private boolean toFinal = false;

    private Transition(Machine parent, FSMStateName from) {
      this.parent = parent;
      this.from = from;
    }

    public Transition nextStates(FSMStateName... ids) {
      this.toStates = ids;
      return this;
    }

    public Transition cleanup(FSMStateName id) {
      this.cleanupState = id;
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
      parent.buildTransition(from, toStates, Optional.fromNullable(cleanupState), toFinal);
      return parent;
    }
  }

  public static class Handlers {

    private Events events = null;
    private Fallback fallback = null;
    private StateChange stateChange = null;
    //*********************
    private Table<FSMStateName, Class, FSMEventHandler> eventHandlers;
    private FSMEventHandler defaultFallback = new FSMEventHandler() {
      @Override
      public FSMStateName handle(FSMStateName state, FSMExternalState es, FSMInternalState is, FSMEvent event) {
        //default drop msgs silently
        return state;
      }
    };
    private Map<FSMStateName, FSMEventHandler> customFallback = new HashMap<>();
    private Map<FSMStateName, FSMStateChangeHandler> onEntryHandlers = new HashMap<>();
    private Map<FSMStateName, FSMStateChangeHandler> onExitHandlers = new HashMap<>();

    private Handlers() {
    }

    public Events events() throws FSMException {
      if (events == null) {
        events = new Events(this);
      }
      return events;
    }

    private void buildEvents(Table<FSMStateName, Class, FSMEventHandler> eventHandlers) {
      this.eventHandlers = eventHandlers;
    }

    public Fallback fallback() throws FSMException {
      return fallback(defaultFallback);
    }

    public Fallback fallback(FSMEventHandler defaultFallback) throws FSMException {
      if (fallback == null) {
        fallback = new Fallback(this, defaultFallback);
      }
      return fallback;
    }

    private void buildFallbacks(FSMEventHandler defaultFallback, Map<FSMStateName, FSMEventHandler> customFallback) {
      this.defaultFallback = defaultFallback;
      this.customFallback = customFallback;
    }

    public StateChange stateChanges() throws FSMException {
      if (stateChange == null) {
        stateChange = new StateChange(this);
      }
      return stateChange;
    }

    private void buildStateChanges(Map<FSMStateName, FSMStateChangeHandler> onEntryHandlers,
      Map<FSMStateName, FSMStateChangeHandler> onExitHandlers) {
      this.onEntryHandlers = onEntryHandlers;
      this.onExitHandlers = onExitHandlers;
    }

    private Map<FSMStateName, FSMStateDef> getStateDefs() throws FSMException {
      if (events == null || eventHandlers.isEmpty()) {
        throw new FSMException("event handlers not set");
      }
      Map<FSMStateName, FSMStateDef> stateDefs = new HashMap<>();
      for (Map.Entry<FSMStateName, Map<Class, FSMEventHandler>> e : eventHandlers.rowMap().entrySet()) {
        FSMEventHandler f = customFallback.get(e.getKey());
        if (f == null) {
          f = defaultFallback;
        }
        Optional<FSMStateChangeHandler> onEntry = Optional.fromNullable(onEntryHandlers.get(e.getKey()));
        Optional<FSMStateChangeHandler> onExit = Optional.fromNullable(onExitHandlers.get(e.getKey()));
        FSMStateDef stateDef = new FSMStateDef(f, onEntry, onExit, new HashMap<>(e.getValue()));
        stateDefs.put(e.getKey(), stateDef);
      }
      return stateDefs;
    }
  }

  public static class Events {

    private Handlers parent;
    private final Table<FSMStateName, Class, FSMEventHandler> eventHandlers = HashBasedTable.create();

    public Events(Handlers parent) {
      this.parent = parent;
    }

    public Event onEvent(Class eventType) {
      return new Event(this, eventType);
    }

    private void buildEvent(Class eventType, Map<FSMStateName, FSMEventHandler> handlers) {
      for (Map.Entry<FSMStateName, FSMEventHandler> handler : handlers.entrySet()) {
        eventHandlers.put(handler.getKey(), eventType, handler.getValue());
      }
    }

    public Handlers buildEvents() throws FSMException {
      if (parent == null) {
        throw new FSMException("this events builder was closed once already");
      }
      parent.buildEvents(eventHandlers);
      Handlers aux = parent;
      parent = null;
      return aux;
    }
  }

  public static class Event {

    private final Events parent;
    private final Class eventType;
    private final Map<FSMStateName, FSMEventHandler> handlers = new HashMap<>();

    private Event(Events parent, Class eventType) {
      this.parent = parent;
      this.eventType = eventType;
    }

    public Event inState(FSMStateName state, FSMEventHandler handler) throws FSMException {
      if (handlers.containsKey(state)) {
        throw new FSMException("handler already registered for state:" + state + " event:" + eventType);
      }
      handlers.put(state, handler);
      return this;
    }

    public Events buildEvent() {
      parent.buildEvent(eventType, handlers);
      return parent;
    }

    //public shortcut on buildEvent - so you can chain events
    public Event onEvent(Class eventType) {
      return buildEvent().onEvent(eventType);
    }

    public Handlers buildEvents() throws FSMException {
      return parent.buildEvents();
    }
  }

  public static class Fallback {

    private Handlers parent;
    private final FSMEventHandler defaultFallback;
    private final Map<FSMStateName, FSMEventHandler> customFallback = new HashMap<>();

    private Fallback(Handlers parent, FSMEventHandler defaultFallback) {
      this.parent = parent;
      this.defaultFallback = defaultFallback;
    }

    public Fallback inState(FSMStateName state, FSMEventHandler fallback) throws FSMException {
      if (customFallback.containsKey(state)) {
        throw new FSMException("fallback handler already registered for state:" + state);
      }
      customFallback.put(state, fallback);
      return this;
    }

    public Handlers buildFallbacks() throws FSMException {
      if (parent == null) {
        throw new FSMException("this fallback builder was closed once already");
      }
      parent.buildFallbacks(defaultFallback, customFallback);
      Handlers aux = parent;
      parent = null;
      return aux;
    }
  }

  public static class StateChange {

    private Handlers parent;
    private final Map<FSMStateName, FSMStateChangeHandler> onEntryHandlers = new HashMap<>();
    private final Map<FSMStateName, FSMStateChangeHandler> onExitHandlers = new HashMap<>();

    private StateChange(Handlers parent) {
      this.parent = parent;
    }

    public StateChange onEntry(FSMStateName state, FSMStateChangeHandler handler) throws FSMException {
      if (onEntryHandlers.containsKey(state)) {
        throw new FSMException("onEntry handler already registered for state:" + state);
      }
      onEntryHandlers.put(state, handler);
      return this;
    }

    public StateChange onExit(FSMStateName state, FSMStateChangeHandler handler) throws FSMException {
      if (onExitHandlers.containsKey(state)) {
        throw new FSMException("onExit handler already registered for state:" + state);
      }
      onExitHandlers.put(state, handler);
      return this;
    }

    public StateChange on(FSMStateName state, FSMStateChangeHandler onEntry, FSMStateChangeHandler onExit)
      throws FSMException {
      onEntry(state, onEntry);
      onExit(state, onExit);
      return this;
    }

    public Handlers buildStateChanges() throws FSMException {
      if (parent == null) {
        throw new FSMException("this events builder was closed once already");
      }
      parent.buildStateChanges(onEntryHandlers, onExitHandlers);
      Handlers aux = parent;
      parent = null;
      return aux;
    }
  }

  public static Machine machine() {
    return new Machine();
  }

  public static Handlers handlers() {
    return new Handlers();
  }
}

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
import com.google.common.collect.Table;
import java.util.HashMap;
import java.util.Map;
import se.sics.ktoolbox.nutil.fsm.FSMStateDef;
import se.sics.ktoolbox.nutil.fsm.FSMachineDef;
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

    public final FSMDefId id;
    private final Map<FSMStateName, FSMStateDef> stateDefs = new HashMap<>();
    private final Table<FSMStateName, FSMStateName, Boolean> transitionTable
      = HashBasedTable.create();

    private Machine(FSMDefId id) {
      this.id = id;
    }

    public State onState(FSMStateName state) throws FSMException {
      if (stateDefs.containsKey(state)) {
        throw new FSMException("state:" + state + " already registered");
      }
      return new State(this, state);
    }

    public Transition withState(FSMStateName state, FSMStateDef stateDef) {
      stateDefs.put(state, stateDef);
      return new Transition(this, state);
    }

    private Transition buildState(FSMStateName state, FSMStateDef stateDef) {
      stateDefs.put(state, stateDef);
      return new Transition(this, state);
    }

    private void buildTransition(FSMStateName from, FSMStateName[] toStates,
      Optional<FSMStateName> cleanupState) throws
      FSMException {
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
      }
      transitionTable.put(from, cleanupState.get(), true);
    }

    public FSMachineDef complete() throws FSMException {
      if (!stateDefs.containsKey(FSMBasicStateNames.START)) {
        throw new FSMException("start state undefined");
      }
      for (Table.Cell<FSMStateName, FSMStateName, Boolean> e : transitionTable.
        cellSet()) {
        if (!stateDefs.containsKey(e.getColumnKey())) {
          throw new FSMException("transition from state:" + e.getColumnKey()
            + " undefined");
        }
        if (FSMBasicStateNames.FINAL.equals(e.getRowKey())) {
          continue;
        }
        if (!stateDefs.containsKey(e.getRowKey())) {
          throw new FSMException("transition to state:" + e.getRowKey()
            + " undefined");
        }
      }
      return new FSMachineDef(id, stateDefs, transitionTable);
    }
  }

  public static class Transition {

    private final Machine parent;
    private final FSMStateName from;
    private FSMStateName[] toStates = new FSMStateName[0];
    private FSMStateName cleanupState = null;

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

    public Machine buildTransition() throws FSMException {
      if (toStates == null) {
        throw new FSMException("to states not registered");
      }
      parent.buildTransition(from, toStates, Optional.fromNullable(cleanupState));
      return parent;
    }
  }

  public static class State {

    private static final FSMEventHandler defaultFallback = new FSMEventHandler() {
      @Override
      public FSMStateName handle(FSMStateName state, FSMExternalState es, FSMInternalState is, FSMEvent event) {
        //default drop msgs silently
        return state;
      }
    };
    private final Machine parent;
    private final FSMStateName state;
    private FSMEventHandler fallback = defaultFallback;
    private Optional<FSMStateChangeHandler> onEntry = Optional.absent();
    private Optional<FSMStateChangeHandler> onExit = Optional.absent();
    private final Map<Class, FSMEventHandler> handlers = new HashMap<>();

    private State(Machine parent, FSMStateName state) {
      this.parent = parent;
      this.state = state;
    }

    public State fallback(FSMEventHandler handler) {
      this.fallback = handler;
      return this;
    }

    public State onEntry(FSMStateChangeHandler handler) {
      this.onEntry = Optional.of(handler);
      return this;
    }

    public State onExit(FSMStateChangeHandler handler) {
      this.onExit = Optional.of(handler);
      return this;
    }

    public State onEvent(Class<? extends FSMEvent> event, FSMEventHandler handler) throws FSMException {
      if (handlers.containsKey(event)) {
        throw new FSMException("Handler already registered for event:" + event + " in state:" + state);
      }
      handlers.put(event, handler);
      return this;
    }

    public Transition buildState() throws FSMException {
      if (handlers.isEmpty()) {
        throw new FSMException("state:" + state + " has no handlers");
      }
      FSMStateDef stateDef = new FSMStateDef(fallback, onEntry, onExit, handlers);
      return parent.buildState(state, stateDef);
    }
  }

  public static Machine builder(String fsmName) {
    return new Machine(FSMIds.getDefId(fsmName));
  }
}

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
import se.sics.ktoolbox.nutil.fsm.ids.FSMDefId;
import se.sics.ktoolbox.nutil.fsm.ids.FSMIds;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FSMachineDef {

  public final FSMDefId id;
  private final Map<FSMStateName, FSMStateDef> stateDefs;
  private final Table<FSMStateName, FSMStateName, Boolean> transitionTable;

  private FSMachineDef(FSMDefId id, Map<FSMStateName, FSMStateDef> stateDefs,
    Table<FSMStateName, FSMStateName, Boolean> transitionTable) {
    this.id = id;
    this.stateDefs = stateDefs;
    this.transitionTable = transitionTable;
  }

  public FSMachine build(Identifier baseId, FSMOnKillAction oka, FSMExternalState es, FSMInternalState is)
    throws FSMException {

    Map<FSMStateName, FSMState> states = new HashMap<>();
    for (Map.Entry<FSMStateName, FSMStateDef> e : stateDefs.entrySet()) {
      FSMState state = e.getValue().build(e.getKey(), es, is);
      states.put(e.getKey(), state);
    }

    return new FSMachine(id.getFSMId(baseId), oka, states, transitionTable);
  }

  public static class Builder {

    public final FSMDefId id;
    private final Map<FSMStateName, FSMStateDef> stateDefs = new HashMap<>();
    private final Table<FSMStateName, FSMStateName, Boolean> transitionTable = HashBasedTable.create();

    private Builder(FSMDefId id) {
      this.id = id;
    }

    public AuxBuilder fromState(FSMStateName from, FSMStateDef stateDef) throws FSMException {
      if (stateDefs.containsKey(from)) {
        throw new FSMException("state:" + from + " already registered");
      }
      stateDefs.put(from, stateDef);
      return new AuxBuilder(this, from);
    }

    private void buildState(FSMStateName from, FSMStateName[] toStates, Optional<FSMStateName> cleanupState) throws
      FSMException {
      for (FSMStateName to : toStates) {
        if (transitionTable.contains(from, to)) {
          throw new FSMException("transition from:" + from + " to:" + to + " already registered");
        }
        transitionTable.put(from, to, true);
      }
      if (cleanupState.isPresent()) {
        if (transitionTable.contains(from, cleanupState.get())) {
          throw new FSMException("transition from:" + from + " to:" + cleanupState.get() + " already registered");
        }
      }
      transitionTable.put(from, cleanupState.get(), true);
    }

    public FSMachineDef complete() throws FSMException {
      if (!stateDefs.containsKey(FSMBasicStateNames.START)) {
        throw new FSMException("start state undefined");
      }
      for (Table.Cell<FSMStateName, FSMStateName, Boolean> e : transitionTable.cellSet()) {
        if (!stateDefs.containsKey(e.getColumnKey())) {
          throw new FSMException("transition from state:" + e.getColumnKey() + " undefined");
        }
        if(FSMBasicStateNames.FINAL.equals(e.getRowKey())) {
          continue;
        }
        if (!stateDefs.containsKey(e.getRowKey())) {
          throw new FSMException("transition to state:" + e.getRowKey() + " undefined");
        }
      }
      return new FSMachineDef(id, stateDefs, transitionTable);
    }
  }

  public static class AuxBuilder {

    private final Builder mb;
    private final FSMStateName from;
    private FSMStateName[] toStates = new FSMStateName[0];
    private FSMStateName cleanupState = null;

    private AuxBuilder(Builder mb, FSMStateName from) {
      this.mb = mb;
      this.from = from;
    }

    public AuxBuilder toStates(FSMStateName... ids) {
      this.toStates = ids;
      return this;
    }

    public AuxBuilder cleanup(FSMStateName id) {
      this.cleanupState = id;
      return this;
    }

    public Builder buildState() throws FSMException {
      if (toStates == null) {
        throw new FSMException("to states not registered");
      }
      mb.buildState(from, toStates, Optional.fromNullable(cleanupState));
      return mb;
    }
  }

  public static Builder builder(String fsmName) {
    return new Builder(FSMIds.getDefId(fsmName));
  }
}

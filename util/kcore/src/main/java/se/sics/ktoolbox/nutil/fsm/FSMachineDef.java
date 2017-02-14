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

import java.util.HashMap;
import java.util.Map;
import org.javatuples.Pair;
import se.sics.ktoolbox.nutil.fsm.ids.FSMDefId;
import se.sics.ktoolbox.nutil.fsm.ids.FSMIds;
import se.sics.ktoolbox.nutil.fsm.ids.FSMStateDefId;
import se.sics.ktoolbox.nutil.fsm.ids.FSMStateId;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FSMachineDef {

  public final FSMDefId id;
  private FSMStateDefId initState = null;
  private final Map<FSMStateDefId, FSMStateDef> stateDefs = new HashMap<>();
  private final Map<FSMTransition, Pair<FSMStateDefId, FSMStateDefId>> transitionTable = new HashMap<>();
  private boolean sealed = false;
  private byte stateDefId = 0;

  private FSMachineDef(FSMDefId id) {
    this.id = id;
  }

  public FSMStateDefId registerInitState(FSMStateDef stateDef) throws FSMException {
    if (sealed) {
      throw new FSMException("trying to register state definition after definition has been sealed");
    }
    if (initState != null) {
      FSMStateDef sd = stateDefs.get(initState);
      if (sd == null) {
        throw new FSMException("FSM logic error");
      }
      throw new FSMException("init state defintion:" + sd.getClass() + " id:" + sd.getId() + " already registered");
    }
    stateDef.setId(id.getFSMStateDefId(stateDefId++));
    stateDefs.put(stateDef.getId(), stateDef);
    initState = stateDef.getId();
    return initState;
  }

  public FSMStateDefId registerState(FSMStateDef stateDef) throws FSMException {
    if (sealed) {
      throw new FSMException("trying to register state definition after definition has been sealed");
    }
    if (initState == null) {
      throw new FSMException("trying to register state definition before init definition registered");
    }
    stateDef.setId(id.getFSMStateDefId(stateDefId++));
    stateDefs.put(stateDef.getId(), stateDef);
    return stateDef.getId();
  }

  public void register(FSMTransition transition, FSMStateDefId from, FSMStateDefId to) throws FSMException {
    if (sealed) {
      throw new FSMException("Trying to register transition after definition has been sealed");
    }
    if (transitionTable.containsKey(transition)) {
      throw new FSMException("transition:" + transition + " already registered");
    }
    if (!stateDefs.containsKey(from)) {
      throw new FSMException("transition:" + transition + " - undefined from state:" + from);
    }
    if (!stateDefs.containsKey(to)) {
      throw new FSMException("transition:" + transition + " - undefined to state:" + to);
    }
    transitionTable.put(transition, Pair.with(from, to));
  }

  public void seal() throws FSMException {
    if (initState == null) {
      throw new FSMException("trying to seal an incomplete definition - missing init state");
    }
    sealed = true;
  }

  public FSMachine build(Identifier baseId, FSMOnKillAction oka, FSMExternalState es,
    FSMInternalState is) throws FSMException {
    if (!sealed) {
      throw new FSMException("Trying to build an unsealed definition");
    }

    Map<FSMStateId, FSMState> states = new HashMap<>();
    for (Map.Entry<FSMStateDefId, FSMStateDef> e : stateDefs.entrySet()) {
      FSMState state = e.getValue().build(baseId, es, is);
      states.put(state.id, state);
    }

    Map<FSMTransition, Pair<FSMStateId, FSMStateId>> tt = new HashMap<>();
    for (Map.Entry<FSMTransition, Pair<FSMStateDefId, FSMStateDefId>> e : transitionTable.entrySet()) {
      FSMStateId from = e.getValue().getValue0().getStateId(baseId);
      FSMStateId to = e.getValue().getValue1().getStateId(baseId);
      tt.put(e.getKey(), Pair.with(from, to));
    }
    FSMState initS = states.get(initState.getStateId(baseId));
    return new FSMachine(id.getFSMId(baseId), oka, states, tt, initS);
  }

  public static FSMachineDef instance(String fsmName) {
    return new FSMachineDef(FSMIds.getDefId(fsmName));
  }
}

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

import se.sics.ktoolbox.nutil.fsm.api.FSMStateName;
import se.sics.ktoolbox.nutil.fsm.api.FSMOnKillAction;
import se.sics.ktoolbox.nutil.fsm.api.FSMInternalState;
import se.sics.ktoolbox.nutil.fsm.api.FSMException;
import se.sics.ktoolbox.nutil.fsm.api.FSMExternalState;
import com.google.common.collect.Table;
import java.util.HashMap;
import java.util.Map;
import se.sics.ktoolbox.nutil.fsm.ids.FSMDefId;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FSMachineDef {

  public final FSMDefId id;
  private final Map<FSMStateName, FSMStateDef> stateDefs;
  private final Table<FSMStateName, FSMStateName, Boolean> transitionTable;

  FSMachineDef(FSMDefId id, Map<FSMStateName, FSMStateDef> stateDefs,
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
}

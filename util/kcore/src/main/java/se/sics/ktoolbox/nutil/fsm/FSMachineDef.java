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

import com.google.common.collect.Table;
import java.util.HashMap;
import java.util.Map;
import se.sics.kompics.id.Identifier;
import se.sics.ktoolbox.nutil.fsm.api.FSMException;
import se.sics.ktoolbox.nutil.fsm.api.FSMExternalState;
import se.sics.ktoolbox.nutil.fsm.api.FSMInternalState;
import se.sics.ktoolbox.nutil.fsm.api.FSMOnKillAction;
import se.sics.ktoolbox.nutil.fsm.api.FSMStateName;
import se.sics.ktoolbox.nutil.fsm.handler.FSMEventHandler;
import se.sics.ktoolbox.nutil.fsm.handler.FSMMsgHandler;
import se.sics.ktoolbox.nutil.fsm.ids.FSMDefId;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FSMachineDef {

  public final FSMDefId id;
  private final Map<FSMStateName, FSMStateDef> stateDefs;
  private final Table<FSMStateName, FSMStateName, Boolean> transitionTable;
  private final FSMEventHandler fallbackEventHandler;
  private final FSMMsgHandler fallbackMsgHandler;
  private final Map<Class, FSMEventHandler> positiveEventFallbackHandlers;
  private final Map<Class, FSMEventHandler> negativeEventFallbackHandlers;
  private final Map<Class, FSMMsgHandler>  positiveMsgFallbackHandlers;
  private final Map<Class, FSMMsgHandler>  negativeMsgFallbackHandlers;

  private FSMachineDef(FSMDefId id, Map<FSMStateName, FSMStateDef> stateDefs,
    Table<FSMStateName, FSMStateName, Boolean> transitionTable, 
    FSMEventHandler fallbackEventHandler, FSMMsgHandler fallbackMsgHandler,
    Map<Class, FSMEventHandler> positiveEventFallbackHandlers, Map<Class, FSMEventHandler> negativeEventFallbackHandlers,
    Map<Class, FSMMsgHandler>  positiveMsgFallbackHandlers, Map<Class, FSMMsgHandler>  negativeMsgFallbackHandlers) {
    this.id = id;
    this.stateDefs = stateDefs;
    this.transitionTable = transitionTable;
    this.fallbackEventHandler = fallbackEventHandler;
    this.fallbackMsgHandler = fallbackMsgHandler;
    this.positiveEventFallbackHandlers = positiveEventFallbackHandlers;
    this.negativeEventFallbackHandlers = negativeEventFallbackHandlers;
    this.positiveMsgFallbackHandlers = positiveMsgFallbackHandlers;
    this.negativeMsgFallbackHandlers = negativeMsgFallbackHandlers;
  }

  public FSMachine build(Identifier baseId, FSMOnKillAction oka, FSMExternalState es, FSMInternalState is)
    throws FSMException {

    Map<FSMStateName, FSMState> states = new HashMap<>();
    for (Map.Entry<FSMStateName, FSMStateDef> e : stateDefs.entrySet()) {
      FSMState state = e.getValue().build(e.getKey(), es, is);
      states.put(e.getKey(), state);
    }

    return new FSMachine(id.getFSMId(baseId), oka, states, transitionTable, fallbackEventHandler, fallbackMsgHandler,
      positiveEventFallbackHandlers,negativeEventFallbackHandlers, positiveMsgFallbackHandlers, negativeMsgFallbackHandlers);
  }
  
  public static FSMachineDef instance(FSMDefId id, Map<FSMStateName, FSMStateDef> stateDefs,
    Table<FSMStateName, FSMStateName, Boolean> transitionTable, 
    FSMEventHandler fallbackEventHandler, FSMMsgHandler fallbackMsgHandler,
    Map<Class, FSMEventHandler> positiveEventFallbackHandlers, Map<Class, FSMEventHandler> negativeEventFallbackHandlers,
    Map<Class, FSMMsgHandler>  positiveMsgFallbackHandlers, Map<Class, FSMMsgHandler>  negativeMsgFallbackHandlers) {
    return new FSMachineDef(id, stateDefs, transitionTable, fallbackEventHandler, fallbackMsgHandler,
      positiveEventFallbackHandlers, negativeEventFallbackHandlers, positiveMsgFallbackHandlers,
      negativeMsgFallbackHandlers);
  }
  
  public static FSMachineDef instance(FSMDefId id, Map<FSMStateName, FSMStateDef> stateDefs,
    Table<FSMStateName, FSMStateName, Boolean> transitionTable, 
    Map<Class, FSMEventHandler> positiveEventFallbackHandlers, Map<Class, FSMEventHandler> negativeEventFallbackHandlers,
    Map<Class, FSMMsgHandler>  positiveMsgFallbackHandlers, Map<Class, FSMMsgHandler>  negativeMsgFallbackHandlers) {
    return new FSMachineDef(id, stateDefs, transitionTable, FSMachine.fallbackEventHandler, FSMachine.fallbackMsgHandler,
      positiveEventFallbackHandlers, negativeEventFallbackHandlers, positiveMsgFallbackHandlers,
      negativeMsgFallbackHandlers);
  }
}

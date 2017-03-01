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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.nutil.fsm.events.Event2;
import se.sics.ktoolbox.nutil.fsm.events.Port2;
import se.sics.ktoolbox.nutil.fsm.ids.FSMId;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FSM2 {
  private static final Logger LOG = LoggerFactory.getLogger(FSM2.class);
  
  public static FSMachineDef build() throws FSMException {
    FSMOnWrongStateAction owsa = new FSMOnWrongStateAction<MyExternalState, InternalState>() {
      @Override
      public void handle(FSMStateName state, FSMEvent event, MyExternalState es, InternalState is) {
        LOG.warn("state:{} does not handle event:{}", state, event);
      }
    };
    FSMachineDef.Builder builder = FSMachineDef.builder(FSMs.fsm2);
    
    FSMStateDef startState = initState(owsa);
    
    FSMachineDef fsm = builder
      .fromState(FSMBasicStateNames.START, startState).toStates(FSMBasicStateNames.START).buildState()
      .complete();
    
    return fsm;
  }

  private static FSMStateDef initState(FSMOnWrongStateAction owsa) throws FSMException {
    FSMStateDef state = new FSMStateDef();
    state.setOnWrongStateAction(owsa);
    state.register(Event2.Req.class, initHandler1);
    state.seal();
    return state;
  }
  
  static FSMEventHandler initHandler1 = new FSMEventHandler<MyExternalState, InternalState, Event2.Req>() {
    @Override
    public FSMStateName handle(MyExternalState es, InternalState is, Event2.Req req) {
      LOG.info("1->2");
      es.getProxy().trigger(req, es.getProxy().getNegative(Port2.class));
      return FSMBasicStateNames.START;
    }
  };
  
  public static class InternalState implements FSMInternalState {
  }

  public static class Builder implements FSMInternalStateBuilder {

    @Override
    public FSMInternalState newState(FSMId fsmId) {
      return new InternalState();
    }
  }
}

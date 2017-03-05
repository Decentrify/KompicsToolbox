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
import se.sics.ktoolbox.nutil.fsm.api.FSMBasicStateNames;
import se.sics.ktoolbox.nutil.fsm.api.FSMEvent;
import se.sics.ktoolbox.nutil.fsm.api.FSMException;
import se.sics.ktoolbox.nutil.fsm.api.FSMInternalState;
import se.sics.ktoolbox.nutil.fsm.api.FSMInternalStateBuilder;
import se.sics.ktoolbox.nutil.fsm.api.FSMStateName;
import se.sics.ktoolbox.nutil.fsm.events.Event2;
import se.sics.ktoolbox.nutil.fsm.events.Port2;
import se.sics.ktoolbox.nutil.fsm.handler.FSMEventHandler;
import se.sics.ktoolbox.nutil.fsm.ids.FSMId;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FSM2 {
  private static final Logger LOG = LoggerFactory.getLogger(FSM2.class);
  
  public static FSMachineDef build() throws FSMException {
    FSMEventHandler owsa = new FSMEventHandler<MyExternalState, InternalState, FSMEvent>() {
      @Override
      public FSMStateName handle(FSMStateName state, MyExternalState es, InternalState is, FSMEvent event) {
        LOG.warn("state:{} does not handle event:{}", state, event);
        return state;
      }
    };
    FSMBuilder.Machine machine = FSMBuilder.machine()
      .onState(FSMBasicStateNames.START)
        .nextStates(FSMBasicStateNames.START)
        .buildTransition();
    
    FSMBuilder.Handlers handlers = FSMBuilder.handlers()
      .events()
        .onEvent(Event2.Req.class)
          .subscribe(initHandler1, FSMBasicStateNames.START)
      .buildEvents()
      .fallback(owsa)
      .buildFallbacks();
    
    FSMachineDef fsm = machine.complete(FSMs.fsm2, handlers);
    return fsm;
  }

  static FSMEventHandler initHandler1 = new FSMEventHandler<MyExternalState, InternalState, Event2.Req>() {
    @Override
    public FSMStateName handle(FSMStateName state, MyExternalState es, InternalState is, Event2.Req req) {
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

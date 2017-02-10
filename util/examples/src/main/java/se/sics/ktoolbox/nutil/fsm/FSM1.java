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
import se.sics.ktoolbox.nutil.fsm.events.Event1;
import se.sics.ktoolbox.nutil.fsm.ids.FSMId;
import se.sics.ktoolbox.nutil.fsm.ids.FSMStateDefId;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FSM1 {
  private static final Logger LOG = LoggerFactory.getLogger(FSM1.class);
  
  public static enum Transition implements FSMTransition {
    T1, T2, T3, T4
  }
  
  public static FSMachineDef build() throws FSMException {
    FSMachineDef fsm = FSMachineDef.instance(FSMs.fsm1);
    FSMStateDefId id1 = fsm.registerInitState(initState());
    FSMStateDefId id2 = fsm.registerState(state2());
    FSMStateDefId id3 = fsm.registerState(state3());
    fsm.register(Transition.T1, id1, id2);
    fsm.register(Transition.T2, id1, id3);
    fsm.register(Transition.T3, id2, id1);
    fsm.register(Transition.T4, id3, id2);
    fsm.seal();
    return fsm;
  }

  private static FSMStateDef initState() throws FSMException {
    FSMStateDef state = new FSMStateDef();
    state.register(Event1.E1.class, initHandler1);
    state.register(Event1.E2.class, initHandler2);
    state.seal();
    return state;
  }
  
  static FSMEventHandler initHandler1 = new FSMEventHandler<MyExternalState, InternalState, Event1.E1>() {
    @Override
    public FSMTransition handle(MyExternalState es, InternalState is, Event1.E1 event) {
      LOG.info("1->2");
      return Transition.T1;
    }
  };
  
  static FSMEventHandler initHandler2 = new FSMEventHandler<MyExternalState, InternalState, Event1.E2>() {
    @Override
    public FSMTransition handle(MyExternalState es, InternalState is, Event1.E2 event) {
      LOG.info("1->3");
      return Transition.T2;
    }
  };
    
  private static FSMStateDef state2() throws FSMException {
    FSMStateDef state = new FSMStateDef();
    state.register(Event1.E3.class, state2Handler1);
    state.seal();
    return state;
  }
  
  static FSMEventHandler state2Handler1 = new FSMEventHandler<MyExternalState, InternalState, Event1.E3>() {
    @Override
    public FSMTransition handle(MyExternalState es, InternalState is, Event1.E3 event) {
      LOG.info("2->1");
      return Transition.T3;
    }
  };
  
  private static FSMStateDef state3() throws FSMException {
    FSMStateDef state = new FSMStateDef();
    state.register(Event1.E4.class, state3Handler1);
    state.seal();
    return state;
  }
  
  static FSMEventHandler state3Handler1 = new FSMEventHandler<MyExternalState, InternalState, Event1.E4>() {
    @Override
    public FSMTransition handle(MyExternalState es, InternalState is, Event1.E4 event) {
      LOG.info("3->2");
      return Transition.T4;
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

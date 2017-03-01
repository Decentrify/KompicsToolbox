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

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FSM1 {

  private static final Logger LOG = LoggerFactory.getLogger(FSM1.class);

  public static enum MyStateNames implements FSMStateName {
    S2,
    S3
  }

  public static FSMachineDef build() throws FSMException {
    FSMOnWrongStateAction owsa = new FSMOnWrongStateAction<MyExternalState, InternalState>() {

      @Override
      public void handle(FSMStateName state, FSMEvent event, MyExternalState es, InternalState is) {
        LOG.warn("state:{} does not handle event:{}", state, event);
      }
    };
    
    FSMachineDef.Builder builder = FSMachineDef.builder(FSMs.fsm1);

    FSMStateDef s1 = state1(owsa);
    FSMStateDef s2 = state2(owsa);
    FSMStateDef s3 = state3(owsa);

    FSMachineDef fsm = builder
      .fromState(FSMBasicStateNames.START, s1).toStates(MyStateNames.S2, MyStateNames.S3).buildState()
      .fromState(MyStateNames.S2, s2).toStates(FSMBasicStateNames.START).buildState()
      .fromState(MyStateNames.S3, s3).toStates(FSMBasicStateNames.FINAL).buildState()
      .complete();

    return fsm;
  }

  static FSMStateDef state1(FSMOnWrongStateAction owsa) throws FSMException {
    FSMStateDef state = new FSMStateDef();
    state.setOnWrongStateAction(owsa);
    state.register(Event1.E1.class, state1Handler1);
    state.register(Event1.E2.class, state1Handler2);
    state.seal();
    return state;
  }

  static FSMEventHandler state1Handler1 = new FSMEventHandler<MyExternalState, InternalState, Event1.E1>() {
    @Override
    public FSMStateName handle(MyExternalState es, InternalState is, Event1.E1 event) {
      LOG.info("1->2");
      return MyStateNames.S2;
    }
  };

  static FSMEventHandler state1Handler2 = new FSMEventHandler<MyExternalState, InternalState, Event1.E2>() {
    @Override
    public FSMStateName handle(MyExternalState es, InternalState is, Event1.E2 event) {
      LOG.info("1->3");
      return MyStateNames.S3;
    }
  };

  static FSMStateDef state2(FSMOnWrongStateAction owsa) throws FSMException {
    FSMStateDef state = new FSMStateDef();
    state.setOnWrongStateAction(owsa);
    state.register(Event1.E3.class, state2Handler);
    state.seal();
    return state;
  }

  static FSMEventHandler state2Handler = new FSMEventHandler<MyExternalState, InternalState, Event1.E3>() {
    @Override
    public FSMStateName handle(MyExternalState es, InternalState is, Event1.E3 event) {
      LOG.info("2->1");
      return FSMBasicStateNames.START;
    }
  };

  static FSMStateDef state3(FSMOnWrongStateAction owsa) throws FSMException {
    FSMStateDef state = new FSMStateDef();
    state.setOnWrongStateAction(owsa);
    state.register(Event1.E4.class, state3Handler);
    state.seal();
    return state;
  }

  static FSMEventHandler state3Handler = new FSMEventHandler<MyExternalState, InternalState, Event1.E4>() {
    @Override
    public FSMStateName handle(MyExternalState es, InternalState is, Event1.E4 event) {
      LOG.info("3->stop");
      return FSMBasicStateNames.FINAL;
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

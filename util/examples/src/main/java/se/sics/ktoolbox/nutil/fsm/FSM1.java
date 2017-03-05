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
import se.sics.ktoolbox.nutil.fsm.events.Event1;
import se.sics.ktoolbox.nutil.fsm.handler.FSMEventHandler;
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
    FSMEventHandler owsa = new FSMEventHandler<MyExternalState, InternalState, FSMEvent>() {

      @Override
      public FSMStateName handle(FSMStateName state, MyExternalState es, InternalState is, FSMEvent event) {
        LOG.warn("state:{} does not handle event:{}", state, event);
        return state;
      }
    };
    
    FSMBuilder.Machine machine = FSMBuilder.machine()
      .onState(FSMBasicStateNames.START)
        .nextStates(MyStateNames.S2, MyStateNames.S3)
        .buildTransition()
      .onState(MyStateNames.S2)
        .nextStates(FSMBasicStateNames.START)
        .buildTransition()
      .onState(MyStateNames.S3)
        .toFinal()
        .buildTransition();

    FSMBuilder.Handlers handlers = FSMBuilder.handlers()
      .events()
        .onEvent(Event1.E1.class)
          .subscribe(state1Handler1, FSMBasicStateNames.START)
        .onEvent(Event1.E2.class)
          .subscribe(state1Handler2, FSMBasicStateNames.START)
        .onEvent(Event1.E3.class)
          .subscribe(state2Handler, MyStateNames.S2)
        .onEvent(Event1.E4.class)
          .subscribe(state3Handler, MyStateNames.S3)
      .buildEvents()
      .fallback(owsa)
      .buildFallbacks();

    FSMachineDef fsm = machine.complete(FSMs.fsm1, handlers);
    return fsm;
  }

  static FSMEventHandler state1Handler1 = new FSMEventHandler<MyExternalState, InternalState, Event1.E1>() {
    @Override
    public FSMStateName handle(FSMStateName state, MyExternalState es, InternalState is, Event1.E1 event) {
      LOG.info("1->2");
      return MyStateNames.S2;
    }
  };

  static FSMEventHandler state1Handler2 = new FSMEventHandler<MyExternalState, InternalState, Event1.E2>() {
    @Override
    public FSMStateName handle(FSMStateName state, MyExternalState es, InternalState is, Event1.E2 event) {
      LOG.info("1->3");
      return MyStateNames.S3;
    }
  };


  static FSMEventHandler state2Handler = new FSMEventHandler<MyExternalState, InternalState, Event1.E3>() {
    @Override
    public FSMStateName handle(FSMStateName state, MyExternalState es, InternalState is, Event1.E3 event) {
      LOG.info("2->1");
      return FSMBasicStateNames.START;
    }
  };

  static FSMEventHandler state3Handler = new FSMEventHandler<MyExternalState, InternalState, Event1.E4>() {
    @Override
    public FSMStateName handle(FSMStateName state, MyExternalState es, InternalState is, Event1.E4 event) {
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

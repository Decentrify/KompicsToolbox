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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.ktoolbox.nutil.fsm.events.Event1;
import se.sics.ktoolbox.nutil.fsm.events.Event2;
import se.sics.ktoolbox.nutil.fsm.events.Port1;
import se.sics.ktoolbox.nutil.fsm.events.Port2;
import se.sics.ktoolbox.nutil.fsm.genericsetup.OnFSMExceptionAction;
import se.sics.ktoolbox.nutil.fsm.ids.FSMDefId;
import se.sics.ktoolbox.nutil.fsm.ids.FSMId;
import se.sics.ktoolbox.nutil.fsm.ids.FSMStateId;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdFactory;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class FSMHostComp extends ComponentDefinition {

  private static final Logger LOG = LoggerFactory.getLogger(FSMHostComp.class);
  private String logPrefix = "";

  private final Negative port1 = provides(Port1.class);
  private final Positive port2 = requires(Port2.class);
  private Component multiFSMComp;
  private final IdentifierFactory idFact;

  public FSMHostComp() {
    idFact = new UUIDIdFactory();
    subscribe(handleStart, control);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      LOG.info("{}starting...", logPrefix);
      try {
        connectFSM();
      } catch (FSMException ex) {
        throw new RuntimeException(ex);
      }
      startFSM();
    }
  };

  private Pair<List<Pair<Class, List<Class>>>, List<Pair<Class, List<Class>>>> defineFSMEvents() {
    List<Pair<Class, List<Class>>> positivePorts = new LinkedList<>();
    Class p1 = Port1.class;
    List<Class> pE1 = new LinkedList<>();
    pE1.add(Event1.E1.class);
    pE1.add(Event1.E2.class);
    pE1.add(Event1.E3.class);
    pE1.add(Event1.E4.class);
    positivePorts.add(Pair.with(p1, pE1));

    List<Pair<Class, List<Class>>> negativePorts = new LinkedList<>();
    Class n1 = Port2.class;
    List<Class> nE1 = new LinkedList<>();
    nE1.add(Event2.Req.class);
    negativePorts.add(Pair.with(n1, nE1));

    return Pair.with(positivePorts, negativePorts);
  }

  private void connectFSM() throws FSMException {
    Map<FSMDefId, FSMachineDef> fsms = new HashMap<>();
    final FSMachineDef fsm1 = FSM1.build();
    fsms.put(fsm1.id, fsm1);
    final FSMachineDef fsm2 = FSM2.build();
    fsms.put(fsm2.id, fsm2);

    Pair<List<Pair<Class, List<Class>>>, List<Pair<Class, List<Class>>>> events = defineFSMEvents();

    FSMIdExtractor fsmIdExtractor = new FSMIdExtractor() {
      Map<Class, FSMDefId> eventToFSM = new HashMap<>();

      {
        eventToFSM.put(Event1.E1.class, fsm1.id);
        eventToFSM.put(Event1.E2.class, fsm1.id);
        eventToFSM.put(Event1.E3.class, fsm1.id);
        eventToFSM.put(Event1.E4.class, fsm1.id);
        eventToFSM.put(Event2.Req.class, fsm2.id);
      }

      @Override
      public Optional<FSMId> fromEvent(FSMEvent event) throws FSMException {
        FSMDefId fsmDefId = eventToFSM.get(event.getClass());
        if (fsmDefId == null) {
          return Optional.absent();
        }
        return Optional.of(fsmDefId.getFSMId(event.getBaseId()));
      }
    };

    FSMInternalStateBuilders builders = new FSMInternalStateBuilders();
    try {
      builders.register(fsm1.id, new FSM1.Builder());
      builders.register(fsm2.id, new FSM2.Builder());
    } catch (FSMException ex) {
      throw new RuntimeException(ex);
    }

    OnFSMExceptionAction oexa = new OnFSMExceptionAction() {
      @Override
      public void handle(FSMException ex) {
        throw new RuntimeException(ex);
      }
    };
    
    FSMOnWrongStateAction owsa = new FSMOnWrongStateAction<MyExternalState, FSM1.InternalState>() {

      @Override
      public void handle(FSMStateId stateId, FSMEvent event, MyExternalState es, FSM1.InternalState is) {
        LOG.warn("state:{} does not handle event:{}", stateId, event);
      }
    };
    MultiFSM fsm = new MultiFSM(oexa, owsa, fsmIdExtractor, fsms, new MyExternalState(), builders, events.getValue0(), 
      events.getValue1());
    multiFSMComp = create(MultiFSMComp.class, new MultiFSMComp.Init(fsm));
    connect(multiFSMComp.getNegative(Port1.class), port1.getPair(), Channel.TWO_WAY);
    connect(multiFSMComp.getPositive(Port2.class), port2.getPair(), Channel.TWO_WAY);
  }

  private void startFSM() {
    trigger(Start.event, multiFSMComp.control());
    Identifier fsm1_v1 = idFact.randomId();
    Identifier fsm1_v2 = idFact.randomId();
    trigger(new Event1.E1(fsm1_v1), port1);
    trigger(new Event1.E1(fsm1_v2), port1);
    trigger(new Event1.E3(fsm1_v1), port1);
    trigger(new Event1.E2(fsm1_v1), port1);
  }
}

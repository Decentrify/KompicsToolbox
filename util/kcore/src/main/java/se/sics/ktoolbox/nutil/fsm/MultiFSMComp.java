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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Start;
import se.sics.ktoolbox.nutil.fsm.ids.FSMDefId;
import se.sics.ktoolbox.nutil.fsm.ids.FSMId;
import se.sics.ktoolbox.nutil.fsm.ids.FSMIds;
import se.sics.ktoolbox.nutil.genericsetup.GenericSetup;
import se.sics.ktoolbox.nutil.genericsetup.OnEventAction;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class MultiFSMComp extends ComponentDefinition {

  private static final Logger LOG = LoggerFactory.getLogger(MultiFSMComp.class);
  private String logPrefix = "";

  private final Map<FSMDefId, FSMachineDef> fsmds;
  private final Map<FSMId, FSMachine> fsms = new HashMap<>();
  private final FSMExternalState es;
  private final FSMInternalStateBuilders isb;

  private final OnEventAction oea = new OnEventAction<FSMEvent>() {
    @Override
    public void handle(FSMEvent event) {
      FSMDefId fsmdId = FSMIds.getDefId(event.getFSMName());
      FSMId fsmId = fsmdId.getFSMId(event.getBaseId());
      FSMachine fsm = fsms.get(fsmId);
      if (fsm == null) {
        FSMachineDef fsmd = fsmds.get(fsmdId);
        if (fsmd == null) {
          throw new RuntimeException("illdefined fsm - critical logical error");
        }
        try {
          fsm = fsmd.build(event.getBaseId(), es, isb.newInternalState(fsmdId));
          fsms.put(fsmId, fsm);
        } catch (FSMException ex) {
          throw new RuntimeException(ex);
        }
      }
      try {
        if (!fsm.handle(event)) {
          LOG.warn("{}fsm:{} did not hanlde event:{}", new Object[]{logPrefix, fsmId, event});
        }
      } catch (FSMException ex) {
        throw new RuntimeException(ex);
      }
    }
  };

  public MultiFSMComp(Init init) {
    this.fsmds = init.fsmds;
    this.es = init.es;
    this.es.setProxy(proxy);
    this.isb = init.isb;

    subscribe(handleStart, control);
    setupPortsAndHandlers(init.positivePorts, init.negativePorts);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      LOG.info("{}starting...", logPrefix);
    }
  };
  
  @Override
  public void tearDown() {
    //TODO Alex - maybe introduce a CLEANUP message to properly clean things before tear down;
    LOG.warn("{}multi fsm tear down is iffy at best atm - might not clean properly external sources");
    FSMStop stop = new FSMStop();
    for(FSMachine fsm : fsms.values()) {
      try {
        fsm.handle(stop);
      } catch (FSMException ex) {
        continue;
      }
    }
  }

  /**
   * Class1 - ? extends PortType , Class2 - ? extends FSMEvent(KompicsEvent)
   * <p>
   * @param posPorts
   * @param negPorts
   */
  private void setupPortsAndHandlers(List<Pair<Class, List<Class>>> posPorts, List<Pair<Class, List<Class>>> negPorts) {

    List<Pair<Class, List<Pair<OnEventAction, Class>>>> positivePorts = new LinkedList<>();
    List<Pair<Class, List<Pair<OnEventAction, Class>>>> negativePorts = new LinkedList<>();

    for (Pair<Class, List<Class>> e : posPorts) {
      List<Pair<OnEventAction, Class>> events = new LinkedList<>();
      for (Class c : e.getValue1()) {
        events.add(Pair.with(oea, c));
      }
      positivePorts.add(Pair.with(e.getValue0(), events));
    }
    for (Pair<Class, List<Class>> e : negPorts) {
      List<Pair<OnEventAction, Class>> events = new LinkedList<>();
      for (Class c : e.getValue1()) {
        events.add(Pair.with(oea, c));
      }
      negativePorts.add(Pair.with(e.getValue0(), events));
    }
    GenericSetup.portsAndHandledEvents(proxy, positivePorts, negativePorts);
  }

  public static class Init extends se.sics.kompics.Init<MultiFSMComp> {

    public final Map<FSMDefId, FSMachineDef> fsmds;
    public final List<Pair<Class, List<Class>>> positivePorts;
    public final List<Pair<Class, List<Class>>> negativePorts;
    public final FSMExternalState es;
    public final FSMInternalStateBuilders isb;

    public Init(Map<FSMDefId, FSMachineDef> fsmds, List<Pair<Class, List<Class>>> positivePorts,
      List<Pair<Class, List<Class>>> negativePorts, FSMExternalState es, FSMInternalStateBuilders isb) {
      this.fsmds = fsmds;
      this.positivePorts = positivePorts;
      this.negativePorts = negativePorts;
      this.es = es;
      this.isb = isb;
    }
  }
}

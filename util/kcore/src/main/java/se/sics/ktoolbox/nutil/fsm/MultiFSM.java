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
import se.sics.kompics.ComponentProxy;
import se.sics.ktoolbox.nutil.fsm.genericsetup.GenericSetup;
import se.sics.ktoolbox.nutil.fsm.genericsetup.OnEventAction;
import se.sics.ktoolbox.nutil.fsm.genericsetup.OnFSMExceptionAction;
import se.sics.ktoolbox.nutil.fsm.ids.FSMDefId;
import se.sics.ktoolbox.nutil.fsm.ids.FSMId;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class MultiFSM {

  private static final Logger LOG = LoggerFactory.getLogger(MultiFSMComp.class);
  private String logPrefix = "";

  private final OnFSMExceptionAction oexa;
  private final FSMIdExtractor fsmIdExtractor;
  private final Map<FSMDefId, FSMachineDef> fsmds;
  private final Map<FSMId, FSMachine> fsms = new HashMap<>();
  private final FSMExternalState es;
  private final FSMInternalStateBuilders isb;
  private final List<Pair<Class, List<Class>>> positivePorts;
  private final List<Pair<Class, List<Class>>> negativePorts;

  private final FSMOnKillAction oka = new FSMOnKillAction() {
    @Override
    public void kill(FSMId fsmId) {
      fsms.remove(fsmId);
    }
  };

  private final OnEventAction oea = new OnEventAction<FSMEvent>() {
    @Override
    public void handle(FSMEvent event) {
      try {
        Optional<FSMId> optFsmId = fsmIdExtractor.fromEvent(event);
        if (!optFsmId.isPresent()) {
          LOG.warn("{}fsm did not handle event:{}", new Object[]{logPrefix, event});
          return;
        }
        FSMId fsmId = optFsmId.get();
        FSMachine fsm = fsms.get(fsmId);
        if (fsm == null) {
          FSMachineDef fsmd = fsmds.get(fsmId.getDefId());
          if (fsmd == null) {
            throw new RuntimeException("illdefined fsm - critical logical error");
          }
          fsm = fsmd.build(event.getBaseId(), oka, es, isb.newInternalState(fsmId));
          fsms.put(fsmId, fsm);
        }
        if (!fsm.handle(event)) {
          LOG.warn("{}fsm:{} did not handle event:{}", new Object[]{logPrefix, fsmId, event});
        }
      } catch (FSMException ex) {
        oexa.handle(ex);
      }
    }
  };

  // Class1 - ? extends PortType , Class2 - ? extends FSMEvent(KompicsEvent)
  public MultiFSM(OnFSMExceptionAction oexa, FSMIdExtractor fsmIdExtractor, Map<FSMDefId, FSMachineDef> fsmds,
    FSMExternalState es, FSMInternalStateBuilders isb, List<Pair<Class, List<Class>>> positivePorts,
    List<Pair<Class, List<Class>>> negativePorts) {
    this.oexa = oexa;
    this.fsmIdExtractor = fsmIdExtractor;
    this.fsmds = fsmds;
    this.es = es;
    this.isb = isb;
    this.positivePorts = positivePorts;
    this.negativePorts = negativePorts;
  }

  public void setProxy(ComponentProxy proxy) {
    this.es.setProxy(proxy);
  }

  public void setupPortsAndHandlers() {
    Pair<List, List> ports = preparePorts();
    GenericSetup.portsAndHandledEvents(es.getProxy(), ports.getValue0(), ports.getValue1());
  }

  public void setupHandlers() {
    Pair<List, List> ports = preparePorts();
    GenericSetup.handledEvents(es.getProxy(), ports.getValue0(), ports.getValue1());
  }

  private Pair<List, List> preparePorts() {
    List pPorts = new LinkedList<>();
    List nPorts = new LinkedList<>();

    for (Pair<Class, List<Class>> e : positivePorts) {
      List<Pair<OnEventAction, Class>> events = new LinkedList<>();
      for (Class c : e.getValue1()) {
        events.add(Pair.with(oea, c));
      }
      pPorts.add(Pair.with(e.getValue0(), events));
    }
    for (Pair<Class, List<Class>> e : negativePorts) {
      List<Pair<OnEventAction, Class>> events = new LinkedList<>();
      for (Class c : e.getValue1()) {
        events.add(Pair.with(oea, c));
      }
      nPorts.add(Pair.with(e.getValue0(), events));
    }
    return Pair.with(pPorts, nPorts);
  }

  public void tearDown() {
    FSMStop stop = new FSMStop();
    for (FSMachine fsm : fsms.values()) {
      try {
        fsm.handle(stop);
      } catch (FSMException ex) {
        continue;
      }
    }
  }
}

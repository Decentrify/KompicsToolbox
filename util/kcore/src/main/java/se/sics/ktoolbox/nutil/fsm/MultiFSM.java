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
import java.util.Set;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentProxy;
import se.sics.ktoolbox.nutil.fsm.api.FSMEvent;
import se.sics.ktoolbox.nutil.fsm.api.FSMException;
import se.sics.ktoolbox.nutil.fsm.api.FSMExternalState;
import se.sics.ktoolbox.nutil.fsm.api.FSMIdExtractor;
import se.sics.ktoolbox.nutil.fsm.api.FSMInternalStateBuilders;
import se.sics.ktoolbox.nutil.fsm.api.FSMOnKillAction;
import se.sics.ktoolbox.nutil.fsm.api.FSMStateName;
import se.sics.ktoolbox.nutil.fsm.genericsetup.GenericSetup;
import se.sics.ktoolbox.nutil.fsm.genericsetup.OnEventAction;
import se.sics.ktoolbox.nutil.fsm.genericsetup.OnFSMExceptionAction;
import se.sics.ktoolbox.nutil.fsm.genericsetup.OnMsgAction;
import se.sics.ktoolbox.nutil.fsm.ids.FSMDefId;
import se.sics.ktoolbox.nutil.fsm.ids.FSMId;
import se.sics.ktoolbox.nutil.fsm.ids.FSMIds;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class MultiFSM {

  private static final Logger LOG = LoggerFactory.getLogger(MultiFSM.class);
  private String logPrefix = "";

  private final FSMachineDef fsmDef;
  private final OnFSMExceptionAction oexa;
  private final FSMIdExtractor fsmIdExtractor;
  private final Map<FSMId, FSMachine> fsms = new HashMap<>();
  private final FSMExternalState es;
  private final FSMInternalStateBuilders isb;
  private final Map<Class, Set<Class>> positivePorts;
  private final Map<Class, Set<Class>> negativePorts;
  private final Set<Class> positiveNetworkMsgs;
  private final Set<Class> negativeNetworkMsgs;

  private final FSMOnKillAction oka = new FSMOnKillAction() {
    @Override
    public void kill(FSMId fsmId) {
      LOG.info("{}removing fsm:{}", logPrefix, fsmId);
      fsms.remove(fsmId);
    }
  };

  private Optional<FSMachine> getFSM(FSMEvent event) throws FSMException {
    Optional<FSMId> optFsmId = fsmIdExtractor.fromEvent(fsmDef.id, event);
    if (!optFsmId.isPresent()) {
      LOG.warn("{}fsm did not handle event:{}", new Object[]{logPrefix, event});
      return Optional.absent();
    }
    FSMId fsmId = optFsmId.get();
    FSMachine fsm = fsms.get(fsmId);
    if (fsm == null) {
      fsm = fsmDef.build(fsmId.baseId, oka, es, isb.newInternalState(fsmId));
      fsms.put(fsmId, fsm);
    }
    return Optional.of(fsm);
  }
  private final OnEventAction oeapos = new OnEventAction<FSMEvent>() {
    @Override
    public void handle(FSMEvent event) {
      try {
        Optional<FSMachine> fsm = getFSM(event);
        if (fsm.isPresent()) {
          fsm.get().handlePositive(event);
        }
      } catch (FSMException ex) {
        oexa.handle(ex);
      }
    }
  };

  private final OnEventAction oeaneg = new OnEventAction<FSMEvent>() {
    @Override
    public void handle(FSMEvent event) {
      try {
        Optional<FSMachine> fsm = getFSM(event);
        if (fsm.isPresent()) {
          fsm.get().handleNegative(event);
        }
      } catch (FSMException ex) {
        oexa.handle(ex);
      }
    }
  };

  private final OnMsgAction omapos = new OnMsgAction<FSMEvent>() {
    @Override
    public void handle(FSMEvent payload, KContentMsg<KAddress, KHeader<KAddress>, FSMEvent> msg) {
      try {
        Optional<FSMachine> fsm = getFSM(payload);
        if (fsm.isPresent()) {
          fsm.get().handlePositive(payload, msg);
        }
      } catch (FSMException ex) {
        oexa.handle(ex);
      }
    }
  };

  private final OnMsgAction omaneg = new OnMsgAction<FSMEvent>() {
    @Override
    public void handle(FSMEvent payload, KContentMsg<KAddress, KHeader<KAddress>, FSMEvent> msg) {
      try {
        Optional<FSMachine> fsm = getFSM(payload);
        if (fsm.isPresent()) {
          fsm.get().handleNegative(payload, msg);
        }
      } catch (FSMException ex) {
        oexa.handle(ex);
      }
    }
  };

  // Class1 - ? extends PortType , Class2 - ? extends FSMEvent(KompicsEvent)
  public MultiFSM(FSMachineDef fsmDef, OnFSMExceptionAction oexa, FSMIdExtractor fsmIdExtractor,
    FSMExternalState es, FSMInternalStateBuilders isb,
    Map<Class, Set<Class>> positivePorts, Map<Class, Set<Class>> negativePorts,
    Set<Class> positiveNetworkMsgs, Set<Class> negativeNetworkMsgs) {
    this.fsmDef = fsmDef;
    this.oexa = oexa;
    this.fsmIdExtractor = fsmIdExtractor;
    this.es = es;
    this.isb = isb;
    this.positivePorts = positivePorts;
    this.negativePorts = negativePorts;
    this.positiveNetworkMsgs = positiveNetworkMsgs;
    this.negativeNetworkMsgs = negativeNetworkMsgs;
  }

  public void setProxy(ComponentProxy proxy) {
    this.es.setProxy(proxy);
  }

  public void setupPortsAndHandlers() {
    Pair<List, List> ports = preparePorts();
    Pair<List, List> networkPorts = prepareNetwork();
    GenericSetup.portsAndHandledEvents(es.getProxy(), ports.getValue0(), ports.getValue1(),
      networkPorts.getValue0(), networkPorts.getValue1());
  }

  public void setupHandlers() {
    Pair<List, List> ports = preparePorts();
    Pair<List, List> networkPorts = prepareNetwork();
    GenericSetup.handledEvents(es.getProxy(), ports.getValue0(), ports.getValue1(),
      networkPorts.getValue0(), networkPorts.getValue1());
  }

  private Pair<List, List> preparePorts() {
    List pPorts = new LinkedList<>();
    List nPorts = new LinkedList<>();

    for (Map.Entry<Class, Set<Class>> e : positivePorts.entrySet()) {
      List<Pair<OnEventAction, Class>> events = new LinkedList<>();
      for (Class c : e.getValue()) {
        events.add(Pair.with(oeapos, c));
      }
      pPorts.add(Pair.with(e.getKey(), events));
    }
    for (Map.Entry<Class, Set<Class>> e : negativePorts.entrySet()) {
      List<Pair<OnEventAction, Class>> events = new LinkedList<>();
      for (Class c : e.getValue()) {
        events.add(Pair.with(oeaneg, c));
      }
      nPorts.add(Pair.with(e.getKey(), events));
    }
    return Pair.with(pPorts, nPorts);
  }

  private Pair<List, List> prepareNetwork() {
    List pPorts = new LinkedList<>();
    List nPorts = new LinkedList<>();
    for (Class c : positiveNetworkMsgs) {
      pPorts.add(Pair.with(omapos, c));
    }
    for (Class c : negativeNetworkMsgs) {
      nPorts.add(Pair.with(omaneg, c));
    }
    return Pair.with(pPorts, nPorts);
  }
  
  public FSMStateName getFSMState(String fsmName, Identifier baseId) {
    FSMDefId fsmDefId = FSMIds.getDefId(fsmName);
    FSMId fsmId = fsmDefId.getFSMId(baseId);
    FSMachine fsm = fsms.get(fsmId);
    return fsm.getState();
  }
  
  public boolean isEmpty() {
    return fsms.isEmpty();
  }
}

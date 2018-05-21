///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * KompicsToolbox is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package se.sics.ktoolbox.util.mngrcomp;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//import org.javatuples.Pair;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import se.sics.kompics.Component;
//import se.sics.kompics.ComponentDefinition;
//import se.sics.kompics.Fault;
//import se.sics.kompics.Handler;
//import se.sics.kompics.Negative;
//import se.sics.kompics.Start;
//import se.sics.ktoolbox.util.mngrcomp.util.CompSetup;
//import se.sics.ktoolbox.util.mngrcomp.util.PortsSetup;
//
///**
// * @author Alex Ormenisan <aaor@kth.se>
// */
//public class MngrComp extends ComponentDefinition {
//
//  private static final Logger LOG = LoggerFactory.getLogger(MngrComp.class);
//  private String logPrefix = "";
//
//  private final Negative<MngrPort> ctrlPort = provides(MngrPort.class);
//  private final PortsSetup ports;
//  private final Map<Integer, CompSetup> compSetup;
//  private final Map<UUID, Pair<Integer, Component>> comps = new HashMap<>();
//
//  public MngrComp(Init init) {
//
//    ports = init.ports;
//    ports.setup(proxy);
//    compSetup = init.compSetup;
//
//    subscribe(handleStart, control);
//    subscribe(handleCreate, ctrlPort);
//    subscribe(handleKill, ctrlPort);
//  }
//
//  Handler handleStart = new Handler<Start>() {
//
//    @Override
//    public void handle(Start event) {
//      LOG.info("{}starting...", logPrefix);
//    }
//  };
//
//  @Override
//  public void tearDown() {
//    for (Map.Entry<UUID, Pair<Integer, Component>> e : comps.entrySet()) {
//      killComp(e.getKey(), e.getValue().getValue0(), e.getValue().getValue1());
//      trigger(new MngrEvents.Killed(e.getKey()), ctrlPort);
//    }
//    comps.clear();
//  }
//
//  @Override
//  public Fault.ResolveAction handleFault(Fault fault) {
//    UUID compId = fault.getSourceCore().id();
//    Pair<Integer, Component> compAux = comps.remove(compId);
//    if(compAux != null) {
//      killComp(compId, compAux.getValue0(), compAux.getValue1());
//      trigger(new MngrEvents.Killed(compId), ctrlPort);
//    }
//    return Fault.ResolveAction.RESOLVED;
//  }
//
//  Handler handleCreate = new Handler<MngrEvents.Create>() {
//
//    @Override
//    public void handle(MngrEvents.Create req) {
//      CompSetup compBuilder = compSetup.get(req.compType);
//      if (compBuilder == null) {
//        throw new RuntimeException("logic exception");
//      }
//      Component comp = compBuilder.createAndStart(proxy, ports, req.appCompId, req.init);
//      comps.put(comp.id(), Pair.with(req.compType, comp));
//      LOG.info("{}created comp:{} of type:{}", new Object[]{logPrefix, comp.id(), req.compType});
//      trigger(req.success(comp.id()), ctrlPort);
//    }
//  };
//
//  Handler handleKill = new Handler<MngrEvents.Kill>() {
//
//    @Override
//    public void handle(MngrEvents.Kill req) {
//      Pair<Integer, Component> compAux = comps.remove(req.compId);
//      if (compAux != null) {
//        killComp(req.compId, compAux.getValue0(), compAux.getValue1());
//      }
//      trigger(req.ack(), ctrlPort);
//    }
//  };
//
//  private void killComp(UUID compId, Integer compType, Component comp) {
//    CompSetup compDef = compSetup.get(compType);
//    if (compDef == null) {
//      throw new RuntimeException("logic exception");
//    }
//    compDef.destroyAndKill(proxy, ports, comp);
//    LOG.info("{}killed comp:{} of type:{}", new Object[]{logPrefix, compId, compType});
//  }
//
//  public static class Init extends se.sics.kompics.Init<MngrComp> {
//
//    public final PortsSetup ports;
//    public final Map<Integer, CompSetup> compSetup;
//
//    public Init(PortsSetup ports, Map<Integer, CompSetup> compSetup) {
//      this.ports = ports;
//      this.compSetup = compSetup;
//    }
//  }
//}

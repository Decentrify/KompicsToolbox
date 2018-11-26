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
package se.sics.ktoolbox.nutil.nxcomp;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.javatuples.Pair;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.util.identifiable.BasicBuilders;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LauncherComp extends ComponentDefinition {

  private final Positive<NxMngrPort> mngrPort = requires(NxMngrPort.class);
  private final Positive<DriverPort> driverPort = requires(DriverPort.class);
  Identifier comp1;
  Identifier comp2;
  IdentifierFactory eventIds;
  int created = 0;
  Component nxMngr;

  public LauncherComp() {
    IdentifierFactory compIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.NODE, Optional.empty());
    eventIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.EVENT, Optional.empty());
    comp1 = compIds.id(new BasicBuilders.IntBuilder(0));
    comp2 = compIds.id(new BasicBuilders.IntBuilder(1));
    subscribe(handleStart, control);
    subscribe(handleCreated, mngrPort);
    subscribe(handleKilled, mngrPort);
    subscribe(handleDone, driverPort);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      createNxMngr();
      trigger(new NxMngrEvents.CreateReq<>(eventIds.randomId(), comp1, new TestComp.Init()), mngrPort);
      trigger(new NxMngrEvents.CreateReq<>(eventIds.randomId(), comp2, new TestComp.Init()), mngrPort);
    }
  };
  
  Handler handleCreated = new Handler<NxMngrEvents.CreateAck>() {
    @Override
    public void handle(NxMngrEvents.CreateAck event) {
      logger.info("created:", event.req.compId);
      created++;
      if(created == 2) {
        createDriver();
      }
    }
  };
  
  Handler handleDone = new Handler<DriverEvents.Done>() {
    @Override
    public void handle(DriverEvents.Done event) {
      trigger(new NxMngrEvents.KillReq(eventIds.randomId(), comp1), mngrPort);
      trigger(new NxMngrEvents.KillReq(eventIds.randomId(), comp2), mngrPort);
    }
  };
  
  Handler handleKilled = new Handler<NxMngrEvents.KillAck>() {
    @Override
    public void handle(NxMngrEvents.KillAck event) {
      logger.info("killed:", event.req.compId);
    }
  };

  private void createDriver() {
    Component driver = create(DriverComp.class, new DriverComp.Init(comp1, comp2));
    connect(driver.getNegative(PortA.class), nxMngr.getPositive(PortA.class), Channel.TWO_WAY);
    connect(driver.getPositive(PortB.class), nxMngr.getNegative(PortB.class), Channel.TWO_WAY);
    trigger(Start.event, driver.control());
  }

  private void createNxMngr() {
    List<Pair<Class<PortType>, NxMngrComp.NxChannelIdExtractor>> negativePorts = new LinkedList<>();
    negativePorts.add(Pair.with((Class) PortA.class, new TestChannelIdExtractor(TestEvent.class)));
    List<Pair<Class<PortType>, NxMngrComp.NxChannelIdExtractor>> positivePorts = new LinkedList<>();
    positivePorts.add(Pair.with((Class) PortB.class, new TestChannelIdExtractor(TestEvent.class)));
    nxMngr = create(NxMngrComp.class, new NxMngrComp.Init<>(TestComp.class, negativePorts, positivePorts));
    connect(nxMngr.getPositive(NxMngrPort.class), mngrPort.getPair(), Channel.TWO_WAY);
    trigger(Start.event, nxMngr.control());
  }
}

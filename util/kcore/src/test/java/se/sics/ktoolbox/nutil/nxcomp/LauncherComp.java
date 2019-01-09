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
import se.sics.ktoolbox.util.network.ports.ChannelIdExtractor;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LauncherComp extends ComponentDefinition {

  private final Positive<NxMngrPort> mngrPort = requires(NxMngrPort.class);
  private final Positive<DriverPort> driverPort = requires(DriverPort.class);
  Identifier stackId1;
  Identifier stackId2;
  IdentifierFactory eventIds;
  int created = 0;
  Component nxMngr;
  NxMngrEvents.CreateReq req1;
  NxMngrEvents.CreateReq req2;

  public LauncherComp() {
    IdentifierFactory compIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.NODE, Optional.empty());
    eventIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.EVENT, Optional.empty());
    stackId1 = compIds.id(new BasicBuilders.IntBuilder(0));
    stackId2 = compIds.id(new BasicBuilders.IntBuilder(1));
    subscribe(handleStart, control);
    subscribe(handleCreated, mngrPort);
    subscribe(handleKilled, mngrPort);
    subscribe(handleDone, driverPort);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      createNxMngr();
      NxStackInit stackInit1 = new NxStackInit.OneComp<>(new TestComp.Init());
      req1 = new NxMngrEvents.CreateReq(eventIds.randomId(), stackId1, stackInit1);
      trigger(req1, mngrPort);
      NxStackInit stackInit2 = new NxStackInit.OneComp<>(new TestComp.Init());
      req2 = new NxMngrEvents.CreateReq(eventIds.randomId(), stackId2, stackInit2);
      trigger(req2, mngrPort);
    }
  };

  Handler handleCreated = new Handler<NxMngrEvents.CreateAck>() {
    @Override
    public void handle(NxMngrEvents.CreateAck event) {
      logger.info("created:", event.req.stackId);
      created++;
      if (created == 2) {
        createDriver();
      }
    }
  };

  Handler handleDone = new Handler<DriverEvents.Done>() {
    @Override
    public void handle(DriverEvents.Done event) {
      trigger(new NxMngrEvents.KillReq(req1), mngrPort);
      trigger(new NxMngrEvents.KillReq(req2), mngrPort);
    }
  };

  Handler handleKilled = new Handler<NxMngrEvents.KillAck>() {
    @Override
    public void handle(NxMngrEvents.KillAck event) {
      logger.info("killed:", event.req.stackId());
    }
  };

  private void createDriver() {
    Component driver = create(DriverComp.class, new DriverComp.Init(stackId1, stackId2));
    connect(driver.getNegative(PortA.class), nxMngr.getPositive(PortA.class), Channel.TWO_WAY);
    connect(driver.getPositive(PortB.class), nxMngr.getNegative(PortB.class), Channel.TWO_WAY);
    trigger(Start.event, driver.control());
  }

  private void createNxMngr() {
    List<Class<PortType>> negativePorts = new LinkedList<>();
    List<ChannelIdExtractor> negativeIdExtractors = new LinkedList<>();
    List<Class<PortType>> positivePorts = new LinkedList<>();
    List<ChannelIdExtractor> positiveIdExtractors = new LinkedList<>();
    negativePorts.add((Class) PortA.class);
    negativeIdExtractors.add(new TestChannelIdExtractor(TestEvent.class));
    positivePorts.add((Class) PortB.class);
    positiveIdExtractors.add(new TestChannelIdExtractor(TestEvent.class));

    NxStackDefinition stackDefintion = new NxStackDefinition.OneComp<>(TestComp.class);
    nxMngr = create(NxMngrComp.class, new NxMngrComp.Init("nxmngr", stackDefintion, negativePorts, negativeIdExtractors, 
      positivePorts, positiveIdExtractors));
    connect(nxMngr.getPositive(NxMngrPort.class), mngrPort.getPair(), Channel.TWO_WAY);
    trigger(Start.event, nxMngr.control());
  }
}

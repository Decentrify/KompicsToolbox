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
package se.sics.ktoolbox.overlaymngr.bootstrap.setup;

import java.util.Optional;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.timer.TimerProxy;
import se.sics.ktoolbox.nutil.timer.TimerProxyImpl;
import se.sics.ktoolbox.omngr.bootstrap.BootstrapClientEvent;
import se.sics.ktoolbox.omngr.bootstrap.BootstrapClientPort;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class DriverComp extends ComponentDefinition {

  Positive<Timer> timerPort = requires(Timer.class);
  Positive<BootstrapClientPort> clientPort = requires(BootstrapClientPort.class);
  private final Init init;
  private final TimerProxy timer;
  private final IdentifierFactory eventIds;
  
  private int counter1 = 10;
  private int counter2 = 10;

  public DriverComp(Init init) {
    this.init = init;

    timer = new TimerProxyImpl().setup(proxy, logger);
    eventIds = IdentifierRegistryV2.instance(BasicIdentifiers.Values.EVENT, Optional.of(1234l));
    subscribe(handleStart, control);
    subscribe(handleBootstrapSample, clientPort);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      timer.scheduleTimer(1000, (_ignore) -> test());
    }
  };
  
  Handler handleBootstrapSample = new Handler<BootstrapClientEvent.Sample>() {
    @Override
    public void handle(BootstrapClientEvent.Sample event) {
      logger.info("{} sample:{}", event.req.overlay, event.sample);
      if(event.req.overlay.equals(init.overlayId1)) {
        counter1--;
      } else if(event.req.overlay.equals(init.overlayId2)) {
        counter2--;
      }
      if(counter1 == 0) {
        counter1--;
        trigger(new BootstrapClientEvent.Stop(eventIds.randomId(), init.overlayId1), clientPort);
      } else if(counter2 == 0) {
        counter2--;
        trigger(new BootstrapClientEvent.Stop(eventIds.randomId(), init.overlayId2), clientPort);
      }
    }
  };

  private void test() {
    trigger(new BootstrapClientEvent.Start(eventIds.randomId(), init.overlayId1), clientPort);
    trigger(new BootstrapClientEvent.Start(eventIds.randomId(), init.overlayId2), clientPort);
  }

  public static class Init extends se.sics.kompics.Init<DriverComp> {

    public final Identifier overlayId1;
    public final Identifier overlayId2;

    public Init(Identifier overlayId1, Identifier overlayId2) {
      this.overlayId1 = overlayId1;
      this.overlayId2 = overlayId2;
    }
  }
}

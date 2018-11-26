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

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.util.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class DriverComp extends ComponentDefinition {

  private final Positive<PortA> portA = requires(PortA.class);
  private final Negative<PortB> portB = provides(PortB.class);
  private final Negative<DriverPort> driverPort = provides(DriverPort.class);

  private final Init init;
  int counter = 0;

  public DriverComp(Init init) {
    this.init = init;
    subscribe(handleStart, control);
    subscribe(handleA, portA);
    subscribe(handleB, portB);
  }

  Handler handleStart = new Handler<Start>() {
    @Override
    public void handle(Start event) {
      trigger(new PortAEvents.Event1(init.comp1), portA);
      trigger(new PortAEvents.Event1(init.comp2), portA);
    }
  };

  Handler handleA = new Handler<PortAEvents.Event2>() {
    @Override
    public void handle(PortAEvents.Event2 event) {
      logger.info("a:{}", event.compId);
      if (event.compId.equals(init.comp1)) {
        trigger(new PortBEvents.Event1(init.comp1), portB);
      } else {
        trigger(new PortBEvents.Event1(init.comp2), portB);
      }
    }

  };

  Handler handleB = new Handler<PortBEvents.Event2>() {
    @Override
    public void handle(PortBEvents.Event2 event) {
      logger.info("b:{}", event.compId);
      counter++;
      if (counter == 2) {
        trigger(new DriverEvents.Done(), driverPort);
      }
    }
  };

  public static class Init extends se.sics.kompics.Init<DriverComp> {

    public final Identifier comp1;
    public final Identifier comp2;

    public Init(Identifier comp1, Identifier comp2) {
      this.comp1 = comp1;
      this.comp2 = comp2;
    }
  }
}

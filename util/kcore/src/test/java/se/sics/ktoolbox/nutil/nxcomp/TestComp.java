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

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TestComp extends ComponentDefinition {
  private final Negative<PortA> portA = provides(PortA.class);
  private final Positive<PortB> portB = requires(PortB.class);
  
  public TestComp(Init init) {
    subscribe(handleA, portA);
    subscribe(handleB, portB);
  }
  
  Handler handleA = new Handler<PortAEvents.Event1>() {
    @Override
    public void handle(PortAEvents.Event1 event) {
      trigger(event.event2(), portA);
    }
  };
  
  Handler handleB = new Handler<PortBEvents.Event1>() {
    @Override
    public void handle(PortBEvents.Event1 event) {
      trigger(event.event2(), portB);
    }
  };
  
  public static class Init extends se.sics.kompics.Init<TestComp> {
  }
}

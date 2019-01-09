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
package se.sics.ktoolbox.nutil.nxcomp.v2;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import se.sics.kompics.Component;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public abstract class NxStackInstanceV2 {

  public final List<Component> components;
  public final List<Positive> positivePorts;
  public final List<Negative> negativePorts;
  public final Optional<Negative> negativeNetwork;

  protected NxStackInstanceV2(List<Component> components,
    List<Positive> positivePorts, List<Negative> negativePorts,
    Optional<Negative> negativeNetwork) {
    this.components = components;
    this.positivePorts = positivePorts;
    this.negativePorts = negativePorts;
    this.negativeNetwork = negativeNetwork;
  }

  public abstract void disconnect();

  public static class OneComp extends NxStackInstanceV2 {

    public OneComp(List<Component> components, 
      List<Positive> positivePorts, List<Negative> negativePorts,
      Optional<Negative> negativeNetwork) {
      super(components, positivePorts, negativePorts, negativeNetwork);
    }

    @Override
    public void disconnect() {
      //nothing to do
    }
    
    public static OneComp instance(Component component, List<Positive> positivePorts, List<Negative> negativePorts) {
      List<Component> components = new LinkedList<>();
      components.add(component);
      return new OneComp(components, positivePorts, negativePorts, Optional.empty());
    }
    
    public static OneComp instance(Component component, List<Positive> positivePorts, List<Negative> negativePorts, 
      Negative negativeNetwork) {
      List<Component> components = new LinkedList<>();
      components.add(component);
      return new OneComp(components, positivePorts, negativePorts, Optional.of(negativeNetwork));
    }
  }
}

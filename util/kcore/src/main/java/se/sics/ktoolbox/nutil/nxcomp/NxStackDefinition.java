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
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;
import se.sics.kompics.util.Identifier;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public interface NxStackDefinition {
  public NxStackInstance setup(ComponentProxy proxy, Identifier stackId, NxStackInit init,
    List<Class<PortType>> negativePorts, List<Class<PortType>> positivePorts);
  
  public static class OneComp<D extends ComponentDefinition> implements NxStackDefinition {
    public final Class<D> compDefinition;
    
    public OneComp(Class<D> compDefinition) {
      this.compDefinition = compDefinition;
    }

    @Override
    public NxStackInstance setup(ComponentProxy proxy, Identifier stackId, NxStackInit stackInit, 
      List<Class<PortType>> negativePorts, List<Class<PortType>> positivePorts) {
      if(!(stackInit instanceof NxStackInit.OneComp)) {
        throw new RuntimeException("bad logic");
      }
      Init compInit = ((NxStackInit.OneComp)stackInit).init;
      Component comp = proxy.create(compDefinition, compInit);
      List<Component> components = new LinkedList<>();
      components.add(comp);
      
      //remember positive from inside the component - negative from outside the component
      List<Positive> positive = new LinkedList<>();
      negativePorts.forEach((portType) -> positive.add(comp.getPositive(portType)));
      
      //remember negative from inside the component - positive from outside the component
      List<Negative> negative = new LinkedList<>();
      positivePorts.forEach((portType) -> negative.add(comp.getNegative(portType)));
      return new NxStackInstance.OneComp(components, positive, negative);
    }
  }
}

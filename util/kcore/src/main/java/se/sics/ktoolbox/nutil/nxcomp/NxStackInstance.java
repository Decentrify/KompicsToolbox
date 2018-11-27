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

import java.util.List;
import se.sics.kompics.Component;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public abstract class NxStackInstance {
  public final List<Component> components;
  public final List<Positive> positivePorts;
  public final List<Negative> negativePorts;
  
  protected NxStackInstance(List<Component> components, List<Positive> positivePorts, List<Negative> negativePorts) {
    this.components = components;
    this.positivePorts = positivePorts;
    this.negativePorts = negativePorts;
  }
  
  public abstract void disconnect();
  
  public static class OneComp extends NxStackInstance {

    public OneComp(List<Component> components, List<Positive> positivePorts, List<Negative> negativePorts) {
      super(components, positivePorts, negativePorts);
    }
    
    @Override
    public void disconnect() {
      //nothing to do
    }
  }
}

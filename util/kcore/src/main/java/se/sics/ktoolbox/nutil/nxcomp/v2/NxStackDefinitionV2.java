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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.network.portsv2.MsgIdExtractorV2;
import se.sics.ktoolbox.nutil.nxcomp.NxStackInit;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public interface NxStackDefinitionV2 {

  public NxStackInstanceV2 setup(ComponentProxy proxy, Identifier stackId, NxStackInit init,
    List<Class<PortType>> negativePorts, List<Class<PortType>> positivePorts, boolean positiveNetwork);

  public static class OneComp<D extends ComponentDefinition> implements NxStackDefinitionV2 {

    public final Class<D> compDefinition;

    public OneComp(Class<D> compDefinition) {
      this.compDefinition = compDefinition;
    }

    @Override
    public NxStackInstanceV2 setup(ComponentProxy proxy, Identifier stackId, NxStackInit stackInit,
      List<Class<PortType>> negativePorts, List<Class<PortType>> positivePorts, boolean negativeNetwork) {
      if (!(stackInit instanceof NxStackInit.OneComp)) {
        throw new RuntimeException("bad logic");
      }
      Init compInit = ((NxStackInit.OneComp) stackInit).init;
      Component comp = proxy.create(compDefinition, compInit);

      //remember positive from inside the component - negative from outside the component
      List<Positive> positive = new LinkedList<>();
      negativePorts.forEach((portType) -> positive.add(comp.getPositive(portType)));

      //remember negative from inside the component - positive from outside the component
      List<Negative> negative = new LinkedList<>();
      positivePorts.forEach((portType) -> negative.add(comp.getNegative(portType)));

      if (negativeNetwork) {
        Negative negNetPort = comp.getNegative(Network.class);
        return NxStackInstanceV2.OneComp.instance(comp, positive, negative, negNetPort);
      } else {
        return NxStackInstanceV2.OneComp.instance(comp, positive, negative);
      }
    }
  }
  
  public static class NetworkPort {

    public final Map<String, MsgIdExtractorV2> idSelectors;

    public NetworkPort(Map<String, MsgIdExtractorV2> idSelectors) {
      this.idSelectors = idSelectors;
    }
  }

  public static class NetworkBuilder {

    public final Map<String, MsgIdExtractorV2> idSelectors = new HashMap<>();

    public void acceptContent(String contentType, MsgIdExtractorV2 idExtractor) {
      idSelectors.put(contentType, idExtractor);
    }

    public NetworkPort build() {
      return new NetworkPort(idSelectors);
    }
  }
}

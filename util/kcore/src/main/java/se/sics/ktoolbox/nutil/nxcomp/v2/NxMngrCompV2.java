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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.PortType;
import se.sics.ktoolbox.nutil.network.portsv2.EventIdExtractorV2;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NxMngrCompV2 extends ComponentDefinition {

  private final NxMngrProxyV2 mngrProxy;

  public NxMngrCompV2(Init init) {
    this.mngrProxy = new NxMngrProxyV2(init.stackDefinition, init.negativePorts, init.negativeIdSelectors,
      init.positivePorts, init.positiveIdSelectors, init.positiveNetwork).setup(proxy, logger);
  }

  @Override
  public void tearDown() {
    mngrProxy.close();
  }

  public static class Init extends se.sics.kompics.Init<NxMngrCompV2> {

    public final NxStackDefinitionV2 stackDefinition;
    public final List<Class<PortType>> negativePorts;
    public final List<Map<String, EventIdExtractorV2>> negativeIdSelectors;
    public final List<Class<PortType>> positivePorts;
    public final List<Map<String, EventIdExtractorV2>> positiveIdSelectors;
    public final Optional<NxStackDefinitionV2.NetworkPort> positiveNetwork;

    public Init(NxStackDefinitionV2 stackDefinition,
      List<Class<PortType>> negativePorts, List<Map<String, EventIdExtractorV2>> negativeIdSelectors,
      List<Class<PortType>> positivePorts, List<Map<String, EventIdExtractorV2>> positiveIdSelectors, 
      Optional<NxStackDefinitionV2.NetworkPort> positiveNetwork) {
      this.stackDefinition = stackDefinition;
      this.negativePorts = negativePorts;
      this.negativeIdSelectors = negativeIdSelectors;
      this.positivePorts = positivePorts;
      this.positiveIdSelectors = positiveIdSelectors;
      this.positiveNetwork = positiveNetwork;
    }
  }
}

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
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.PortType;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NxMngrComp extends ComponentDefinition {

  private final NxMngrProxy mngrProxy;

  public NxMngrComp(Init init) {
    this.mngrProxy = new NxMngrProxy(init.stackDefinition, init.negativePorts, init.negativeIdExtractors,
      init.positivePorts, init.positiveIdExtractors).setup(proxy, logger);
  }

  @Override
  public void tearDown() {
    mngrProxy.close();
  }

  public static class Init extends se.sics.kompics.Init<NxMngrComp> {

    public final NxStackDefinition stackDefinition;
    public final List<Class<PortType>> negativePorts;
    public final List<NxChannelIdExtractor> negativeIdExtractors;
    public final List<Class<PortType>> positivePorts;
    public final List<NxChannelIdExtractor> positiveIdExtractors;

    public Init(NxStackDefinition stackDefinition,
      List<Class<PortType>> negativePorts, List<NxChannelIdExtractor> negativeIdExtractors,
      List<Class<PortType>> positivePorts, List<NxChannelIdExtractor> positiveIdExtractors) {
      this.stackDefinition = stackDefinition;
      this.negativePorts = negativePorts;
      this.negativeIdExtractors = negativeIdExtractors;
      this.positivePorts = positivePorts;
      this.positiveIdExtractors = positiveIdExtractors;
    }
  }
}

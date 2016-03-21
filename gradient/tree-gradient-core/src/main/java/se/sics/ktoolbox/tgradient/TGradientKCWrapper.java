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
package se.sics.ktoolbox.tgradient;

import se.sics.kompics.config.Config;
import se.sics.ktoolbox.util.aggregation.AggregationLevel;
import se.sics.ktoolbox.util.config.KConfigHelper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TGradientKCWrapper {
    public final Config configCore;
    public final int centerNodes;
    public final int branching;

    public final AggregationLevel tgradientAggLevel;
    public final long tgradientAggPeriod;
    
    public TGradientKCWrapper(Config configCore) {
        this.configCore = configCore;
        centerNodes = KConfigHelper.read(this.configCore, TGradientKConfig.centerNodes);
        branching = KConfigHelper.read(this.configCore, TGradientKConfig.branching);
        
        tgradientAggLevel = KConfigHelper.read(configCore, TGradientKConfig.aggLevel);
        tgradientAggPeriod = KConfigHelper.read(configCore, TGradientKConfig.aggPeriod);
    }
}

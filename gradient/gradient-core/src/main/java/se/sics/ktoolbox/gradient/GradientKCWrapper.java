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
package se.sics.ktoolbox.gradient;

import se.sics.kompics.config.Config;
import se.sics.ktoolbox.util.aggregation.AggregationLevel;
import se.sics.ktoolbox.util.config.KConfigHelper;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class GradientKCWrapper {

    public final Config configCore;
    public final int viewSize;
    public final int shuffleSize;
    public final long shufflePeriod;
    public final long shuffleTimeout;
    public final double softMaxTemp;
    public final int oldThreshold;
    
    public final AggregationLevel gradientAggLevel;
    public final long gradientAggPeriod;

    public GradientKCWrapper(Config configCore) {
        this.configCore = configCore;
        viewSize = KConfigHelper.read(this.configCore, GradientKConfig.viewSize);
        shuffleSize = KConfigHelper.read(this.configCore, GradientKConfig.shuffleSize);
        shufflePeriod = KConfigHelper.read(this.configCore, GradientKConfig.shufflePeriod);
        shuffleTimeout = KConfigHelper.read(this.configCore, GradientKConfig.shuffleTimeout);
        softMaxTemp = KConfigHelper.read(this.configCore, GradientKConfig.softMaxTemp);
        oldThreshold = KConfigHelper.read(this.configCore, GradientKConfig.oldThreshold);
        
        gradientAggLevel = KConfigHelper.read(configCore, GradientKConfig.aggLevel);
        gradientAggPeriod = KConfigHelper.read(configCore, GradientKConfig.aggPeriod);
    }
}

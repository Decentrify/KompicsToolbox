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

import se.sics.ktoolbox.util.aggregation.AggregationLevelOption;
import se.sics.ktoolbox.util.config.KConfigOption;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class GradientKConfig {
    public final static KConfigOption.Basic<Integer> viewSize = new KConfigOption.Basic("gradient.viewSize", Integer.class);
    public final static KConfigOption.Basic<Integer> shuffleSize = new KConfigOption.Basic("gradient.shuffleSize", Integer.class);
    public final static KConfigOption.Basic<Long> shufflePeriod = new KConfigOption.Basic("gradient.shufflePeriod", Long.class);
    public final static KConfigOption.Basic<Long> shuffleTimeout = new KConfigOption.Basic("gradient.shuffleTimeout", Long.class);
    public final static KConfigOption.Basic<Double> softMaxTemp = new KConfigOption.Basic("gradient.softMaxTemperature", Double.class);
    public final static KConfigOption.Basic<Integer> oldThreshold = new KConfigOption.Basic("gradient.oldThreshold", Integer.class);

    public final static AggregationLevelOption aggLevel = new AggregationLevelOption("gradient.aggLevel");
    public final static KConfigOption.Basic<Long> aggPeriod = new KConfigOption.Basic("gradient.aggPeriod", Long.class);
}

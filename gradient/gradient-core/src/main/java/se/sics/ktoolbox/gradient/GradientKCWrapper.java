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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.util.config.KConfigCore;
import se.sics.ktoolbox.util.config.KConfigHelper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class GradientKCWrapper {

    public final KConfigCore configCore;
    public final int viewSize;
    public final int shuffleSize;
    public final long shufflePeriod;
    public final long shuffleTimeout;
    public final double softMaxTemp;
    public final int oldThreshold;
    
    public final long seed;
    public final int overlayId;

    public GradientKCWrapper(KConfigCore configCore, long seed, int overlayId) {
        this.configCore = configCore;
        this.viewSize = KConfigHelper.read(this.configCore, GradientKConfig.viewSize);
        this.shuffleSize = KConfigHelper.read(this.configCore, GradientKConfig.shuffleSize);
        this.shufflePeriod = KConfigHelper.read(this.configCore, GradientKConfig.shufflePeriod);
        this.shuffleTimeout = KConfigHelper.read(this.configCore, GradientKConfig.shuffleTimeout);
        this.softMaxTemp = KConfigHelper.read(this.configCore, GradientKConfig.softMaxTemp);
        this.oldThreshold = KConfigHelper.read(this.configCore, GradientKConfig.oldThreshold);
        this.seed = seed;
        this.overlayId = overlayId;
    }
}

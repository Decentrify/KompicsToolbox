/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Croupier is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.croupier;

import com.google.common.base.Optional;
import se.sics.kompics.config.Config;
import se.sics.ktoolbox.croupier.util.CroupierAggLevel;
import se.sics.ktoolbox.util.config.KConfigHelper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CroupierKCWrapper {

    public final Config configCore;
    public final CroupierSelectionPolicy policy;
    public final int viewSize;
    public final int minViewSize;
    public final int shuffleSize;
    public final long shufflePeriod;
    public final int shuffleSpeedFactor;
    public final long shuffleTimeout;
    public final boolean softMax;
    public final Optional<Double> softMaxTemp;
    
    public final CroupierAggLevel croupierAggLevel;
    public final long croupierAggPeriod;

    public CroupierKCWrapper(Config configCore) {
        this.configCore = configCore;
        policy = KConfigHelper.read(configCore, CroupierKConfig.selectionPolicy);
        viewSize = KConfigHelper.read(configCore, CroupierKConfig.viewSize);
        minViewSize = (viewSize / 4 < 2 ? viewSize / 4 : 2);
        shuffleSize = KConfigHelper.read(configCore, CroupierKConfig.shuffleSize);
        shufflePeriod = KConfigHelper.read(configCore, CroupierKConfig.shufflePeriod);
        shuffleSpeedFactor = KConfigHelper.read(configCore, CroupierKConfig.shuffleSpeedFactor);
        shuffleTimeout = KConfigHelper.read(configCore, CroupierKConfig.shuffleTimeout);
        softMax = KConfigHelper.read(configCore, CroupierKConfig.softMax);
        softMaxTemp = CroupierKConfig.softMaxTemp.readValue(configCore);
        if (shufflePeriod / shuffleSpeedFactor < shuffleTimeout) {
            String cause = "shufflePeriod:" + shufflePeriod;
            cause += "should be at least shuffleTimeout" + shuffleTimeout;
            cause += " * shuffleSpeedFactor:" + shuffleSpeedFactor;
            throw new RuntimeException(cause);
        }
        croupierAggLevel = KConfigHelper.read(configCore, CroupierKConfig.aggLevel);
        croupierAggPeriod = KConfigHelper.read(configCore, CroupierKConfig.aggPeriod);
    }
}

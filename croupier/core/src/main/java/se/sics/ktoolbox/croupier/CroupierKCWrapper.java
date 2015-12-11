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
import se.sics.ktoolbox.croupier.CroupierSelectionPolicy;
import se.sics.p2ptoolbox.util.config.KConfigHelper;
import se.sics.p2ptoolbox.util.config.KConfigCore;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CroupierKCWrapper {

    public final KConfigCore configCore;
    public final CroupierSelectionPolicy policy;
    public final int viewSize;
    public final int minViewSize;
    public final int shuffleSize;
    public final long shufflePeriod;
    public final long shuffleTimeout;
    public final boolean softMax;
    public final Optional<Double> softMaxTemp;

    public CroupierKCWrapper(KConfigCore configCore) {
        this.configCore = configCore;
        policy = KConfigHelper.read(configCore, CroupierKConfig.sPolicy);
        viewSize = KConfigHelper.read(configCore, CroupierKConfig.viewSize);
        minViewSize = (viewSize / 4 < 2 ? viewSize / 4 : 2);
        shuffleSize = KConfigHelper.read(configCore, CroupierKConfig.shuffleSize);
        shufflePeriod = KConfigHelper.read(configCore, CroupierKConfig.shufflePeriod);
        shuffleTimeout = KConfigHelper.read(configCore, CroupierKConfig.shuffleTimeout);
        softMax = KConfigHelper.read(configCore, CroupierKConfig.softMax);
        softMaxTemp = configCore.readValue(CroupierKConfig.softMaxTemp);
    }
}

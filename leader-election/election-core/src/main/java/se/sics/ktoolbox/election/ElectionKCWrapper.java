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
package se.sics.ktoolbox.election;

import se.sics.kompics.config.Config;
import se.sics.ktoolbox.util.aggregation.AggregationLevel;
import se.sics.ktoolbox.util.config.KConfigHelper;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ElectionKCWrapper {

    public final Config configCore;

    public final long leaderLeaseTime;
    public final long followerLeaseTime;
    public final int viewSize;
    public final int convergenceRounds;
    public final double convergenceTest;
    public final int maxLeaderGroupSize;

    public final AggregationLevel electionAggLevel;
    public final long electionAggPeriod;

    public ElectionKCWrapper(Config configCore) {
        this.configCore = configCore;
        viewSize = KConfigHelper.read(configCore, ElectionKConfig.viewSize);
        maxLeaderGroupSize = KConfigHelper.read(configCore, ElectionKConfig.maxLeaderGroupSize);
        leaderLeaseTime = KConfigHelper.read(configCore, ElectionKConfig.leaderLeaseTime);
        followerLeaseTime = KConfigHelper.read(configCore, ElectionKConfig.followerLeaseTime);
        convergenceRounds = KConfigHelper.read(configCore, ElectionKConfig.convergenceRounds);
        convergenceTest = KConfigHelper.read(configCore, ElectionKConfig.convergenceTest);

        if (leaderLeaseTime >= followerLeaseTime) {
            throw new RuntimeException("Leader Lease should always be less than follower lease");
        }

        electionAggLevel = KConfigHelper.read(configCore, ElectionKConfig.aggLevel);
        electionAggPeriod = KConfigHelper.read(configCore, ElectionKConfig.aggPeriod);
    }
    
    @Override
    public String toString() {
        return "ElectionConfig{" +
                "leaderLeaseTime=" + leaderLeaseTime +
                ", followerLeaseTime=" + followerLeaseTime +
                ", viewSize=" + viewSize +
                ", convergenceRounds=" + convergenceRounds +
                ", convergenceTest=" + convergenceTest +
                ", maxLeaderGroupSize=" + maxLeaderGroupSize +
                '}';
    }
}

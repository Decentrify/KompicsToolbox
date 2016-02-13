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
package se.sics.ktoolbox.election.aggregation;

import se.sics.ktoolbox.election.api.ports.LeaderElectionPort;
import se.sics.ktoolbox.election.event.ElectionState;
import se.sics.ktoolbox.election.event.ExtensionUpdate;
import se.sics.ktoolbox.election.event.LeaderState;
import se.sics.ktoolbox.election.event.LeaderUpdate;
import se.sics.ktoolbox.election.event.ViewUpdate;
import se.sics.ktoolbox.election.junk.MockedGradientUpdate;
import se.sics.ktoolbox.election.junk.TestPort;
import se.sics.ktoolbox.util.aggregation.AggregationRegistry;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ElectionAggregation {

    public static void registerPorts() {
        AggregationRegistry.registerPositive(LeaderUpdate.class, LeaderElectionPort.class);
        AggregationRegistry.registerNegative(ViewUpdate.class, LeaderElectionPort.class);
        AggregationRegistry.registerPositive(LeaderState.ElectedAsLeader.class, LeaderElectionPort.class);
        AggregationRegistry.registerPositive(LeaderState.TerminateBeingLeader.class, LeaderElectionPort.class);
        AggregationRegistry.registerPositive(ExtensionUpdate.class, LeaderElectionPort.class);
        AggregationRegistry.registerPositive(ElectionState.EnableLGMembership.class, LeaderElectionPort.class);
        AggregationRegistry.registerPositive(ElectionState.DisableLGMembership.class, LeaderElectionPort.class);
        
        //TODO Alex - in time remove- cleanup testing code from deployment code
        AggregationRegistry.registerNegative(MockedGradientUpdate.class, TestPort.class);
    }
}

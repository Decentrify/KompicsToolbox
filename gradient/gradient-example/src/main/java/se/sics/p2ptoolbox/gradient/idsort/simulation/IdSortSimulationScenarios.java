/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
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
package se.sics.p2ptoolbox.gradient.idsort.simulation;

import se.sics.p2ptoolbox.simulator.dsl.SimulationScenario;
import se.sics.p2ptoolbox.simulator.dsl.distribution.extra.BasicIntSequentialDistribution;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class IdSortSimulationScenarios {

    public static SimulationScenario simpleBoot(final int nodes, final long seed, final int simulatedSeconds, double softMaxTemperature) {
        SimulationScenario scen = new SimulationScenario() {
            {
                StochasticProcess startPeers = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(nodes, IdSortScenarioOperations.startNodeOp, new BasicIntSequentialDistribution(1));
                    }
                };
                
                StochasticProcess newPeers = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(nodes, IdSortScenarioOperations.startNodeOp, new BasicIntSequentialDistribution(1 + nodes));
                    }
                };
                
                StochasticProcess fetchSimulationResult = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, IdSortScenarioOperations.simulationResult);
                    }
                };

                startPeers.start();
                newPeers.startAfterTerminationOf(simulatedSeconds*1000, startPeers);
                fetchSimulationResult.startAfterTerminationOf(simulatedSeconds * 1000, newPeers);
                terminateAfterTerminationOf(5000, fetchSimulationResult);
            }
        };
        IdSortScenarioOperations.seed = seed;
        scen.setSeed(seed);
        return scen;
    }

}

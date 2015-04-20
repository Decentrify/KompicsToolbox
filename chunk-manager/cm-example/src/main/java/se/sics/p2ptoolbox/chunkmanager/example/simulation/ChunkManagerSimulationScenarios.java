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
package se.sics.p2ptoolbox.chunkmanager.example.simulation;

import se.sics.p2ptoolbox.simulator.dsl.SimulationScenario;
import se.sics.p2ptoolbox.simulator.dsl.distribution.extra.BasicIntSequentialDistribution;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ChunkManagerSimulationScenarios {

    public static SimulationScenario simpleBoot(final int nodes, final long seed, final int simulatedSeconds) {
        SimulationScenario scen = new SimulationScenario() {
            {
                StochasticProcess startPeer1 = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, ChunkManagerScenarioOperations.startNodeOp, new BasicIntSequentialDistribution(1), new BasicIntSequentialDistribution(0));
                    }
                };
                StochasticProcess startPeer2 = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, ChunkManagerScenarioOperations.startNodeOp, new BasicIntSequentialDistribution(2), new BasicIntSequentialDistribution(1));
                    }
                };
                StochasticProcess fetchSimulationResult = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, ChunkManagerScenarioOperations.simulationResult);
                    }
                };

                startPeer1.start();
                startPeer2.startAfterTerminationOf(1000, startPeer1);
                fetchSimulationResult.startAfterTerminationOf(simulatedSeconds*1000, startPeer2);
                terminateAfterTerminationOf(5*1000, fetchSimulationResult);
            }
        };
        ChunkManagerScenarioOperations.seed = seed;
        scen.setSeed(seed);
        return scen;
    }

}

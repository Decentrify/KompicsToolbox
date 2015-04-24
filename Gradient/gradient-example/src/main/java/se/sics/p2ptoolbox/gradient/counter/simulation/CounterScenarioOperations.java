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
package se.sics.p2ptoolbox.gradient.counter.simulation;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import se.sics.p2ptoolbox.gradient.simulation.GradientSimulationResult;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.javatuples.Pair;
import se.sics.p2ptoolbox.croupier.CroupierConfig;
import se.sics.p2ptoolbox.gradient.GradientConfig;
import se.sics.p2ptoolbox.gradient.counter.CounterHostComp;
import se.sics.p2ptoolbox.simulator.cmd.OperationCmd;
import se.sics.p2ptoolbox.simulator.cmd.impl.SimulationResult;
import se.sics.p2ptoolbox.simulator.cmd.impl.StartAggregatorCmd;
import se.sics.p2ptoolbox.simulator.cmd.impl.StartNodeCmd;
import se.sics.p2ptoolbox.simulator.dsl.adaptor.Operation;
import se.sics.p2ptoolbox.simulator.dsl.adaptor.Operation1;
import se.sics.p2ptoolbox.simulator.dsl.adaptor.Operation2;
import se.sics.p2ptoolbox.util.config.SystemConfig;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CounterScenarioOperations {

    public static long seed = 1234l;
    private static int bootstrapSize = 5;
    private static InetAddress localHost;
    static {
        try {
            localHost = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
    }
    private final static Pair<Integer, Integer> counterAction = Pair.with(1000, 10);
    private final static Map<Integer, Pair<Double, Integer>> counterRateMap = new HashMap<Integer, Pair<Double, Integer>>();
    static {
        counterRateMap.put(1, Pair.with(0.95d, 5)); //medium
        counterRateMap.put(2, Pair.with(0.99d, 15)); //slow but big steps
        counterRateMap.put(3, Pair.with(0.90d, 1)); //fast but little steps
    }

    static Operation1<StartAggregatorCmd, Integer> startAggregatorOp = new Operation1<StartAggregatorCmd, Integer>() {

        @Override
        public StartAggregatorCmd generate(final Integer nodeId) {
            return null;
        }
    };

    static Operation2<StartNodeCmd, Integer, Integer> startNodeOp = new Operation2<StartNodeCmd, Integer, Integer>() {

        @Override
        public StartNodeCmd generate(final Integer nodeId, final Integer counterRateType) {
            return new StartNodeCmd<CounterHostComp, DecoratedAddress>() {
                private DecoratedAddress nodeAddress;

                @Override
                public Class getNodeComponentDefinition() {
                    return CounterHostComp.class;
                }

                @Override
                public CounterHostComp.HostInit getNodeComponentInit(DecoratedAddress aggregatorServer, Set<DecoratedAddress> bootstrapNodes) {
                    //open address
                    nodeAddress = new DecoratedAddress(new BasicAddress(localHost, 12345, nodeId));
                    /**
                     * we don't want all nodes to start their pseudo random
                     * generators with same seed else they might behave the same
                     */
                    long nodeSeed = seed + nodeId;
                    Config config = ConfigFactory.load("application.conf");
                    SystemConfig systemConfig  = new SystemConfig(nodeSeed, nodeAddress, aggregatorServer, new ArrayList<DecoratedAddress>(bootstrapNodes));
                    CroupierConfig croupierConfig = new CroupierConfig(config);
                    GradientConfig gradientConfig = new GradientConfig(config);
                    return new CounterHostComp.HostInit(systemConfig, croupierConfig, gradientConfig, counterAction, counterRateMap.get(counterRateType));
                }

                @Override
                public Integer getNodeId() {
                    return nodeId;
                }

                @Override
                public DecoratedAddress getAddress() {
                    return nodeAddress;
                }

                @Override
                public int bootstrapSize() {
                    return bootstrapSize;
                }

            };
        }
    };

    static Operation<SimulationResult> simulationResult = new Operation<SimulationResult>() {

        public SimulationResult generate() {
            return new SimulationResult() {
                @Override
                public void setSimulationResult(OperationCmd.ValidationException failureCause) {
                    GradientSimulationResult.failureCause = failureCause;
                }
            };
        }
    };
}

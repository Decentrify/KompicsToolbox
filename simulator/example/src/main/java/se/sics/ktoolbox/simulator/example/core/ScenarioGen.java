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
package se.sics.ktoolbox.simulator.example.core;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import se.sics.ktoolbox.simulator.SimulationScenario;
import se.sics.ktoolbox.simulator.adaptor.Operation1;
import se.sics.ktoolbox.simulator.distributions.extra.BasicIntSequentialDistribution;
import se.sics.ktoolbox.simulator.events.system.StartNodeEvent;
import se.sics.ktoolbox.util.address.basic.BasicAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ScenarioGen {

    private static final Map<Integer, BasicAddress> nodeAddressMap = new HashMap<Integer, BasicAddress>();

    static {
        InetAddress localHost;
        try {
            localHost = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
        nodeAddressMap.put(1, new BasicAddress(localHost, 12345, 1));
        nodeAddressMap.put(2, new BasicAddress(localHost, 12346, 2));
        nodeAddressMap.put(3, new BasicAddress(localHost, 12347, 3));
        nodeAddressMap.put(4, new BasicAddress(localHost, 12348, 4));
    }

    static Operation1<StartNodeEvent, Integer> startNodeOp = new Operation1<StartNodeEvent, Integer>() {

        @Override
        public StartNodeEvent generate(final Integer nodeId) {
            return new StartNodeEvent<MyComponent, BasicAddress>() {

                @Override
                public Integer getNodeId() {
                    return nodeId;
                }

                @Override
                public Class getComponentDefinition() {
                    return MyComponent.class;
                }

                @Override
                public MyComponent.MyInit getComponentInit() {
                    BasicAddress self = nodeAddressMap.get(nodeId);
                    BasicAddress ping = null;
                    if (nodeId < nodeAddressMap.size()) {
                        ping = nodeAddressMap.get(nodeId + 1);
                    }
                    return new MyComponent.MyInit(self, ping);
                }

                @Override
                public BasicAddress getAddress() {
                    return nodeAddressMap.get(nodeId);
                }

                @Override
                public String toString() {
                    return "START<" + MyComponent.class.getName() + ":" + nodeId + ">";
                }
            };
        }
    };

//    static Operation<ChangeNetworkModelCmd> changeNetworkModelOp = new Operation<ChangeNetworkModelCmd>() {
//
//        @Override
//        public ChangeNetworkModelCmd generate() {
//            NetworkModel networkModel = new PartitionedNetworkModel(new UniformRandomModel(50, 500), PartitionMapperFactory.get2EqualPartitions());
//            return new ChangeNetworkModelCmd(networkModel);
//        }
//    };
    public static SimulationScenario simpleBoot() {
        SimulationScenario scen = new SimulationScenario() {
            {
                StochasticProcess startPeers = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(4, startNodeOp, new BasicIntSequentialDistribution(1));
                    }
                };

                startPeers.start();
                terminateAfterTerminationOf(20000, startPeers);
            }
        };

        return scen;
    }
}

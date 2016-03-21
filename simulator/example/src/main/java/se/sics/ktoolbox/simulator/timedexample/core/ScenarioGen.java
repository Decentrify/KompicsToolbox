package se.sics.ktoolbox.simulator.timedexample.core;

///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * GVoD is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package se.sics.p2ptoolbox.simulator.timedexample.core;
//
//import java.net.InetAddress;
//import java.net.UnknownHostException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Set;
//import se.sics.ktoolbox.util.address.basic.BasicAddress;
//import se.sics.p2ptoolbox.simulator.cmd.impl.StartNodeCmd;
//import se.sics.p2ptoolbox.simulator.dsl.SimulationScenario;
//import se.sics.p2ptoolbox.simulator.dsl.adaptor.Operation1;
//import se.sics.p2ptoolbox.simulator.dsl.distribution.extra.BasicIntSequentialDistribution;
//
///**
// * @author Alex Ormenisan <aaor@sics.se>
// */
//public class ScenarioGen {
//
//    private static final Map<Integer, BasicAddress> nodeAddressMap = new HashMap<Integer, BasicAddress>();
//
//    static {
//        InetAddress localHost;
//        try {
//            localHost = InetAddress.getByName("127.0.0.1");
//        } catch (UnknownHostException ex) {
//            throw new RuntimeException(ex);
//        }
//        nodeAddressMap.put(1, new BasicAddress(localHost, 12345, 1));
//        nodeAddressMap.put(2, new BasicAddress(localHost, 12345, 2));
//        nodeAddressMap.put(3, new BasicAddress(localHost, 12345, 3));
//        nodeAddressMap.put(4, new BasicAddress(localHost, 12345, 4));
//    }
//
//    static Operation1<StartNodeCmd, Integer> startNodeOp = new Operation1<StartNodeCmd, Integer>() {
//
//        @Override
//        public StartNodeCmd generate(final Integer nodeId) {
//            return new StartNodeCmd<MyComponent, BasicAddress>() {
//
//                @Override
//                public Integer getNodeId() {
//                    return nodeId;
//                }
//
//                @Override
//                public Class getNodeComponentDefinition() {
//                    return MyComponent.class;
//                }
//
//                @Override
//                public MyComponent.MyInit getNodeComponentInit(BasicAddress aggregatorServer, Set<BasicAddress> bootstrapNodes) {
//                    BasicAddress self = nodeAddressMap.get(nodeId);
//                    BasicAddress ping = null;
//                    if (nodeId < nodeAddressMap.size()) {
//                        ping = nodeAddressMap.get(nodeId + 1);
//                    }
//                    return new MyComponent.MyInit(self, ping);
//                }
//
//                @Override
//                public BasicAddress getAddress() {
//                    return nodeAddressMap.get(nodeId);
//                }
//
//                @Override
//                public int bootstrapSize() {
//                    return 5;
//                }
//
//                @Override
//                public String toString() {
//                    return "START<" + MyComponent.class.getCanonicalName() + ">";
//                }
//            };
//        }
//    };
//
////    static Operation<ChangeNetworkModelCmd> changeNetworkModelOp = new Operation<ChangeNetworkModelCmd>() {
////
////        @Override
////        public ChangeNetworkModelCmd generate() {
////            NetworkModel networkModel = new PartitionedNetworkModel(new UniformRandomModel(50, 500), PartitionMapperFactory.get2EqualPartitions());
////            return new ChangeNetworkModelCmd(networkModel);
////        }
////    };
////    static Operation<SimulationResult> simulationResult = new Operation<SimulationResult>() {
////
////        public SimulationResult generate() {
////            return new SimulationResult() {
////
////                @Override
////                public void setSimulationResult(OperationCmd.ValidationException failureCause) {
////                    MyExperimentResult.failureCause = failureCause;
////                }
////
////            };
////
////        }
////    };
//    public static SimulationScenario simpleBoot() {
//        SimulationScenario scen = new SimulationScenario() {
//            {
//                StochasticProcess startPeers = new StochasticProcess() {
//                    {
//                        eventInterArrivalTime(constant(1000));
//                        raise(4, startNodeOp, new BasicIntSequentialDistribution(1));
//                    }
//                };
//
//                startPeers.start();
//                terminateAfterTerminationOf(20000, startPeers);
//
//            }
//        };
//
//        return scen;
//    }
//
////    public static SimulationScenario simpleChangeNetworkModel(final long seed) {
////        SimulationScenario scen = new SimulationScenario() {
////            {
////                StochasticProcess startPeer = new StochasticProcess() {
////                    {
////                        eventInterArrivalTime(constant(1000));
////                        raise(1, startNodeOp, new ConstantDistribution<Integer>(Integer.class, 1));
////                    }
////                };
////                StochasticProcess changeNetworkModel = new StochasticProcess() {
////                    {
////                        eventInterArrivalTime(constant(1000));
////                        raise(1, changeNetworkModelOp);
////                    }
////                };
////                StochasticProcess fetchSimulationResult = new StochasticProcess() {
////                    {
////                        eventInterArrivalTime(constant(1000));
////                        raise(1, simulationResult);
////                    }
////                };
////
////                startPeer.start();
////                changeNetworkModel.startAfterTerminationOf(1000, startPeer);
////                fetchSimulationResult.startAfterTerminationOf(1000, changeNetworkModel);
////                terminateAfterTerminationOf(1000, fetchSimulationResult);
////            }
////        };
////        scen.setSeed(seed);
////        return scen;
////    }
//}

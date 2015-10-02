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
package se.sics.p2ptoolbox.simulator.timedexample.simulation;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import se.sics.kompics.network.Msg;
import se.sics.p2ptoolbox.simulator.cmd.impl.StartNodeCmd;
import se.sics.p2ptoolbox.simulator.SimulationContext;
import se.sics.p2ptoolbox.simulator.cmd.impl.ChangeNetworkModelCmd;
import se.sics.p2ptoolbox.simulator.cmd.NetworkOpCmd;
import se.sics.p2ptoolbox.simulator.cmd.OperationCmd;
import se.sics.p2ptoolbox.simulator.cmd.impl.SimulationResult;
import se.sics.p2ptoolbox.simulator.core.network.NetworkModel;
import se.sics.p2ptoolbox.simulator.core.network.impl.PartitionedNetworkModel;
import se.sics.p2ptoolbox.simulator.core.network.impl.UniformRandomModel;
import se.sics.p2ptoolbox.simulator.core.network.PartitionMapperFactory;
import se.sics.p2ptoolbox.simulator.dsl.SimulationScenario;
import se.sics.p2ptoolbox.simulator.dsl.adaptor.Operation;
import se.sics.p2ptoolbox.simulator.dsl.adaptor.Operation1;
import se.sics.p2ptoolbox.simulator.dsl.distribution.ConstantDistribution;
import se.sics.p2ptoolbox.simulator.timedexample.core.MyNetMsg;
import se.sics.p2ptoolbox.simulator.timedexample.core.MyComponent;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ScenarioGen {

    private static final Map<Integer, DecoratedAddress> nodeAddressMap = new HashMap<Integer, DecoratedAddress>();

    static {
        InetAddress localHost;
        try {
            localHost = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
        nodeAddressMap.put(1, new DecoratedAddress(new BasicAddress(localHost, 12345, 1)));
        nodeAddressMap.put(2, new DecoratedAddress(new BasicAddress(localHost, 12345, 2)));
    }

    static Operation1<StartNodeCmd, Integer> startNodeOp = new Operation1<StartNodeCmd, Integer>() {

        @Override
        public StartNodeCmd generate(final Integer nodeId) {
            return new StartNodeCmd<MyComponent, DecoratedAddress>() {

                @Override
                public Integer getNodeId() {
                    return nodeId;
                }

                @Override
                public Class getNodeComponentDefinition() {
                    return MyComponent.class;
                }

                @Override
                public MyComponent.MyInit getNodeComponentInit(DecoratedAddress aggregatorServer, Set<DecoratedAddress> bootstrapNodes) {
                    return new MyComponent.MyInit(nodeAddressMap.get(nodeId), bootstrapNodes);
                }

                @Override
                public DecoratedAddress getAddress() {
                    return nodeAddressMap.get(nodeId);
                }

                @Override
                public int bootstrapSize() {
                    return 5;
                }

            };
        }
    };
    
    static Operation1<NetworkOpCmd, Integer> networkPingOp = new Operation1<NetworkOpCmd, Integer>() {

        public NetworkOpCmd generate(final Integer nodeId) {
            return new NetworkOpCmd<DecoratedAddress>() {
                private DecoratedAddress destination = nodeAddressMap.get(nodeId);
                private DecoratedAddress origin;
                private UUID pingId = UUID.randomUUID();

                @Override
                public Msg getNetworkMsg(DecoratedAddress origin) {
                    this.origin = origin;
                    return new MyNetMsg.NetPing(origin, destination, pingId);
                }

                @Override
                public void beforeCmd(SimulationContext context) {
                    return;
                }

                @Override
                public void afterValidation(SimulationContext context) {
                    return;
                }

                @Override
                public void validate(SimulationContext context, Msg response) throws OperationCmd.ValidationException {
                    if (!(response instanceof MyNetMsg.NetPong)) {
                        throw new OperationCmd.ValidationException("wrong message type");
                    }
                    if(((MyNetMsg.NetPong)response).getContent().id.equals(pingId)) {
                       return; 
                    }
                    throw new OperationCmd.ValidationException("wrong ping");
                }

                @Override
                public boolean myResponse(Msg resp) {
                    if(!(resp instanceof MyNetMsg.NetPong)) {
                        return false;
                    }
                    if(resp.getHeader().getDestination().equals(origin)) {
                        return true;
                    }
                    return false;
                }

            };

        }
    };
    
    static Operation<ChangeNetworkModelCmd> changeNetworkModelOp = new Operation<ChangeNetworkModelCmd>() {

        @Override
        public ChangeNetworkModelCmd generate() {
            NetworkModel networkModel = new PartitionedNetworkModel(new UniformRandomModel(50, 500), PartitionMapperFactory.get2EqualPartitions());
            return new ChangeNetworkModelCmd(networkModel);
        }
    };

    static Operation<SimulationResult> simulationResult = new Operation<SimulationResult>() {

        public SimulationResult generate() {
            return new SimulationResult() {

                @Override
                public void setSimulationResult(OperationCmd.ValidationException failureCause) {
                    MyExperimentResult.failureCause = failureCause;
                }

            };

        }
    };
    
    public static SimulationScenario simpleBoot() {
        SimulationScenario scen = new SimulationScenario() {
            {
                StochasticProcess startPeers = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, startNodeOp, new ConstantDistribution<Integer>(Integer.class, 1));
                        raise(1, startNodeOp, new ConstantDistribution<Integer>(Integer.class, 2));
                    }
                };

                StochasticProcess networkPing = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(5, networkPingOp, new ConstantDistribution<Integer>(Integer.class, 1));
                        raise(5, networkPingOp, new ConstantDistribution<Integer>(Integer.class, 2));
                    }
                };
                
                StochasticProcess fetchSimulationResult = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, simulationResult);
                    }
                };

                startPeers.start();
                networkPing.startAfterTerminationOf(1000, startPeers);
                terminateAfterTerminationOf(10000, networkPing);

            }
        };

        return scen;
    }

    public static SimulationScenario simpleChangeNetworkModel(final long seed) {
        SimulationScenario scen = new SimulationScenario() {
            {
                StochasticProcess startPeer = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, startNodeOp, new ConstantDistribution<Integer>(Integer.class, 1));
                    }
                };
                StochasticProcess changeNetworkModel = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, changeNetworkModelOp);
                    }
                };
                StochasticProcess fetchSimulationResult = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, simulationResult);
                    }
                };
                
                startPeer.start();
                changeNetworkModel.startAfterTerminationOf(1000, startPeer);
                fetchSimulationResult.startAfterTerminationOf(1000, changeNetworkModel);
                terminateAfterTerminationOf(1000, fetchSimulationResult);
            }
        };
        scen.setSeed(seed);
        return scen;
    }
}

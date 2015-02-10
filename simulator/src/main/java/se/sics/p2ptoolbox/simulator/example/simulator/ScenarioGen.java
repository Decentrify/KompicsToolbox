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
package se.sics.p2ptoolbox.simulator.example.simulator;

import se.sics.p2ptoolbox.simulator.example.proj.MyComponent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import se.sics.gvod.address.Address;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.msgs.DirectMsg;
import se.sics.kompics.Init;
import se.sics.p2ptoolbox.simulator.cmd.StartNodeCmd;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation1;
import se.sics.kompics.p2p.experiment.dsl.distribution.ConstantDistribution;
import se.sics.kompics.p2p.experiment.dsl.distribution.Distribution;
import se.sics.p2ptoolbox.simulator.SimulationContext;
import se.sics.p2ptoolbox.simulator.cmd.NetworkOpCmd;
import se.sics.p2ptoolbox.simulator.cmd.OperationCmd;
import se.sics.p2ptoolbox.simulator.cmd.SimulationResult;
import se.sics.p2ptoolbox.simulator.example.proj.MyNetMsg;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ScenarioGen {

    private static final Distribution<Integer> nodeDist1 = new ConstantDistribution<Integer>(Integer.class, 1);
    private static final Map<Integer, VodAddress> nodeAddressMap = new HashMap<Integer, VodAddress>();

    static {
        InetAddress localHost;
        try {
            localHost = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
        nodeAddressMap.put(1, new VodAddress(new Address(localHost, 12345, 1), 0));
        nodeAddressMap.put(2, new VodAddress(new Address(localHost, 12345, 2), 0));
    }

    static Operation1<StartNodeCmd, Integer> startNodeOp = new Operation1<StartNodeCmd, Integer>() {

        @Override
        public StartNodeCmd generate(final Integer nodeId) {
            return new StartNodeCmd<MyComponent>() {

                @Override
                public Integer getNodeId() {
                    return nodeId;
                }

                @Override
                public Class getNodeComponentDefinition() {
                    return MyComponent.class;
                }

                @Override
                public MyComponent.MyInit getNodeComponentInit(VodAddress statusServer) {
                    return new MyComponent.MyInit(nodeAddressMap.get(nodeId), statusServer);
                }
            };
        }
    };

    static Operation1<NetworkOpCmd, Integer> networkPingOp = new Operation1<NetworkOpCmd, Integer>() {

        public NetworkOpCmd generate(final Integer nodeId) {
            return new NetworkOpCmd() {
                private VodAddress destination = nodeAddressMap.get(nodeId);
                
                @Override
                public DirectMsg getNetworkMsg(VodAddress origin) {
                    return new MyNetMsg.Ping(origin, destination);
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
                public void validate(SimulationContext context, DirectMsg response) throws OperationCmd.ValidationException {
                    if (!(response instanceof MyNetMsg.Pong)) {
                        throw new OperationCmd.ValidationException("wrong message type");
                    }
                    MyNetMsg.Pong pong = (MyNetMsg.Pong) response;
                    if (response.getVodSource().getId() != nodeId) {
                        throw new OperationCmd.ValidationException("node id is wrong");
                    }
                }

                @Override
                public boolean myResponse(DirectMsg resp) {
                    if(resp instanceof MyNetMsg.Pong) {
                        MyNetMsg.Pong pong = (MyNetMsg.Pong) resp;
                        if(pong.getVodSource().equals(destination)) {
                            return true;
                        }
                    }
                    return false;
                }

            };

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

    public static SimulationScenario simpleBoot(final long seed) {
        SimulationScenario scen = new SimulationScenario() {
            {
                StochasticProcess startPeer = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, startNodeOp, nodeDist1);
                    }
                };

                StochasticProcess networkPing = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, networkPingOp, nodeDist1);
                    }
                };
                
                StochasticProcess fetchSimulationResult = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, simulationResult);
                    }
                };

                startPeer.start();
                networkPing.startAfterTerminationOf(1000, startPeer);
                fetchSimulationResult.startAfterTerminationOf(1000, networkPing);
                terminateAfterTerminationOf(1000, fetchSimulationResult);

            }
        };

        scen.setSeed(seed);

        return scen;
    }
}

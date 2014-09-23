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
package se.sics.p2ptoolbox.simulator.exampleMain;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import se.sics.gvod.address.Address;
import se.sics.gvod.net.VodAddress;
import se.sics.p2ptoolbox.simulator.cmd.StartPeerCmd;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation1;
import se.sics.kompics.p2p.experiment.dsl.adaptor.Operation2;
import se.sics.kompics.p2p.experiment.dsl.distribution.ConstantDistribution;
import se.sics.kompics.p2p.experiment.dsl.distribution.Distribution;
import se.sics.p2ptoolbox.simulator.cmd.LocalCmd;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ScenarioGen {

    private static final Distribution<Integer> peer1 = new ConstantDistribution<Integer>(Integer.class, 1);
    private static final Distribution<Integer> peer2 = new ConstantDistribution<Integer>(Integer.class, 2);
    private static final Map<Integer, VodAddress> peerAddressMap = new HashMap<Integer, VodAddress>();
    
    static {
        InetAddress localHost;
        try {
            localHost = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
        peerAddressMap.put(1, new VodAddress(new Address(localHost, 12345, 1), 0));
        peerAddressMap.put(2, new VodAddress(new Address(localHost, 12345, 2), 0));
    }
    
    static Operation2<StartPeerCmd, Integer, Integer> startPeerOp
            = new Operation2<StartPeerCmd, Integer, Integer>() {

                @Override
                public StartPeerCmd generate(Integer peerId, Integer partnerId) {
                    return new StartPeerCmd(peerId, SimplePeer.class, SimplePeerPort.class,
                            new SimplePeer.SimplePeerInit(peerAddressMap.get(peerId), peerAddressMap.get(partnerId)));
                }
            };

    static Operation1<LocalCmd, Integer> localPingOp
            = new Operation1<LocalCmd, Integer>() {

                public LocalCmd generate(Integer peerId) {
                    return new LocalCmd(peerId, new TestMsg.Ping());
                }
            };

    public static SimulationScenario simpleBoot(final long seed) {
        SimulationScenario scen = new SimulationScenario() {
            {
                StochasticProcess startPeer = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, startPeerOp, peer1, peer2);
                    }
                };
                
                StochasticProcess startPartner = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, startPeerOp, peer2, peer1);
                    }
                };

                StochasticProcess localPing = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, localPingOp, peer2);
                    }
                };
                startPeer.start();
                startPartner.startAfterStartOf(1000, startPeer);
                localPing.startAfterTerminationOf(1000, startPartner);
                terminateAfterTerminationOf(1000 * 1000, localPing);
            }
        };

        scen.setSeed(seed);

        return scen;
    }
}

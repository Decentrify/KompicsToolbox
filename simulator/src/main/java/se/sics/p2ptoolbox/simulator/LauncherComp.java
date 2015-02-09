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
package se.sics.p2ptoolbox.simulator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.sics.gvod.address.Address;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.p2ptoolbox.simulator.core.P2pSimulator;
import se.sics.p2ptoolbox.simulator.core.P2pSimulatorInit;
import se.sics.p2ptoolbox.simulator.core.network.UniformRandomModel;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;
import se.sics.kompics.simulation.SimulatorScheduler;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class LauncherComp extends ComponentDefinition {
    public static SimulatorScheduler scheduler;
    public static SimulationScenario scenario;
    
    {
        VodAddress simAddress = null;
        try {
            simAddress = new VodAddress(new Address(InetAddress.getByName("127.0.0.1"), 45654, -1), -1);
        } catch (UnknownHostException ex) {
            Logger.getLogger(LauncherComp.class.getName()).log(Level.SEVERE, null, ex);
        }
        P2pSimulator.setSimulationPortType(ExperimentPort.class);
        Component simulator = create(P2pSimulator.class, new P2pSimulatorInit(scheduler, scenario, new UniformRandomModel(1, 10)));
        Component simManager = create(SimMngrComponent.class, new SimMngrComponent.SimMngrInit(new Random(), simAddress));
        connect(simManager.getNegative(VodNetwork.class), simulator.getPositive(VodNetwork.class));
        connect(simManager.getNegative(Timer.class), simulator.getPositive(Timer.class));
        connect(simManager.getNegative(ExperimentPort.class), simulator.getPositive(ExperimentPort.class));
    }
}

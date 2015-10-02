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
package se.sics.p2ptoolbox.simulator.timed.api;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Kompics;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.simulator.ExperimentPort;
import se.sics.p2ptoolbox.simulator.SimMngrComponent;
import se.sics.p2ptoolbox.simulator.SystemStatusHandler;
import se.sics.p2ptoolbox.simulator.core.P2pSimulator;
import se.sics.p2ptoolbox.simulator.core.P2pSimulatorInit;
import se.sics.p2ptoolbox.simulator.dsl.SimulationScenario;
import se.sics.p2ptoolbox.simulator.timed.TimedComp;
import se.sics.p2ptoolbox.simulator.timed.impl.TimedControlerBuilderImpl;
import se.sics.p2ptoolbox.simulator.timed.impl.TimedSimulatorScheduler;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TimedLauncher extends ComponentDefinition implements TimedComp {

    private static final Logger log = LoggerFactory.getLogger(TimedLauncher.class);

    public static TimedSimulatorScheduler simulatorScheduler = new TimedSimulatorScheduler();
    private static Address simulatorClientAddress;
    public static final Set<SystemStatusHandler> systemStatusHandlers = new HashSet<SystemStatusHandler>();

    static {
        try {
            simulatorClientAddress = new DecoratedAddress(new BasicAddress(InetAddress.getByName("127.0.0.1"), 30000, -1));
        } catch (UnknownHostException ex) {
            throw new RuntimeException("cannot create address for localhost");
        }
    }

    public static void main(String[] args) {
        Kompics.setScheduler(simulatorScheduler);
        Kompics.createAndStart(TimedLauncher.class, 1);
    }

    private SimulationScenario scenario = SimulationScenario.load(System.getProperty("scenario"));

    public TimedLauncher() {
        P2pSimulator.setSimulationPortType(ExperimentPort.class);
        Component simulator = create(P2pSimulator.class, new P2pSimulatorInit(simulatorScheduler, scenario, null));
        Component simManager = create(SimMngrComponent.class, new SimMngrComponent.SimMngrInit(new TimedControlerBuilderImpl(simulatorScheduler), new Random(), simulatorClientAddress, systemStatusHandlers));
        connect(simManager.getNegative(Network.class), simulator.getPositive(Network.class));
        connect(simManager.getNegative(Timer.class), simulator.getPositive(Timer.class));
        connect(simManager.getNegative(ExperimentPort.class), simulator.getPositive(ExperimentPort.class));
    }
}

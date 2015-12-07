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
package se.sics.ktoolbox.simulator.run;

import java.util.HashSet;
import java.util.Set;
import se.sics.kompics.Channel;
import se.sics.ktoolbox.simulator.P2pSimulator;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Kompics;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.simulator.P2pSimulator.P2pSimulatorInit;
import se.sics.ktoolbox.simulator.SimulationScenario;
import se.sics.ktoolbox.simulator.SimulatorMngrComp;
import se.sics.ktoolbox.simulator.SimulatorPort;
import se.sics.ktoolbox.simulator.scheduler.BasicSimulationScheduler;
import se.sics.ktoolbox.simulator.util.SystemStatusHandler;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class LauncherComp extends ComponentDefinition {
    public static BasicSimulationScheduler simulatorScheduler = new BasicSimulationScheduler();
    public static final Set<SystemStatusHandler> systemStatusHandlers = new HashSet<>();

    public static void main(String[] args) {
        simulationSetup();
        
        Kompics.setScheduler(simulatorScheduler);
        Kompics.createAndStart(LauncherComp.class, 1);
    }
    
    private static void simulationSetup() {
//        SimulationStatistics.cleanStatistics();
//        FingerprintDecorator fDecorator = new FingerprintDecorator();
//        Set<HandlerDecorator> beforeHandler = new HashSet<>();
//        beforeHandler.add(fDecorator);
//        Set<HandlerDecorator> afterHandler = new HashSet<>();
//        HandlerDecoratorRegistry.register(beforeHandler, afterHandler);
    }

    private final SimulationScenario scenario = SimulationScenario.load(System.getProperty("scenario"));
    
    public LauncherComp(){
        Component simulator = create(P2pSimulator.class, new P2pSimulatorInit(simulatorScheduler, scenario, null));
        Component simManager = create(SimulatorMngrComp.class, new SimulatorMngrComp.SimulatorMngrInit(systemStatusHandlers));
        connect(simManager.getNegative(Network.class), simulator.getPositive(Network.class), Channel.TWO_WAY);
        connect(simManager.getNegative(Timer.class), simulator.getPositive(Timer.class), Channel.TWO_WAY);
        connect(simManager.getNegative(SimulatorPort.class), simulator.getPositive(SimulatorPort.class), Channel.TWO_WAY);
    }
}

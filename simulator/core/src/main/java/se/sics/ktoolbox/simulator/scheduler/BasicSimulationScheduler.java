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
package se.sics.ktoolbox.simulator.scheduler;

import java.util.LinkedList;
import se.sics.kompics.Component;
import se.sics.kompics.Scheduler;
import se.sics.ktoolbox.simulator.Simulator;
import se.sics.ktoolbox.simulator.scheduler.SimulationScheduler;

/**
 * The <code>BasicSimulationScheduler</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class BasicSimulationScheduler extends Scheduler implements SimulationScheduler {

    private final LinkedList<Component> readyQueue = new LinkedList<>();
    private Simulator simulator;
    private boolean shutdown = false;

    @Override
    public void setSimulator(Simulator simulator) {
        this.simulator = simulator;
    }

    @Override
    public void schedule(Component c, int w) {
        readyQueue.add(c);
    }

    @Override
    public void proceed() {
        boolean ok = true;
        while (ok && !shutdown) {
            // sequentially execute all scheduled events
            while (!readyQueue.isEmpty()) {
                Component component = readyQueue.poll();
                executeComponent(component, 0);
            }
            // then advance simulation
            ok = simulator.advanceSimulation();
        }

        //TODO Alex - should I actually do this? - does it really matter?
        // execute all scheduled events after simulation terminates
        while (!readyQueue.isEmpty()) {
            Component component = readyQueue.poll();
            executeComponent(component, 0);
        }
    }

    @Override
    public void shutdown() {
        shutdown = true;
    }

    // TODO: document rationale for simulator not executed as a component:
    // could not control FEL order and the fact that the simulator would be
    // only executed in a quiescent state;
    @Override
    public void asyncShutdown() {
        shutdown();
    }
}

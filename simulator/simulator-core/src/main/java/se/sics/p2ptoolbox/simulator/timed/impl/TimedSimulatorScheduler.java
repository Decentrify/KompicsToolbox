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
package se.sics.p2ptoolbox.simulator.timed.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.simulation.Simulator;
import se.sics.kompics.simulation.SimulatorScheduler;
import se.sics.p2ptoolbox.simulator.timed.TimedComp;
import se.sics.p2ptoolbox.simulator.timed.TimedSimulator;
import se.sics.p2ptoolbox.simulator.timed.api.TimedControler;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TimedSimulatorScheduler extends SimulatorScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(TimedSimulatorScheduler.class);

    private final int nodeParalelism;
    private final Map<Integer, TimedNodeScheduler> nodeMap = new HashMap<>();
    private final Map<UUID, TimedNodeScheduler> compMap = new HashMap<>();
    private final LinkedList<Component> timedInternal = new LinkedList<>();

    private final SchedulerProxy sProxy = new SchedulerProxy();
    private TimedSimulator simulator;
    private boolean shutdown = false;

    public TimedSimulatorScheduler() {
        this(1);
    }

    public TimedSimulatorScheduler(int nodeParalelism) {
        this.nodeParalelism = nodeParalelism;
    }

    public void setSimulator(Simulator simulator) {
        this.simulator = (TimedSimulator) simulator;
    }

    public TimedControler register(int nodeId, ComponentDefinition comp) {
        TimedNodeScheduler tns = nodeMap.get(nodeId);
        if (tns == null) {
            tns = new TimedNodeScheduler(nodeParalelism);
            nodeMap.put(nodeId, tns);
        }
        compMap.put(comp.getComponentCore().id(), tns);
        return tns.register(comp);
    }

    //*****************************SCHEDULER************************************
    @Override
    public void schedule(Component comp, int w) {
        LOG.info("scheduling:{}", comp.getComponent().getClass());
        if (comp.getComponent() instanceof TimedComp) {
            timedInternal.add(comp);
            return;
        }
        TimedNodeScheduler tns = compMap.get(comp.id());
        if (tns == null) {
            throw new RuntimeException("timed simulation - unregistered component:" + comp.getComponent().getClass());
        }
        tns.schedule(comp, w);
    }

    @Override
    public void proceed() {
        boolean ok = true;
        long time = 0;
        while (ok && !shutdown) {
            LOG.info("time:{}", time);
            while (!timedInternal.isEmpty()) {
                Component component = timedInternal.poll();
                executeComponent(component, 0);
            }
            // sequentially execute all scheduled events
            long minDelay = Long.MAX_VALUE;
            long auxDelay;
            for (TimedNodeScheduler tns : nodeMap.values()) {
                auxDelay = tns.advance(sProxy, time);
                minDelay = (minDelay > auxDelay ? auxDelay : minDelay);
            }
            auxDelay = simulator.advanceSimulation(time);
            if (auxDelay == -1) {
                ok = false;
            } else {
                minDelay = (minDelay > auxDelay ? auxDelay : minDelay);
            }
            time = minDelay;
        }

        // execute all scheduled events after simulation terminates
        while (time != Long.MAX_VALUE) {
            while (!timedInternal.isEmpty()) {
                Component component = timedInternal.poll();
                executeComponent(component, 0);
            }
            long minDelay = Long.MAX_VALUE;
            long auxDelay;
            for (TimedNodeScheduler tns : nodeMap.values()) {
                auxDelay = tns.advance(sProxy, time);
                minDelay = (minDelay > auxDelay ? auxDelay : minDelay);
            }
            time = minDelay;
        }
    }

    @Override
    public void shutdown() {
        shutdown = true;
    }

    //TODO Alex - what does this comment mean?
    // TODO: document rationale for simulator not executed as a component:
    // could not control FEL order and the fact that the simulator would be
    // only executed in a quiescent state;
    @Override
    public void asyncShutdown() {
        shutdown();
    }

    public class SchedulerProxy {

        public void executeComponent(Component component, int w) {
            TimedSimulatorScheduler.this.executeComponent(component, w);
        }
    }
}

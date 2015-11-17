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

import com.google.common.base.Optional;
import com.google.common.util.concurrent.SettableFuture;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.p2ptoolbox.simulator.timed.api.TimedControler;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TimedNodeScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(TimedSimulatorScheduler.class);
    
    private final int maxThreads;
    //time controller of all components on a node
    private final Map<UUID, TimedControlerImpl> tcMap = new HashMap<>();
    //<componentId, busyUntil>
    private final Map<UUID, Long> busyComponents = new HashMap<>();
    //scheduled components
    private final LinkedList<Component> waiting = new LinkedList<>();

    public TimedNodeScheduler(int maxThreads) {
        this.maxThreads = maxThreads;
    }
    
    public TimedControler register(ComponentDefinition comp) {
        if(tcMap.containsKey(comp.getComponentCore().id())) {
            throw new RuntimeException("timed simulation - double component registration:" + comp.getClass());
        }
        TimedControlerImpl tc = new TimedControlerImpl();
        tcMap.put(comp.getComponentCore().id(), tc);
        return tc;
    }
    
    public void schedule(Component comp, int w) {
        waiting.add(comp);
    }

    //<nextThingHappeningLocally, executedANewEvent>
    public Pair<Long, Boolean> advance(TimedSimulatorScheduler.SchedulerProxy proxy, long timeMilis) {
        boolean executingNewEvent = false;
        long minDelay = cleanBusyMap(timeMilis);
        if (busyComponents.size() == maxThreads) {
            //all simulated threads are busy
            return Pair.with(minDelay, executingNewEvent); 
        }
        while (!waiting.isEmpty() && busyComponents.size() < maxThreads) {
            Optional<Component> next = nextComponentToSchedule();
            if (!next.isPresent()) {
                //all waiting components are busy
                return Pair.with(minDelay, executingNewEvent); 
            }
            Long taskEnd = timeMilis + executeNextEventOn(proxy, next.get());
            executingNewEvent = true;
            busyComponents.put(next.get().id(), taskEnd);
            minDelay = (minDelay < taskEnd ? minDelay : taskEnd);
        }
        return Pair.with(minDelay, executingNewEvent);
    }

    private long cleanBusyMap(long timeMilis) {
        Iterator<Map.Entry<UUID, Long>> it = busyComponents.entrySet().iterator();
        long minDelay = Long.MAX_VALUE;
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            if (e.getValue() <= timeMilis) {
                it.remove();
            } else {
                minDelay = (minDelay < e.getValue() ? minDelay : e.getValue());
            }
        }
        return minDelay;
    }

    private Optional<Component> nextComponentToSchedule() {
        Iterator<Component> it = waiting.iterator();
        while (it.hasNext()) {
            Component next = it.next();
            if (!busyComponents.containsKey(next.id())) {
                it.remove();
                return Optional.of(next);
            }
        }
        return Optional.absent();
    }

    private long executeNextEventOn(TimedSimulatorScheduler.SchedulerProxy proxy, Component comp) {
        TimedControlerImpl tc = tcMap.get(comp.id());
        if(tc == null) {
            throw new RuntimeException("timed simulation - no controller registered for:" + comp.getComponent().getClass());
        }
        SettableFuture<Long> future = tc.eventTime(comp);
        proxy.executeComponent(comp, 0);
        try {
            //wait until you know how long the task takes
            LOG.trace("waiting on handler execution of component:{}<{}>", comp.getComponent().getClass().getCanonicalName(), comp.id());
            long executionTime = future.get();
            LOG.trace("component:{}<{}> returned, continuing...", comp.getComponent().getClass().getCanonicalName(), comp.id());
            return executionTime;
        } catch (InterruptedException ex) {
            throw new RuntimeException("scheduler future error");
        } catch (ExecutionException ex) {
            throw new RuntimeException("scheduler future error");
        }
    }
    
    @Override 
    public String toString() {
        return "node components:" + tcMap.size() + " busy size:" + busyComponents.size() + " waiting size:" + waiting.size();
    }
}

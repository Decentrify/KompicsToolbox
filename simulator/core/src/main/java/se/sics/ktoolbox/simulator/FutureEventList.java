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
package se.sics.ktoolbox.simulator;

import java.util.Collections;
import java.util.LinkedList;
import java.util.PriorityQueue;
import se.sics.ktoolbox.simulator.events.stochastic.StochasticSimulatorEvent;

/**
 * The <code>FutureEventList</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class FutureEventList {

    private PriorityQueue<StochasticSimulatorEvent> futureEventList = new PriorityQueue<>();

    public FutureEventList() {
    }

    void scheduleFutureEvent(long now, StochasticSimulatorEvent event) {
        if (event.getTime() < now) {
            throw new RuntimeException("Cannot schedule an event in the past");
        }
        futureEventList.add(event);
        event.setOnList(true);
    }

    boolean cancelFutureEvent(long now, StochasticSimulatorEvent event) {
        if (event == null) {
            throw new RuntimeException("Cannot cancel a null event");
        }

        boolean removed = futureEventList.remove(event);
        if (removed) {
            event.setOnList(false);
        }
        return removed;
    }

    boolean hasMoreEventsAtTime(long now) {
        StochasticSimulatorEvent event = futureEventList.peek();
        if (event != null) {
            return (event.getTime() == now);
        }
        return false;
    }

    long getFirstEventTime() {
        return futureEventList.isEmpty() ? -1 : futureEventList.peek().getTime();
    }

    StochasticSimulatorEvent getAndRemoveFirstEvent(long now) {
        StochasticSimulatorEvent event = futureEventList.poll();

        if (event != null) {
            event.setOnList(false);
        }

        return event;
    }

    void dumpFEL() {
        System.err.print(". FEL(" + futureEventList.size() + "): ");
        LinkedList<Long> times = new LinkedList<>();

        for (StochasticSimulatorEvent simulatorEvent : futureEventList) {
            times.add(simulatorEvent.getTime());
        }
        Collections.sort(times);

        for (Long long1 : times) {
            System.err.print(long1 + ", ");
        }
        System.err.println();
        System.err.flush();
    }
}

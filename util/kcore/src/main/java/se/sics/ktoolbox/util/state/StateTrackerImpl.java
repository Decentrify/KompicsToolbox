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
package se.sics.ktoolbox.util.state;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.util.UUID;
import org.javatuples.Pair;
import org.slf4j.Logger;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Handler;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.util.identifiable.Identifiable;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class StateTrackerImpl implements StateTracker {

    private final Logger log;
    private final String logPrefix;

    private final ComponentProxy cp;
    private final long checkPeriod;
    private UUID checkTid;

    public final Table<Class<? extends KompicsEvent>, Class<? extends PortType>, Integer> currentEvents = HashBasedTable.create();
    public final Multimap<Identifier, Handler> handlers = ArrayListMultimap.create();
    public final StateTrackedComp stateKeeper;

    public StateTrackerImpl(ComponentProxy cp, Pair<Logger, String> log, long checkPeriod, StateTrackedComp stateKeeper) {
        this.log = log.getValue0();
        this.logPrefix = log.getValue1();
        this.cp = cp;
        this.checkPeriod = checkPeriod;
        this.stateKeeper = stateKeeper;
    }

    @Override
    public void start() {
        log.info("{}starting state tracker", logPrefix);
        schedulePeriodicCheck();
        cp.subscribe(handleCheck, cp.getNegative(Timer.class).getPair());
    }

    @Override
    public Identifier registerPort(final Port<? extends PortType> port) {
        Identifier portId = UUIDIdentifier.randomId();
        if (port instanceof Negative) {
            for (final Class<? extends KompicsEvent> eventType : PortRegistry.getPositive(port.getPortType().getClass())) {
                Handler handleEvent = new Handler(eventType) {
                    @Override
                    public void handle(KompicsEvent event) {
                        countEvent(port.getPortType().getClass(), eventType);
                    }
                };
                cp.subscribe(handleEvent, port);
                handlers.put(portId, handleEvent);
            }
        } else {
            for (final Class<? extends KompicsEvent> eventType : PortRegistry.getNegative(port.getPortType().getClass())) {
                Handler handleEvent = new Handler(eventType) {
                    @Override
                    public void handle(KompicsEvent event) {
                        countEvent(port.getPortType().getClass(), eventType);
                    }
                };
                cp.subscribe(handleEvent, port);
                handlers.put(portId, handleEvent);
            }
        }
            return portId;
        }

    

    private void countEvent(Class<? extends PortType> portClass, Class<? extends KompicsEvent> eventClass) {
        Integer counter = currentEvents.get(eventClass, portClass);
        if (counter == null) {
            counter = 0;
        }
        currentEvents.put(eventClass, portClass, counter + 1);
    }

    Handler handleCheck = new Handler<PeriodicCheck>() {
        @Override
        public void handle(PeriodicCheck event) {
            log.info("{}periodic check", logPrefix);
            for (Cell<Class<? extends KompicsEvent>, Class<? extends PortType>, Integer> eventCounter : currentEvents.cellSet()) {
                log.info("{}port:{} event:{} - nr:{}", new Object[]{logPrefix, eventCounter.getColumnKey(),
                    eventCounter.getRowKey(), eventCounter.getValue()});
            }
            currentEvents.clear();
            stateKeeper.reportState();
        }
    };

    private void schedulePeriodicCheck() {
        log.info("{}starting periodic check every:{}ms", logPrefix, checkPeriod);
        if (checkTid != null) {
            log.warn("{}double starting state check", logPrefix);
        }
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(checkPeriod, checkPeriod);
        PeriodicCheck sc = new PeriodicCheck(spt);
        spt.setTimeoutEvent(sc);
        checkTid = sc.getTimeoutId();
        cp.trigger(spt, cp.getNegative(Timer.class).getPair());
    }

    private void cancelPeriodicCheck() {
        if (checkTid == null) {
            return;
        }
        CancelTimeout cpt = new CancelTimeout(checkTid);
        checkTid = null;
        cp.trigger(cpt, cp.getNegative(Timer.class).getPair());
    }

    private static class PeriodicCheck extends Timeout implements Identifiable {

        private PeriodicCheck(SchedulePeriodicTimeout request) {
            super(request);
        }

        @Override
        public Identifier getId() {
            return new UUIDIdentifier(getTimeoutId());
        }

        @Override
        public String toString() {
            return "STATE_CHECK<" + getTimeoutId() + ">";
        }
    }
}

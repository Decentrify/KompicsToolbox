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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.javatuples.Pair;
import org.slf4j.Logger;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Handler;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Port;
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

    public final Set<Class> listeningPorts = new HashSet<>();
    public final Map<Class, Integer> currentEvents = new HashMap<>();
    public final StateTrackedComp stateKeeper;

    public StateTrackerImpl(ComponentProxy cp, Pair<Logger, String> log, long checkPeriod, StateTrackedComp stateKeeper) {
        this.log = log.getValue0();
        this.logPrefix = log.getValue1();
        this.cp = cp;
        this.checkPeriod = checkPeriod;
        this.stateKeeper = stateKeeper;
    }

    public void start() {
        schedulePeriodicCheck();
    }

    public void registerPort(Port port) {
        cp.subscribe(handleEvent, port);
    }

    Handler handleEvent = new Handler<KompicsEvent>() {
        @Override
        public void handle(KompicsEvent event) {
            Integer counter = currentEvents.get(event.getClass());
            if (counter == null) {
                counter = 0;
            }
            currentEvents.put(event.getClass(), counter + 1);
        }
    };

    Handler handleCheck = new Handler<PeriodicCheck>() {
        @Override
        public void handle(PeriodicCheck event) {
            log.info("{}periodic check", logPrefix);
            for (Map.Entry<Class, Integer> eventCounter : currentEvents.entrySet()) {
                log.info("{}event:{} - nr:{}", new Object[]{logPrefix, eventCounter.getKey(), eventCounter.getValue()});
            }
            currentEvents.clear();
            stateKeeper.reportState();
        }
    };

    private void schedulePeriodicCheck() {
        if (checkTid != null) {
            log.warn("{}double starting state check", logPrefix);
        }
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(checkPeriod, checkPeriod);
        PeriodicCheck sc = new PeriodicCheck(spt);
        spt.setTimeoutEvent(sc);
        checkTid = sc.getTimeoutId();
        cp.trigger(spt, cp.getPositive(Timer.class));
    }

    private void cancelPeriodicCheck() {
        if (checkTid == null) {
            return;
        }
        CancelTimeout cpt = new CancelTimeout(checkTid);
        checkTid = null;
        cp.trigger(cpt, cp.getPositive(Timer.class));
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

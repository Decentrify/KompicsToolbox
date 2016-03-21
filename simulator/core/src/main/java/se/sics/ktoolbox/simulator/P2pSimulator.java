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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Negative;
import se.sics.kompics.Start;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.simulator.events.ChangeNetworkModelEvent;
import se.sics.ktoolbox.simulator.events.TerminateExperiment;
import se.sics.ktoolbox.simulator.events.stochastic.StochasticKompicsSimulatorEvent;
import se.sics.ktoolbox.simulator.events.stochastic.StochasticPeriodicSimulatorEvent;
import se.sics.ktoolbox.simulator.events.stochastic.StochasticSimulationTerminatedEvent;
import se.sics.ktoolbox.simulator.events.stochastic.StochasticSimulatorEvent;
import se.sics.ktoolbox.simulator.events.stochastic.StochasticProcessEvent;
import se.sics.ktoolbox.simulator.events.stochastic.StochasticProcessStartEvent;
import se.sics.ktoolbox.simulator.events.stochastic.StochasticProcessTerminatedEvent;
import se.sics.ktoolbox.simulator.events.stochastic.StochasticTakeSnapshotEvent;
import se.sics.ktoolbox.simulator.instrumentation.SimulatorSystem;
import se.sics.ktoolbox.simulator.netmodel.NetworkModel;
import se.sics.ktoolbox.simulator.scheduler.SimulationScheduler;

/**
 * The <code>P2pSimulator</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public final class P2pSimulator extends ComponentDefinition implements Simulator, SimulatorComp {

    private static final Logger LOG = LoggerFactory.getLogger(P2pSimulator.class);
    private String logPrefix = "";

    Negative simPort = provides(SimulatorPort.class);
    Negative simControlPort = provides(SimulatorControlPort.class);
    Negative network = provides(Network.class);
    Negative timer = provides(Timer.class);

    private final SimulationScheduler scheduler;
    private final SimulationScenario scenario;

    private NetworkModel networkModel;

    private long CLOCK;
    private Random random;
    private FutureEventList futureEventList;

    // set of active timers
    private final HashMap<UUID, StochasticKompicsSimulatorEvent> activeTimers = new HashMap<>();
    // set of active periodic timers
    private final HashMap<UUID, StochasticPeriodicSimulatorEvent> activePeriodicTimers = new HashMap<>();
    // time statistics
    private long simulationStartTime = 0;

    public P2pSimulator(P2pSimulatorInit init) {
        LOG.info("{}initiating...", logPrefix);

        scheduler = init.scheduler;
        scheduler.setSimulator(this);
        scenario = init.scenario;
        networkModel = init.networkModel;
        random = SimulationScenario.getRandom();
        
         //simulator system
        SimulatorSystem.setSimulator(this);

        futureEventList = new FutureEventList();
        CLOCK = 0;
        simulationStartTime = System.currentTimeMillis();

        subscribe(handleStart, control);
        subscribe(handleMsg, network);
        subscribe(handleST, timer);
        subscribe(handleSPT, timer);
        subscribe(handleCT, timer);
        subscribe(handleCPT, timer);
        subscribe(handleTerminate, simControlPort);
    }

//    public Pair<Long, Boolean> advanceSimulation(long milis) {
//        boolean executingNewEvent = false;
//        long nextEventTime = futureEventList.getFirstEventTime();
//        if (nextEventTime > milis) {
//            CLOCK = milis;
//            return Pair.with(nextEventTime, executingNewEvent);
//        }
//        if (nextEventTime == -1) {
//            executingNewEvent = false;
//        } else {
//            executingNewEvent = true;
//        }
//        advanceSimulation();
//        nextEventTime = futureEventList.getFirstEventTime();
//        return Pair.with(futureEventList.getFirstEventTime(), executingNewEvent);
//    }
    @Override
    public boolean advanceSimulation() {
        StochasticSimulatorEvent event = futureEventList.getAndRemoveFirstEvent(CLOCK);
        if (event == null) {
            LOG.error("Simulator ran out of events.");
            logTimeStatistics();
            return false;
        }

        long time = event.getTime();

        if (time < CLOCK) {
            throw new RuntimeException("Future event has past timestamp."
                    + " CLOCK=" + CLOCK + " event=" + time + event);
        }
        CLOCK = time;

        // execute this event
        boolean ok = executeEvent(event);
        if (!ok) {
            return false;
        }
        // execute all events scheduled to occur at the same time
        while (futureEventList.hasMoreEventsAtTime(CLOCK)) {
            event = futureEventList.getAndRemoveFirstEvent(CLOCK);
            ok = executeEvent(event);
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private String pName(StochasticSimulatorEvent event) {
        if (event instanceof StochasticProcessEvent) {
            return ((StochasticProcessEvent) event).getProcessName();
        } else if (event instanceof StochasticProcessStartEvent) {
            return ((StochasticProcessStartEvent) event).getProcessName();
        } else if (event instanceof StochasticProcessTerminatedEvent) {
            return ((StochasticProcessTerminatedEvent) event).getProcessName();
        }
        return "";
    }

    private boolean executeEvent(StochasticSimulatorEvent event) {
        if (event instanceof StochasticProcessEvent) {
            executeStochasticProcessEvent((StochasticProcessEvent) event);
        } else if (event instanceof StochasticProcessStartEvent) {
            executeStochasticProcessStartEvent((StochasticProcessStartEvent) event);
        } else if (event instanceof StochasticProcessTerminatedEvent) {
            executeStochasticProcessTerminatedEvent((StochasticProcessTerminatedEvent) event);
        } else if (event instanceof StochasticPeriodicSimulatorEvent) {
            executePeriodicSimulatorEvent((StochasticPeriodicSimulatorEvent) event);
        } else if (event instanceof StochasticKompicsSimulatorEvent) {
            StochasticKompicsSimulatorEvent kse = (StochasticKompicsSimulatorEvent) event;
            if (!kse.canceled()) {
                executeKompicsEvent(kse.getEvent());
            }
        } else if (event instanceof StochasticTakeSnapshotEvent) {
            executeTakeSnapshotEvent((StochasticTakeSnapshotEvent) event);
        } else if (event instanceof StochasticSimulationTerminatedEvent) {
            return executeSimultationTerminationEvent((StochasticSimulationTerminatedEvent) event);
        }
        return true;
    }

    private void executeStochasticProcessStartEvent(StochasticProcessStartEvent event) {
        if (event.shouldHandleNow()) {
            LOG.debug("{}Started:{}", logPrefix, pName(event));
            // trigger start events relative to this one
            LinkedList<StochasticProcessStartEvent> startEvents = event.getStartEvents();
            for (StochasticProcessStartEvent startEvent : startEvents) {
                startEvent.setTime(CLOCK);
                futureEventList.scheduleFutureEvent(CLOCK, startEvent);
            }
            // get the stochastic process running
            StochasticProcessEvent stochasticEvent = event.getStochasticEvent();
            stochasticEvent.setTime(CLOCK);
            futureEventList.scheduleFutureEvent(CLOCK, stochasticEvent);
        }
    }

    private void executeStochasticProcessTerminatedEvent(StochasticProcessTerminatedEvent event) {
        LOG.debug("{}Terminated process:{}", logPrefix, pName(event));
        // trigger start events relative to this process termination
        LinkedList<StochasticProcessStartEvent> startEvents = event.getStartEvents();
        for (StochasticProcessStartEvent startEvent : startEvents) {
            startEvent.setTime(CLOCK);
            futureEventList.scheduleFutureEvent(CLOCK, startEvent);
        }

        // trigger snapshot relative to this process termination
        StochasticTakeSnapshotEvent snapshotEvent = event.getSnapshotEvent();
        if (snapshotEvent != null) {
            if (snapshotEvent.isOnList()) {
                boolean removed = futureEventList.cancelFutureEvent(CLOCK, snapshotEvent);
                if (!removed) {
                    throw new RuntimeException("Event should have been scheduled:" + snapshotEvent);
                }
                snapshotEvent.shouldHandleNow();
            }
            snapshotEvent.setTime(CLOCK);
            futureEventList.scheduleFutureEvent(CLOCK, snapshotEvent);
        }

        // trigger simulation termination relative to this process termination
        StochasticSimulationTerminatedEvent terminationEvent = event.getTerminationEvent();
        if (terminationEvent != null) {
            if (terminationEvent.isOnList()) {
                boolean removed = futureEventList.cancelFutureEvent(CLOCK, terminationEvent);
                if (!removed) {
                    throw new RuntimeException("Event should have been scheduled:" + terminationEvent);
                }
                terminationEvent.shouldTerminateNow();
            }
            terminationEvent.setTime(CLOCK);
            futureEventList.scheduleFutureEvent(CLOCK, terminationEvent);
        }
    }

    private void executeStochasticProcessEvent(StochasticProcessEvent event) {
        KompicsEvent e = event.generateOperation(random);

        if (e instanceof ChangeNetworkModelEvent) {
            ChangeNetworkModelEvent networkEvent = (ChangeNetworkModelEvent) e;
            LOG.debug("{}changing network parameters acording to:{}", logPrefix, networkEvent.netModel);
            networkModel = networkEvent.netModel;
        } else {
            LOG.trace("{}sending:{}-{}", new Object[]{logPrefix, pName(event), e});
            trigger(e, simPort);
        }

        if (event.getCurrentCount() > 0) {
            // still have operations to generate, reschedule
            event.setNextTime();
            futureEventList.scheduleFutureEvent(CLOCK, event);
        } else {
            // no operations left. stochastic process terminated
            StochasticProcessTerminatedEvent t = event.getTerminatedEvent();
            t.setTime(CLOCK);
            futureEventList.scheduleFutureEvent(CLOCK, t);
        }
    }

    private void executeKompicsEvent(KompicsEvent kompicsEvent) {
        // trigger Messages on the Network port
        if (Msg.class.isAssignableFrom(kompicsEvent.getClass())) {
            Msg message = (Msg) kompicsEvent;
            LOG.trace("{}delivered network msg:{} from:{} to:{}", new Object[]{logPrefix,
                message, message.getHeader().getSource(), message.getHeader().getDestination()});
            trigger(kompicsEvent, network);
            return;
        }
        // trigger Timeouts on the Timer port
        if (Timeout.class.isAssignableFrom(kompicsEvent.getClass())) {
            Timeout timeout = (Timeout) kompicsEvent;
            LOG.trace("{}trigger timeout:{}<{}>", new Object[]{logPrefix, kompicsEvent.getClass(), timeout.getTimeoutId()});
            activeTimers.remove(timeout.getTimeoutId());
            trigger(kompicsEvent, timer);
            return;
        }

        // trigger other Kompics events on the simulation port
        LOG.trace("{}other:{}", new Object[]{logPrefix, kompicsEvent});
        trigger(kompicsEvent, simPort);
    }

    private void executePeriodicSimulatorEvent(StochasticPeriodicSimulatorEvent periodic) {
        // reschedule periodic event
        periodic.setTime(CLOCK + periodic.getPeriod());

        // clone timeouts
        if (Timeout.class.isAssignableFrom(periodic.getEvent().getClass())) {
            Timeout timeout = (Timeout) periodic.getEvent();
            try {
                periodic.setEvent((Timeout) timeout.clone());
            } catch (CloneNotSupportedException ex) {
                LOG.error("{}timeout is not clonable - kompics internal error", logPrefix);
                System.exit(1);
            }

            LOG.debug("{}triggered [periodic] timeout:{}<{}>", new Object[]{logPrefix,
                timeout.getClass().getName(), timeout.getTimeoutId()});
            trigger(timeout, timer);
        }
        futureEventList.scheduleFutureEvent(CLOCK, periodic);
    }

    private void executeTakeSnapshotEvent(StochasticTakeSnapshotEvent event) {
        if (event.shouldHandleNow()) {
            trigger(event.getTakeSnapshotEvent(), simPort);
        }
    }

    private boolean executeSimultationTerminationEvent(
            StochasticSimulationTerminatedEvent event) {
        if (event.shouldTerminateNow()) {
            try {
                trigger(new TerminateExperiment(), simControlPort);
            } catch (Exception e) {
                LOG.warn("{}could not trigger TerminateExperiment on the SimulationPort", logPrefix);
            }

            LOG.info("{}simulation terminated", logPrefix);
            logTimeStatistics();
            return false;
        }
        return true;
    }
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            // generate initial future events from the scenario
            LinkedList<StochasticSimulatorEvent> events = scenario.generateEventList();
            for (StochasticSimulatorEvent simulatorEvent : events) {
                futureEventList.scheduleFutureEvent(CLOCK, simulatorEvent);
            }
            LOG.info("{}simulation started", logPrefix);
        }
    };
    Handler handleMsg = new Handler<Msg>() {

        @Override
        public void handle(Msg event) {
            random.nextInt();
            LOG.debug("{}sending msg:{} from:{} to:{}", new Object[]{logPrefix,
                event, event.getHeader().getSource(), event.getHeader().getDestination()});

            if (networkModel != null) {
                long latency = networkModel.getLatencyMs(event);
                if (latency == -1) {
                    //drop message
                    return;
                }
                futureEventList.scheduleFutureEvent(CLOCK,
                        new StochasticKompicsSimulatorEvent(event, CLOCK + latency));
            } else {
                // we just echo the message on the network port
                futureEventList.scheduleFutureEvent(CLOCK,
                        new StochasticKompicsSimulatorEvent(event, CLOCK + 0));
            }
        }
    };
    Handler handleST = new Handler<ScheduleTimeout>() {

        @Override
        public void handle(ScheduleTimeout event) {
            LOG.debug("{}scheduleTimeout@{} : {} {} AT={}", new Object[]{logPrefix, event.getDelay(),
                event.getTimeoutEvent(), event.getTimeoutEvent().getTimeoutId(), activeTimers.keySet()});

            if (event.getDelay() < 0) {
                throw new RuntimeException("Cannot set a negative timeout value.");
            }
            if (event.getTimeoutEvent() == null) {
                throw new IllegalStateException("Timeout event was null for:" + event.getClass().getCanonicalName());
            }

            StochasticKompicsSimulatorEvent timeout = new StochasticKompicsSimulatorEvent(
                    event.getTimeoutEvent(), CLOCK + event.getDelay());
            activeTimers.put(event.getTimeoutEvent().getTimeoutId(), timeout);
            futureEventList.scheduleFutureEvent(CLOCK, timeout);
        }
    };
    Handler handleSPT = new Handler<SchedulePeriodicTimeout>() {

        @Override
        public void handle(SchedulePeriodicTimeout event) {
            LOG.debug("{}schedulePeriodicTimeout@{} : {}", new Object[]{logPrefix, event.getPeriod(),
                    event.getTimeoutEvent()});

            if (event.getDelay() < 0 || event.getPeriod() < 0) {
                throw new RuntimeException("Cannot set a negative timeout value.");
            }

            StochasticPeriodicSimulatorEvent periodicTimeout = new StochasticPeriodicSimulatorEvent(
                    event.getTimeoutEvent(), CLOCK + event.getDelay(), event.getPeriod());
            activePeriodicTimers.put(event.getTimeoutEvent().getTimeoutId(),
                    periodicTimeout);
            futureEventList.scheduleFutureEvent(CLOCK, periodicTimeout);
        }
    };
    Handler handleCT = new Handler<CancelTimeout>() {

        @Override
        public void handle(CancelTimeout event) {
            UUID timeoutId = event.getTimeoutId();
            LOG.debug("{}cancelTimeout: {}. AT={}", new Object[]{logPrefix, timeoutId, activeTimers.keySet()});

            StochasticKompicsSimulatorEvent kse = activeTimers.remove(timeoutId);

            if (kse != null) {
                kse.cancel();
            } else {
                // CancelTimeout comes after expiration or previous cancelation 
                LOG.warn("{}cannot find timeout:{}", logPrefix, event.getTimeoutId());
            }
        }
    };
    Handler handleCPT = new Handler<CancelPeriodicTimeout>() {

        @Override
        public void handle(CancelPeriodicTimeout event) {
            UUID timeoutId = event.getTimeoutId();
            LOG.debug("{}cancelPeriodicTimeout: {}. APT={}", new Object[]{logPrefix, timeoutId,
                    activePeriodicTimers.keySet()});

            StochasticKompicsSimulatorEvent kse = activePeriodicTimers.remove(timeoutId);
            boolean removed = futureEventList.cancelFutureEvent(CLOCK, kse);
            if (!removed) {
                LOG.warn("{}cannot find periodic timeout:{}", logPrefix, event.getTimeoutId());
            }
        }
    };
    Handler handleTerminate = new Handler<TerminateExperiment>() {

        @Override
        public void handle(TerminateExperiment event) {
            StochasticSimulationTerminatedEvent terminatedEvent = new StochasticSimulationTerminatedEvent(
                    CLOCK, 0, false);
            futureEventList.scheduleFutureEvent(CLOCK, terminatedEvent);
        }
    };

    // === intercepted calls related to time
    @Override
    public long java_lang_System_currentTimeMillis() { // System
        return CLOCK;
    }

    @Override
    public long java_lang_System_nanoTime() { // System
        return CLOCK * 1000000;
    }

    @Override
    public void java_lang_Thread_sleep(long millis) { // Thread
        // TODO
        throw new RuntimeException("I cannot simulate sleep without a continuation.");
    }

    @Override
    public void java_lang_Thread_sleep(long millis, int nanos) { // Thread
        if (nanos != 0) {
            throw new RuntimeException("I can't sleep nanos.");
        }
        java_lang_Thread_sleep(millis);
    }

    @Override
    public void java_lang_Thread_start() { // Thread
        throw new RuntimeException("You cannot start threads in reproducible simulation mode.");
    }

    // statistics
    private final void logTimeStatistics() {
        long realDuration = System.currentTimeMillis() - simulationStartTime;
        LOG.info("========================================================");
        LOG.info("{}Simulated time: {}", logPrefix, durationToString(CLOCK));
        LOG.info("{}Real time: {}", logPrefix, durationToString(realDuration));
        if (CLOCK > realDuration) {
            LOG.info("{}Time compression factor:{}", logPrefix,
                    ((double) CLOCK / realDuration));
        } else {
            LOG.info("{}Time expansion factor::{}", logPrefix, 
                    ((double) realDuration / CLOCK));
        }
        LOG.info("========================================================");
    }

    public static final String durationToString(long duration) {
        StringBuilder sb = new StringBuilder();
        int ms = 0, s = 0, m = 0, h = 0, d = 0, y = 0;

        ms = (int) (duration % 1000);
        // get duration in seconds
        duration /= 1000;
        s = (int) (duration % 60);
        // get duration in minutes
        duration /= 60;
        if (duration > 0) {
            m = (int) (duration % 60);
            // get duration in hours
            duration /= 60;
            if (duration > 0) {
                h = (int) (duration % 24);
                // get duration in days
                duration /= 24;
                if (duration > 0) {
                    d = (int) (duration % 365);
                    // get duration in years
                    y = (int) (duration / 365);
                }
            }
        }
        boolean printed = false;
        if (y > 0) {
            sb.append(y).append("y ");
            printed = true;
        }
        if (d > 0) {
            sb.append(d).append("d ");
            printed = true;
        }
        if (h > 0) {
            sb.append(h).append("h ");
            printed = true;
        }
        if (m > 0) {
            sb.append(m).append("m ");
            printed = true;
        }
        if (s > 0 || !printed) {
            sb.append(s);
            if (ms > 0) {
                sb.append(".").append(String.format("%03d", ms));
            }
            sb.append("s");
        }
        return sb.toString();
    }

    public static final class P2pSimulatorInit extends Init<P2pSimulator> {

        public final SimulationScheduler scheduler;
        public final SimulationScenario scenario;
        public final NetworkModel networkModel;

        public P2pSimulatorInit(SimulationScheduler scheduler, SimulationScenario scenario, NetworkModel networkModel) {
            super();
            this.scheduler = scheduler;
            this.scenario = scenario;
            this.networkModel = networkModel;
        }
    }
}

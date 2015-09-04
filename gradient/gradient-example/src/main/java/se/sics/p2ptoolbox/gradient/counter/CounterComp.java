/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Croupier is free software; you can redistribute it and/or
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
package se.sics.p2ptoolbox.gradient.counter;

import java.util.Random;
import java.util.UUID;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Address;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.gradient.GradientPort;
import se.sics.p2ptoolbox.gradient.msg.GradientSample;
import se.sics.p2ptoolbox.gradient.msg.GradientUpdate;
import se.sics.p2ptoolbox.util.update.SelfViewUpdatePort;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CounterComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(CounterComp.class);

    private Positive gradient = requires(GradientPort.class);
    private Positive timer = requires(Timer.class);
    private Negative viewUpdate = provides(SelfViewUpdatePort.class);

    private final Address selfAddress;
    private final Random rand;
    private int counter;
    private int counterTicks;
    private final int period;
    private final Pair<Double, Integer> rate;
    private final String logPrefix;

    private UUID counterCycleId;

    public CounterComp(CounterInit init) {
        this.selfAddress = init.selfAddress;
        this.logPrefix = "id:" + selfAddress;
        log.info("{} initiating...", logPrefix);
        this.rand = init.rand;
        this.counter = 0;
        this.counterTicks = init.counterAction.getValue1();
        this.period = init.counterAction.getValue0();
        this.rate = init.counterRate;

        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleGradientSample, gradient);
        subscribe(handlePeriodicAction, timer);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            log.info("{} starting...", logPrefix);
            trigger(new GradientUpdate(new CounterView(counter)), viewUpdate);
            schedulePeriodicCounter();
        }
    };

    Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            log.info("{} stopping...", logPrefix);
        }
    };

    Handler handleGradientSample = new Handler<GradientSample>() {

        @Override
        public void handle(GradientSample sample) {
            log.info("{} counter:{} gradient:{}",
                    new Object[]{logPrefix, counter, sample.gradientSample});
        }
    };

    Handler handlePeriodicAction = new Handler<CounterCycle>() {

        @Override
        public void handle(CounterCycle event) {
            counterTicks--;
            if (counterTicks == 0) {
                cancelPeriodicCounter();
            }
            if (rand.nextDouble() > rate.getValue0()) {
                counter = counter + rate.getValue1();
                trigger(new GradientUpdate(new CounterView(counter)), viewUpdate);
            }
        }

    };

    private void schedulePeriodicCounter() {
        if (counterCycleId != null) {
            log.warn("{} double starting periodic counter", logPrefix);
            return;
        }
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(period, period);
        CounterCycle sc = new CounterCycle(spt);
        spt.setTimeoutEvent(sc);
        counterCycleId = sc.getTimeoutId();
        trigger(spt, timer);
    }

    private void cancelPeriodicCounter() {
        if (counterCycleId == null) {
            log.warn("{} double stopping periodic counter", logPrefix);
            return;
        }
        CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(counterCycleId);
        counterCycleId = null;
        trigger(cpt, timer);
    }

    public static class CounterInit extends Init<CounterComp> {

        public final Address selfAddress;
        public final Random rand;
        public final Pair<Integer, Integer> counterAction;
        public final Pair<Double, Integer> counterRate;

        public CounterInit(Address selfAddress, long seed, Pair<Integer, Integer> counterAction, Pair<Double, Integer> counterRate) {
            this.selfAddress = selfAddress;
            this.rand = new Random(seed);
            this.counterAction = counterAction;
            this.counterRate = counterRate;
        }
    }

    public class CounterCycle extends Timeout {

        public CounterCycle(SchedulePeriodicTimeout request) {
            super(request);
        }

        @Override
        public String toString() {
            return "COUNTER_CYCLE";
        }
    }
}

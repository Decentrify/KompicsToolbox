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
package se.sics.p2ptoolbox.simulator.timedexample.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.simulator.timed.api.Timed;
import se.sics.p2ptoolbox.simulator.timed.api.TimedControler;
import se.sics.p2ptoolbox.simulator.timed.api.TimedControlerBuilder;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class MyComponent extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(MyComponent.class);

    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);

    private TimedControler tc;
    private DecoratedAddress self;
    private Random rand;
    
    private List<DecoratedAddress> bootstrapNodes;

    public MyComponent(MyInit init) {
        this.self = init.self;
        log.debug("initiating test node:{}", init.self);

        tc = init.tcb.registerComponent(self.getId(), this);
        this.rand = new Random(self.getId());
        this.bootstrapNodes = new ArrayList<DecoratedAddress>(init.bootstrapNodes);

        subscribe(handleStart, control);
        subscribe(handleNetPing, network);
        subscribe(handleNetPong, network);
        subscribe(handleTimeout, timer);
    }

    private Handler<Start> handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            tc.advance(MyComponent.this, rand.nextInt(10));
            log.debug("starting test node:{}", self);
            schedulePeriodicShuffle();
        }
    };

    private Handler<MyNetMsg.NetPing> handleNetPing = new Handler<MyNetMsg.NetPing>() {

        @Override
        public void handle(MyNetMsg.NetPing ping) {
            tc.advance(MyComponent.this, rand.nextInt(10));
            log.debug("{} received net ping from {}", self, ping.getHeader().getSource());
            if (bootstrapNodes.size() > 0) {
                trigger(new MyNetMsg.NetPong(self, bootstrapNodes.get(0), ping.getContent().id), network);
            }
            log.info("sending status msgs");
        }
    };

    private Handler<MyNetMsg.NetPong> handleNetPong = new Handler<MyNetMsg.NetPong>() {

        @Override
        public void handle(MyNetMsg.NetPong event) {
            tc.advance(MyComponent.this, rand.nextInt(10));
            log.debug("{} received net pong from {}", self, event.getHeader().getSource());
        }
    };

    private Handler handleTimeout = new Handler<StatusTimeout>() {

        @Override
        public void handle(StatusTimeout timeout) {
            tc.advance(MyComponent.this, rand.nextInt(10));
            log.debug("{} time:{}", self, System.currentTimeMillis());
        }
    };

    private void schedulePeriodicShuffle() {
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(1000, 1000);
        StatusTimeout sc = new StatusTimeout(spt);
        spt.setTimeoutEvent(sc);
        trigger(spt, timer);
    }

    public static class MyInit extends Init<MyComponent> implements Timed {

        public TimedControlerBuilder tcb;
        public final DecoratedAddress self;
        public final Set<DecoratedAddress> bootstrapNodes;

        public MyInit(DecoratedAddress self, Set<DecoratedAddress> bootstrapNodes) {
            this.self = self;
            this.bootstrapNodes = bootstrapNodes;
        }

        @Override
        public void set(TimedControlerBuilder tcb) {
            this.tcb = tcb;
        }
    }

    public static class StatusTimeout extends Timeout {

        public StatusTimeout(SchedulePeriodicTimeout spt) {
            super(spt);
        }
    }
}
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
package se.sics.ktoolbox.simulator.example.core;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.util.address.basic.BasicAddress;
import se.sics.ktoolbox.util.msg.BasicContentMsg;
import se.sics.ktoolbox.util.msg.BasicHeader;
import se.sics.ktoolbox.util.msg.DecoratedHeader;
import se.sics.ktoolbox.simulator.common.Ping;
import se.sics.ktoolbox.simulator.common.Pong;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class MyComponent extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(MyComponent.class);
    private String logPrefix;

    private final Positive network = requires(Network.class);
    private final Positive timer = requires(Timer.class);

    private BasicAddress self;
    private BasicAddress statusServer;
    private BasicAddress ping;

    private UUID statusTid;

    public MyComponent(MyInit init) {
        self = init.self;
        logPrefix = "<nid:" + self.getId() + "> ";
        ping = init.ping;
        LOG.info("{}initiating...", logPrefix);

        subscribe(handleStart, control);
        subscribe(handleStatus, timer);
        subscribe(handlePing, network);
        subscribe(handlePong, network);
    }

    private Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            scheduleStatus();
        }
    };

    private void send(Object content, BasicAddress target) {
        DecoratedHeader<BasicAddress> header = new DecoratedHeader(new BasicHeader(self, target, Transport.UDP), null, null);
        BasicContentMsg msg = new BasicContentMsg(header, content);
        LOG.info("{}sending:{} from:{} to:{}", new Object[]{logPrefix, msg.getContent(), msg.getSource(), msg.getDestination()});
        trigger(msg, network);
    }
    private Handler handleStatus = new Handler<StatusTimeout>() {

        @Override
        public void handle(StatusTimeout timeout) {
            if (ping == null) {
                cancelStatus();
                return;
            }
            send(new Ping(UUID.randomUUID()), ping);
        }
    };

    ClassMatchedHandler handlePing
            = new ClassMatchedHandler<Ping, BasicContentMsg<BasicAddress, DecoratedHeader<BasicAddress>, Ping>>() {

                @Override
                public void handle(Ping content, BasicContentMsg<BasicAddress, DecoratedHeader<BasicAddress>, Ping> container) {
                    LOG.info("{}received:{} from:{}", new Object[]{logPrefix, content, container.getSource()});
                    send(new Pong(content.id), container.getSource());
                }
            };

    ClassMatchedHandler handlePong
            = new ClassMatchedHandler<Pong, BasicContentMsg<BasicAddress, DecoratedHeader<BasicAddress>, Pong>>() {

                @Override
                public void handle(Pong content, BasicContentMsg<BasicAddress, DecoratedHeader<BasicAddress>, Pong> container) {
                    LOG.info("{}received:{} from:{}", new Object[]{logPrefix, content, container.getSource()});
                    ping = null;
                    cancelStatus();
                }
            };

    public static class MyInit extends Init<MyComponent> {

        public final BasicAddress self;
        public final BasicAddress ping;

        public MyInit(BasicAddress self, BasicAddress ping) {
            this.self = self;
            this.ping = ping;
        }
    }

    private void scheduleStatus() {
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(1000, 1000);
        StatusTimeout status = new StatusTimeout(spt);
        spt.setTimeoutEvent(status);
        trigger(spt, timer);
        statusTid = status.getTimeoutId();
    }

    private void cancelStatus() {
        trigger(new CancelPeriodicTimeout(statusTid), timer);
        statusTid = null;
    }

    public static class StatusTimeout extends Timeout {

        public StatusTimeout(SchedulePeriodicTimeout spt) {
            super(spt);
        }
    }
}

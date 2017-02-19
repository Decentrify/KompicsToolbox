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
package se.sics.ledbat.system;

import com.google.common.base.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdFactory;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;
import se.sics.ledbat.core.AppCongestionWindow;
import se.sics.ledbat.core.LedbatConfig;
import se.sics.ledbat.ncore.msg.LedbatMsg;
import se.sics.nutil.tracking.load.NetworkQueueLoadProxy;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class Leecher extends ComponentDefinition {

    private final static Logger LOG = LoggerFactory.getLogger(Leecher.class);
    private String logPrefix = "";
    private static final long MIN_RTO = 100;

    //**************************************************************************
    Positive<Network> networkPort = requires(Network.class);
    Positive<Timer> timerPort = requires(Timer.class);
    //**************************************************************************
    private final KAddress self;
    private final KAddress seeder;
    private final AppCongestionWindow conn;
    private final NetworkQueueLoadProxy networkLoad;
    private final Set<UUID> ongoing = new HashSet<UUID>();
    //**************************************************************************
    private UUID advanceTId;
    private LedbatConfig ledbatConfig;

    public Leecher(Init init) {
        self = init.self;
        seeder = init.seeder;
        ledbatConfig = new LedbatConfig(config());
        logPrefix = "<" + self.getId().toString() +  ">";
        UUIDIdFactory uuidFactory = new UUIDIdFactory();
        this.conn = new AppCongestionWindow(ledbatConfig, uuidFactory.randomId(), MIN_RTO, Optional.fromNullable((String)null));
        this.networkLoad = NetworkQueueLoadProxy.instance("load_leecher" + logPrefix, proxy, config(), Optional.fromNullable((String)null));

        subscribe(handleStart, control);
        subscribe(handleAdvance, timerPort);
        subscribe(handleMsgTimeout, timerPort);
        subscribe(handleMsgResponse, networkPort);
    }

    Handler handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            networkLoad.start();
            trySend();

            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(5000, 5000);
            AdvanceTimeout timeout = new AdvanceTimeout(spt);
            spt.setTimeoutEvent(timeout);
            trigger(spt, timerPort);
            advanceTId = timeout.getTimeoutId();
        }
    };

    Handler handleAdvance = new Handler<AdvanceTimeout>() {
        @Override
        public void handle(AdvanceTimeout event) {
            trySend();
        }
    };

    Handler handleMsgTimeout = new Handler<ExMsg.Timeout>() {
        @Override
        public void handle(ExMsg.Timeout event) {
            LOG.debug("{}timeout", logPrefix);
            if (ongoing.remove(event.getTimeoutId())) {
                conn.timeout(System.currentTimeMillis(), ledbatConfig.mss);
            }
        }
    };

    ClassMatchedHandler handleMsgResponse
            = new ClassMatchedHandler<LedbatMsg.Response<ExMsg.Response>, KContentMsg<KAddress, KHeader<KAddress>, LedbatMsg.Response<ExMsg.Response>>>() {

                @Override
                public void handle(LedbatMsg.Response<ExMsg.Response> content, KContentMsg<KAddress, KHeader<KAddress>, LedbatMsg.Response<ExMsg.Response>> context) {
                    LOG.trace("{}received resp", logPrefix);
                    if (ongoing.remove(content.getWrappedContent().eventId)) {
                        cancelTimeout(content.getWrappedContent().eventId);
                        conn.success(System.currentTimeMillis(), ledbatConfig.mss, content);
                        trySend();
                    } else {
                        conn.late(System.currentTimeMillis(), ledbatConfig.mss, content);
                        trySend();
                    }
                }
            };

    private void trySend() {
        long now = System.currentTimeMillis();
        conn.adjustState(networkLoad.adjustment());
        while (conn.canSend()) {
            conn.request(now, ledbatConfig.mss);
            request();
        }
    }

    private void request() {
        ScheduleTimeout st = new ScheduleTimeout(conn.getRTT());
        ExMsg.Timeout timeout = new ExMsg.Timeout(st);
        st.setTimeoutEvent(timeout);
        trigger(st, timerPort);

        ExMsg.Request req = new ExMsg.Request(timeout.getTimeoutId());
        KHeader header = new BasicHeader(self, seeder, Transport.UDP);
        KContentMsg msg = new BasicContentMsg(header, new LedbatMsg.Request(req));
        trigger(msg, networkPort);

        ongoing.add(req.eventId);
    }

    private void cancelTimeout(UUID timeoutId) {
        CancelTimeout ct = new CancelTimeout(timeoutId);
        trigger(ct, timerPort);
    }

    public static class Init extends se.sics.kompics.Init<Leecher> {

        public final KAddress self;
        public final KAddress seeder;

        public Init(KAddress self, KAddress seeder) {
            this.self = self;
            this.seeder = seeder;
        }
    }

    public static class AdvanceTimeout extends Timeout {

        public AdvanceTimeout(SchedulePeriodicTimeout spt) {
            super(spt);
        }
    }
};

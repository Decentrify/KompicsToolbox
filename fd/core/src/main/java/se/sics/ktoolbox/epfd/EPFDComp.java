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
package se.sics.ktoolbox.epfd;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.util.Identifier;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.epfd.event.EPFDFollow;
import se.sics.ktoolbox.epfd.event.EPFDIndication;
import se.sics.ktoolbox.epfd.event.EPFDUnfollow;
import se.sics.ktoolbox.epfd.msg.EPFDPing;
import se.sics.ktoolbox.epfd.msg.EPFDPong;
import se.sics.ktoolbox.epfd.util.HostProber;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class EPFDComp extends ComponentDefinition implements EPFDService {

    private static final Logger LOG = LoggerFactory.getLogger(EPFDComp.class);
    private String logPrefix;

    private final Negative<EPFDPort> epfd = provides(EPFDPort.class);
    private final Positive<Network> network = requires(Network.class);
    private final Positive<Timer> timer = requires(Timer.class);

    private final SystemKCWrapper systemConfig;
    private final EPFDKCWrapper epfdConfig;
    private KAddress selfAdr;

    private final HashMap<Identifier, HostProber> hostProbers = new HashMap<>();
    private final HashSet<UUID> outstandingTimeouts = new HashSet<>();
    
    private UUID periodicStateCheckTid;

    public EPFDComp(EPFDInit init) {
        systemConfig = new SystemKCWrapper(config());
        epfdConfig = new EPFDKCWrapper(config());
        selfAdr = init.selfAdr;
        logPrefix = "<nid:" + systemConfig.id + ">: ";
        LOG.info("{}initiating...", logPrefix);

        subscribe(handleStart, control);
        subscribe(handleStateCheck, timer);
        subscribe(handleFollow, epfd);
        subscribe(handleUnfollow, epfd);
        subscribe(handleNextPingTimeout, timer);
        subscribe(handlePongTimeout, timer);
        subscribe(handlePing, network);
        subscribe(handlePong, network);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            schedulePeriodicStateCheck();
        }
    };
    
    Handler handleStateCheck = new Handler<PeriodicStateCheck>() {
        @Override
        public void handle(PeriodicStateCheck event) {
            int followers = 0;
            for(HostProber hostProber : hostProbers.values()) {
                followers += hostProber.followers();
            }
            LOG.info("{}hosts probed:{} with a total of:{} followers", 
                    new Object[]{logPrefix, hostProbers.size(), followers});
        }
    };

    //**************************************************************************
    Handler handleFollow = new Handler<EPFDFollow>() {
        @Override
        public void handle(EPFDFollow request) {
            KAddress hostAddress = request.target;
            HostProber hostProber = hostProbers.get(hostAddress.getId());
            if (hostProber == null) {
                hostProber = new HostProber(EPFDComp.this, hostAddress, epfdConfig.minRto);
                hostProbers.put(hostAddress.getId(), hostProber);
                hostProber.start();
                LOG.debug("{}started probing host:{}", logPrefix, hostAddress.getId());
            }
            hostProber.addRequest(request);
            LOG.trace("{}new follower:{} for host:{}",
                    new Object[]{logPrefix, request.followerId, hostAddress.getId()});
        }
    };

    Handler handleUnfollow = new Handler<EPFDUnfollow>() {
        @Override
        public void handle(EPFDUnfollow update) {
            KAddress hostAddress = update.req.target;
            HostProber prober = hostProbers.get(hostAddress.getId());
            if (prober != null) {
                Identifier requestId = update.req.id;
                if (prober.hasRequest(requestId)) {
                    boolean last = prober.removeRequest(requestId);
                    if (last) {
                        hostProbers.remove(hostAddress.getId());
                        prober.stop();
                        LOG.debug("{}stopped probing host:{}", logPrefix, hostAddress.getId());
                    }
                } else {
                    LOG.warn("{}no request of id:{} for the probing of host:{}",
                            new Object[]{logPrefix, requestId, hostAddress.getId()});
                }
            } else {
                LOG.debug("{}host:{} is not currently being probed(STOP)", logPrefix, hostAddress.getId());
            }
        }
    };

    Handler handleNextPingTimeout = new Handler<NextPingTimeout>() {
        @Override
        public void handle(NextPingTimeout timeout) {
            if (outstandingTimeouts.remove(timeout.getTimeoutId())) {
                HostProber prober = hostProbers.get(timeout.target.getId());
                if (prober != null) {
                    prober.ping();
                } else {
                    LOG.debug("{}host:{} is not currently being probed (SEND_PING)",
                            logPrefix, timeout.target.getId());
                }
            }
        }
    };

    Handler handlePongTimeout = new Handler<PongTimeout>() {
        @Override
        public void handle(PongTimeout timeout) {
            if (outstandingTimeouts.remove(timeout.getTimeoutId())) {
                KAddress host = timeout.target;
                HostProber hostProber = hostProbers.get(host.getId());
                if (hostProber != null) {
                    LOG.debug("{}host:{} SUSPECTED due to timeout:{}",
                            new Object[]{logPrefix, host.getId(), timeout.getTimeoutId()});
                    hostProber.pongTimeout();
                } else {
                    LOG.debug("{}host:{} is not currently being probed (TIMEOUT)", logPrefix, host.getId());
                }
            }
        }
    };

    ClassMatchedHandler handlePing
            = new ClassMatchedHandler<EPFDPing, BasicContentMsg<KAddress, BasicHeader<KAddress>, EPFDPing>>() {

                @Override
                public void handle(EPFDPing content, BasicContentMsg<KAddress, BasicHeader<KAddress>, EPFDPing> container) {
                    LOG.trace("{}received:{} from:{}", new Object[]{logPrefix, content, container.getSource().getId()});
                    send(content.pong(), container.getSource());
                }
            };

    ClassMatchedHandler handlePong
            = new ClassMatchedHandler<EPFDPong, BasicContentMsg<KAddress, BasicHeader<KAddress>, EPFDPong>>() {

                @Override
                public void handle(EPFDPong content, BasicContentMsg<KAddress, BasicHeader<KAddress>, EPFDPong> container) {
                    LOG.trace("{}received:{} from:{}", new Object[]{logPrefix, content, container.getSource().getId()});
                    cancelPongTimeout(content.ping.timeoutId);
                    HostProber hostProber = hostProbers.get(container.getSource().getId());
                    if (hostProber != null) {
                        hostProber.pong(content.ping.msgId, content.ping.ts);
                    } else {
                        LOG.debug("{}host:{} is not currently being probed (GOT_PONG)", 
                                logPrefix, container.getSource().getId());
                    }
                }
            };

    private void send(Object content, KAddress dst) {
        BasicHeader<KAddress> header = new BasicHeader(selfAdr, dst, Transport.UDP);
        BasicContentMsg msg = new BasicContentMsg(header, content);
        LOG.trace("{}sending:{} to:{}", new Object[]{logPrefix, content, dst.getId()});
        trigger(msg, network);
    }
    //********************************EPFD**************************************
    @Override
    public void answerRequest(EPFDFollow request, EPFDIndication indication) {
        answer(request, indication);
    }

    @Override
    public UUID nextPing(boolean suspected, KAddress probedHost) {
        long timeout = suspected ? epfdConfig.deadPingInterval : epfdConfig.livePingInterval;
        return scheduleNextPingTimeout(timeout, probedHost);
    }

    @Override
    public UUID ping(long ts, KAddress probedHost, long expectedRtt) {
        long timeout = expectedRtt + epfdConfig.pingTimeoutIncrement;
        UUID pingTid = schedulePongTimeout(timeout, probedHost);
        send(new EPFDPing(pingTid, ts), probedHost);
        return pingTid;
    }

    @Override
    public void stop(UUID nextPingTid, UUID pongTid) {
        cancelNextPingTimeout(nextPingTid);
        cancelPongTimeout(pongTid);
    }

    public static class EPFDInit extends Init<EPFDComp> {
        public final KAddress selfAdr;

        public EPFDInit(KAddress selfAdr) {
            this.selfAdr = selfAdr;
        }
    }

    private UUID scheduleNextPingTimeout(long timeout, KAddress target) {
        ScheduleTimeout st = new ScheduleTimeout(timeout);
        NextPingTimeout pt = new NextPingTimeout(st, target);
        st.setTimeoutEvent(pt);
        trigger(pt, timer);
        outstandingTimeouts.add(pt.getTimeoutId());
        return pt.getTimeoutId();
    }

    private void cancelNextPingTimeout(UUID pongTid) {
        if (outstandingTimeouts.remove(pongTid)) {
            CancelTimeout ct = new CancelTimeout(pongTid);
            trigger(ct, timer);
        }
    }

    public static class NextPingTimeout extends Timeout {

        public final KAddress target;

        public NextPingTimeout(ScheduleTimeout st, KAddress target) {
            super(st);
            this.target = target;
        }

        @Override
        public String toString() {
            return "PING_TIMEOUT<" + getTimeoutId() + ">";
        }
    }

    private UUID schedulePongTimeout(long timeout, KAddress target) {
        ScheduleTimeout st = new ScheduleTimeout(timeout);
        PongTimeout pt = new PongTimeout(st, target);
        st.setTimeoutEvent(pt);
        trigger(pt, timer);
        outstandingTimeouts.add(pt.getTimeoutId());
        return pt.getTimeoutId();
    }

    private void cancelPongTimeout(UUID nextPingTid) {
        if (outstandingTimeouts.remove(nextPingTid)) {
            CancelTimeout ct = new CancelTimeout(nextPingTid);
            trigger(ct, timer);
        }
    }

    public static class PongTimeout extends Timeout {

        public final KAddress target;

        public PongTimeout(ScheduleTimeout st, KAddress target) {
            super(st);
            this.target = target;
        }

        @Override
        public String toString() {
            return "PONG_TIMEOUT<" + getTimeoutId() + ">";
        }
    }
    
      private void schedulePeriodicStateCheck() {
        if (periodicStateCheckTid != null) {
            LOG.warn("{}double starting internal state check timeout", logPrefix);
            return;
        }
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(epfdConfig.stateCheckPeriod, epfdConfig.stateCheckPeriod);
        PeriodicStateCheck sc = new PeriodicStateCheck(spt);
        spt.setTimeoutEvent(sc);
        periodicStateCheckTid = sc.getTimeoutId();
        trigger(spt, timer);
    }

    private void cancelPeriodicStateCheck() {
        if (periodicStateCheckTid == null) {
            LOG.warn("{}double stopping internal state check timeout", logPrefix);
            return;
        }
        CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(periodicStateCheckTid);
        periodicStateCheckTid = null;
        trigger(cpt, timer);
    }

    public static class PeriodicStateCheck extends Timeout {

        public PeriodicStateCheck(SchedulePeriodicTimeout spt) {
            super(spt);
        }
    }
}

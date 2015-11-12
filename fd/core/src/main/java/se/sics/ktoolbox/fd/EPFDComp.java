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
package se.sics.ktoolbox.fd;

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
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.fd.event.EPFDEvent;
import se.sics.ktoolbox.fd.event.EPFDFollow;
import se.sics.ktoolbox.fd.event.EPFDIndication;
import se.sics.ktoolbox.fd.event.EPFDRestore;
import se.sics.ktoolbox.fd.event.EPFDSuspect;
import se.sics.ktoolbox.fd.event.EPFDUnfollow;
import se.sics.ktoolbox.fd.msg.EPFDPing;
import se.sics.ktoolbox.fd.msg.EPFDPong;
import se.sics.ktoolbox.fd.util.HostProber;
import se.sics.p2ptoolbox.util.config.KConfigCore;
import se.sics.p2ptoolbox.util.config.impl.SystemKCWrapper;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.BasicHeader;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;
import se.sics.p2ptoolbox.util.update.SelfAddressUpdate;
import se.sics.p2ptoolbox.util.update.SelfAddressUpdatePort;
import static sun.rmi.transport.TransportConstants.Ping;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class EPFDComp extends ComponentDefinition implements EPFDService {

    private static final Logger LOG = LoggerFactory.getLogger(EPFDComp.class);
    private String logPrefix;

    private final Negative<EPFDPort> epfd = provides(EPFDPort.class);
    private final Positive<Network> network = requires(Network.class);
    private final Positive<Timer> timer = requires(Timer.class);
    private final Positive<SelfAddressUpdatePort> addressUpdate = requires(SelfAddressUpdatePort.class);

    private final SystemKCWrapper systemConfig;
    private final EPFDKCWrapper epfdConfig;
    private DecoratedAddress selfAdr;

    private final HashMap<BasicAddress, HostProber> hostProbers = new HashMap<>();
    private final HashSet<UUID> outstandingTimeouts = new HashSet<>();
    
    private UUID periodicStateCheckTid;

    public EPFDComp(EPFDInit init) {
        systemConfig = new SystemKCWrapper(init.configCore);
        epfdConfig = new EPFDKCWrapper(init.configCore);
        selfAdr = init.selfAdr;
        logPrefix = "<nid:" + systemConfig.id + ">: ";
        LOG.info("{}initiating...", logPrefix);

        subscribe(handleStart, control);
        subscribe(handleStateCheck, timer);
        subscribe(handleAddressUpdate, addressUpdate);
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

    Handler handleAddressUpdate = new Handler<SelfAddressUpdate>() {
        @Override
        public void handle(SelfAddressUpdate update) {
            LOG.info("{}update address:{}", logPrefix, update.self);
            selfAdr = update.self;
        }
    };
    //**************************************************************************
    Handler handleFollow = new Handler<EPFDFollow>() {
        @Override
        public void handle(EPFDFollow request) {
            DecoratedAddress hostAddress = request.target;
            HostProber hostProber = hostProbers.get(hostAddress.getBase());
            if (hostProber == null) {
                hostProber = new HostProber(EPFDComp.this, hostAddress, epfdConfig.minRto);
                hostProbers.put(hostAddress.getBase(), hostProber);
                hostProber.start();
                LOG.debug("{}started probing host:{}", logPrefix, hostAddress.getBase());
            }
            hostProber.addRequest(request);
            LOG.trace("{}new follower:{} for host:{}",
                    new Object[]{logPrefix, request.followerId, hostAddress.getBase()});
        }
    };

    Handler handleUnfollow = new Handler<EPFDUnfollow>() {
        @Override
        public void handle(EPFDUnfollow update) {
            DecoratedAddress hostAddress = update.req.target;
            HostProber prober = hostProbers.get(hostAddress.getBase());
            if (prober != null) {
                UUID requestId = update.req.id;
                if (prober.hasRequest(requestId)) {
                    boolean last = prober.removeRequest(requestId);
                    if (last) {
                        hostProbers.remove(hostAddress.getBase());
                        prober.stop();
                        LOG.debug("{}stopped probing host:{}", logPrefix, hostAddress.getBase());
                    }
                } else {
                    LOG.warn("{}no request of id:{} for the probing of host:{}",
                            new Object[]{logPrefix, requestId, hostAddress.getBase()});
                }
            } else {
                LOG.debug("{}host:{} is not currently being probed(STOP)", logPrefix, hostAddress.getBase());
            }
        }
    };

    Handler handleNextPingTimeout = new Handler<NextPingTimeout>() {
        @Override
        public void handle(NextPingTimeout timeout) {
            if (outstandingTimeouts.remove(timeout.getTimeoutId())) {
                HostProber prober = hostProbers.get(timeout.target.getBase());
                if (prober != null) {
                    prober.ping();
                } else {
                    LOG.debug("{}host:{} is not currently being probed (SEND_PING)",
                            logPrefix, timeout.target.getBase());
                }
            }
        }
    };

    Handler handlePongTimeout = new Handler<PongTimeout>() {
        @Override
        public void handle(PongTimeout timeout) {
            if (outstandingTimeouts.remove(timeout.getTimeoutId())) {
                DecoratedAddress host = timeout.target;
                HostProber hostProber = hostProbers.get(host.getBase());
                if (hostProber != null) {
                    LOG.debug("{}host:{} SUSPECTED due to timeout:{}",
                            new Object[]{logPrefix, host.getBase(), timeout.getTimeoutId()});
                    hostProber.pongTimeout();
                } else {
                    LOG.debug("{}host:{} is not currently being probed (TIMEOUT)", logPrefix, host.getBase());
                }
            }
        }
    };

    ClassMatchedHandler handlePing
            = new ClassMatchedHandler<EPFDPing, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, EPFDPing>>() {

                @Override
                public void handle(EPFDPing content, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, EPFDPing> container) {
                    LOG.trace("{}received:{} from:{}", new Object[]{logPrefix, content, container.getSource().getBase()});
                    send(content.pong(), container.getSource());
                }
            };

    ClassMatchedHandler handlePong
            = new ClassMatchedHandler<EPFDPong, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, EPFDPong>>() {

                @Override
                public void handle(EPFDPong content, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, EPFDPong> container) {
                    LOG.trace("{}received:{} from:{}", new Object[]{logPrefix, content, container.getSource().getBase()});
                    cancelPongTimeout(content.ping.id);
                    HostProber hostProber = hostProbers.get(container.getSource().getBase());
                    if (hostProber != null) {
                        hostProber.pong(content.ping.id, content.ping.ts);
                    } else {
                        LOG.debug("{}host:{} is not currently being probed (GOT_PONG)", 
                                logPrefix, container.getSource().getBase());
                    }
                }
            };

    private void send(Object content, DecoratedAddress dst) {
        DecoratedHeader<DecoratedAddress> header = new DecoratedHeader(
                new BasicHeader(selfAdr, dst, Transport.UDP), null, null);
        BasicContentMsg msg = new BasicContentMsg(header, content);
        LOG.trace("{}sending:{} to:{}", new Object[]{logPrefix, content, dst.getBase()});
        trigger(msg, network);
    }
    //********************************EPFD**************************************
    @Override
    public void answerRequest(EPFDFollow request, EPFDIndication indication) {
        answer(request, indication);
    }

    @Override
    public UUID nextPing(boolean suspected, DecoratedAddress probedHost) {
        long timeout = suspected ? epfdConfig.deadPingInterval : epfdConfig.livePingInterval;
        return scheduleNextPingTimeout(timeout, probedHost);
    }

    @Override
    public UUID ping(long ts, DecoratedAddress probedHost, long expectedRtt) {
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

        public final KConfigCore configCore;
        public final DecoratedAddress selfAdr;

        public EPFDInit(KConfigCore configCore, DecoratedAddress selfAdr) {
            this.selfAdr = selfAdr;
            this.configCore = configCore;
        }
    }

    private UUID scheduleNextPingTimeout(long timeout, DecoratedAddress target) {
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

    public static class NextPingTimeout extends Timeout implements EPFDEvent {

        public final DecoratedAddress target;

        public NextPingTimeout(ScheduleTimeout st, DecoratedAddress target) {
            super(st);
            this.target = target;
        }

        @Override
        public String toString() {
            return "PING_TIMEOUT<" + getTimeoutId() + ">";
        }
    }

    private UUID schedulePongTimeout(long timeout, DecoratedAddress target) {
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

    public static class PongTimeout extends Timeout implements EPFDEvent {

        public final DecoratedAddress target;

        public PongTimeout(ScheduleTimeout st, DecoratedAddress target) {
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

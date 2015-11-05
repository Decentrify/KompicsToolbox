///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * KompicsToolbox is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package se.sics.ktoolbox.fd;
//
//import java.nio.ByteBuffer;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Set;
//import java.util.UUID;
//import org.javatuples.Pair;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import se.sics.kompics.ClassMatchedHandler;
//import se.sics.kompics.ComponentDefinition;
//import se.sics.kompics.Handler;
//import se.sics.kompics.Init;
//import se.sics.kompics.Positive;
//import se.sics.kompics.Start;
//import se.sics.kompics.network.Network;
//import se.sics.kompics.timer.CancelPeriodicTimeout;
//import se.sics.kompics.timer.SchedulePeriodicTimeout;
//import se.sics.kompics.timer.Timeout;
//import se.sics.kompics.timer.Timer;
//import se.sics.ktoolbox.fd.event.Heartbeat;
//import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
//import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
//import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
//import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;
//
///**
// *
// * @author Alex Ormenisan <aaor@kth.se>
// */
//public class HeartbeatComp extends ComponentDefinition {
//
//    private final static Logger LOG = LoggerFactory.getLogger(HeartbeatComp.class);
//    private final String logPrefix;
//
//    private Positive<Network> network = requires(Network.class);
//    private Positive<Timer> timer = requires(Timer.class);
//
//    private final HeartbeatKCWrapper config;
//
//    private final Map<BasicAddress, Pair<DecoratedAddress, Set<ByteBuffer>>> heartbeatTo = new HashMap<>();
//    private final Set<BasicAddress> heartbeats = new HashSet<>(); //TODO Alex - check timeouts and rtts
//
//    private UUID internalStateCheckId;
//    private UUID heartbeatId;
//    private UUID heartbeatCheckId;
//
//    public HeartbeatComp(HeartbeatInit init) {
//        this.config = init.config;
//
//        subscribe(handleStart, control);
//
//        subscribe(handleHeartbeat, network);
//        subscribe(handleHeartbeatTimeout, timer);
//        subscribe(handleHeartbeatCheckTimeout, timer);
//    }
//
//    Handler handleStart = new Handler<Start>() {
//        @Override
//        public void handle(Start event) {
//            scheduleInternalStateCheck();
//            scheduleHeartbeat();
//            scheduleHeartbeatCheck();
//        }
//    };
//
//    Handler handleInternalStateCheck = new Handler<PeriodicInternalStateCheck>() {
//        @Override
//        public void handle(PeriodicInternalStateCheck event) {
//
//        }
//    };
//
//    ClassMatchedHandler handleHeartbeat
//            = new ClassMatchedHandler<Heartbeat, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, Heartbeat>>() {
//                @Override
//                public void handle(Heartbeat content, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, Heartbeat> container) {
//                    LOG.debug("{}heartbeat from:{}", logPrefix, container.getSource());
////                    heartbeats.add(container.getSource().getBase());
////                    if (children.containsKey(container.getSource().getBase())) {
////                        children.put(container.getSource().getBase(), container.getSource());
////                    }
//                }
//            };
//
//    Handler handleHeartbeatTimeout = new Handler<PeriodicHeartbeat>() {
//        @Override
//        public void handle(PeriodicHeartbeat event) {
//            LOG.debug("{}periodic heartbeat", logPrefix);
////            for (DecoratedAddress parent : children.values()) {
////                LOG.debug("{}heartbeating to child:{}", logPrefix, parent.getBase());
////                DecoratedHeader<DecoratedAddress> requestHeader = new DecoratedHeader(new BasicHeader(self, parent, Transport.UDP), null, null);
////                ContentMsg request = new BasicContentMsg(requestHeader, new PMMsg.Heartbeat());
////                trigger(request, network);
////            }
//        }
//    };
//
//    Handler handleHeartbeatCheckTimeout = new Handler<PeriodicHeartbeatCheck>() {
//        @Override
//        public void handle(PeriodicHeartbeatCheck event) {
//            LOG.debug("{}periodic heartbeat check", logPrefix);
////            Set<BasicAddress> suspected = Sets.difference(children.keySet(), heartbeats);
////            if (!suspected.isEmpty()) {
////                LOG.info("{}removing suspected children:{}", logPrefix, suspected);
////                for (BasicAddress suspectChild : suspected) {
////                    children.remove(suspectChild);
////                }
////                changed = true;
////            }
////            if (changed) {
////                trigger(new Update(new HashMap(children)), parentMaker);
////                changed = false;
////            }
//        }
//    };
//
//    public static class HeartbeatInit extends Init<HeartbeatComp> {
//
//        public final HeartbeatKCWrapper config;
//
//        public HeartbeatInit() {
//            this.config = new HeartbeatKCWrapper();
//        }
//    }
//
//    private void scheduleHeartbeatCheck() {
//        if (heartbeatCheckId != null) {
//            LOG.warn("{}double starting heartbeat check timeout", logPrefix);
//            return;
//        }
//        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(2 * config.heartbeatTimeout, 2 * config.heartbeatTimeout);
//        PeriodicHeartbeatCheck sc = new PeriodicHeartbeatCheck(spt);
//        spt.setTimeoutEvent(sc);
//        heartbeatCheckId = sc.getTimeoutId();
//        trigger(spt, timer);
//    }
//
//    private void cancelHeartbeatCheck() {
//        if (heartbeatCheckId == null) {
//            LOG.warn("{}double stopping heartbeat check timeout", logPrefix);
//            return;
//        }
//        CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(heartbeatCheckId);
//        heartbeatCheckId = null;
//        trigger(cpt, timer);
//
//    }
//
//    public static class PeriodicHeartbeatCheck extends Timeout {
//
//        public PeriodicHeartbeatCheck(SchedulePeriodicTimeout spt) {
//            super(spt);
//        }
//    }
//
//    private void scheduleHeartbeat() {
//        if (heartbeatId != null) {
//            LOG.warn("{}double starting heartbeat timeout", logPrefix);
//            return;
//        }
//        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(config.heartbeatTimeout, config.heartbeatTimeout);
//        PeriodicHeartbeat sc = new PeriodicHeartbeat(spt);
//        spt.setTimeoutEvent(sc);
//        heartbeatId = sc.getTimeoutId();
//        trigger(spt, timer);
//    }
//
//    private void cancelHeartbeat() {
//        if (heartbeatId == null) {
//            LOG.warn("{}double stopping heartbeat timeout", logPrefix);
//            return;
//        }
//        CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(heartbeatId);
//        heartbeatId = null;
//        trigger(cpt, timer);
//
//    }
//
//    public static class PeriodicHeartbeat extends Timeout {
//
//        public PeriodicHeartbeat(SchedulePeriodicTimeout spt) {
//            super(spt);
//        }
//    }
//
//    private void scheduleInternalStateCheck() {
//        if (internalStateCheckId != null) {
//            LOG.warn("{}double starting internal state check timeout", logPrefix);
//            return;
//        }
//        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(config.stateCheckTimeout, config.stateCheckTimeout);
//        PeriodicInternalStateCheck sc = new PeriodicInternalStateCheck(spt);
//        spt.setTimeoutEvent(sc);
//        internalStateCheckId = sc.getTimeoutId();
//        trigger(spt, timer);
//    }
//
//    private void cancelInternalStateCheck() {
//        if (internalStateCheckId == null) {
//            LOG.warn("{}double stopping internal state check timeout", logPrefix);
//            return;
//        }
//        CancelPeriodicTimeout cpt = new CancelPeriodicTimeout(internalStateCheckId);
//        internalStateCheckId = null;
//        trigger(cpt, timer);
//
//    }
//
//    private static class PeriodicInternalStateCheck extends Timeout {
//
//        public PeriodicInternalStateCheck(SchedulePeriodicTimeout spt) {
//            super(spt);
//        }
//    }
//}

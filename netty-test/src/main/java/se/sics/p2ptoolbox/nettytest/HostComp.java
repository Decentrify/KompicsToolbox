///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * GVoD is free software; you can redistribute it and/or
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
//package se.sics.p2ptoolbox.nettytest;
//
//import java.util.HashSet;
//import java.util.Set;
//import java.util.UUID;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import se.sics.gvod.net.VodAddress;
//import se.sics.gvod.net.VodNetwork;
//import se.sics.gvod.timer.Timer;
//import se.sics.kompics.ComponentDefinition;
//import se.sics.kompics.Handler;
//import se.sics.kompics.Init;
//import se.sics.kompics.Positive;
//import se.sics.kompics.Start;
//import se.sics.p2ptoolbox.nettytest.msg.MsgA;
//import se.sics.p2ptoolbox.nettytest.msg.MsgB;
//import se.sics.p2ptoolbox.nettytest.util.MsgProcessor;
//
///**
// * @author Alex Ormenisan <aaor@sics.se>
// */
//public class HostComp extends ComponentDefinition {
//
//    private static final Logger log = LoggerFactory.getLogger(HostComp.class);
//
//    private Positive<VodNetwork> network = requires(VodNetwork.class);
//    private Positive<Timer> timer = requires(Timer.class);
//
//    private final MsgProcessor msgProc;
//
//    private VodAddress selfAddress = null;
//    private VodAddress partnerAddress = null;
//
//    public HostComp(HostInit init) {
//        this.msgProc = new MsgProcessor();
//
//        selfAddress = init.selfAddress;
//        partnerAddress = init.partnerAddress;
//
//        // subscribe the handlers.
//        subscribe(startHandler, control);
//        subscribe(handleNetRequest, network);
//        subscribe(handleNetResponse, network);
//
//        msgProc.subscribe(handleMsgARequest);
//        msgProc.subscribe(handleMsgAResponse);
//        msgProc.subscribe(handleMsgBRequest);
//        msgProc.subscribe(handleMsgBResponse);
//    }
//
//    Handler<Start> startHandler = new Handler<Start>() {
//        @Override
//        public void handle(Start event) {
////            log.info("<{}> starting", selfAddress);
//
//            log.info("<{}> sending req", selfAddress);
//            MsgA.Request req = new MsgA.Request(UUID.randomUUID(), 1);
//            trigger(new MyNettyMsg.Request<MsgA.Request>(selfAddress, partnerAddress, req), network);
//        }
//    };
//
//    Handler<MyNettyMsg.Request> handleNetRequest = new Handler<MyNettyMsg.Request>() {
//        @Override
//        public void handle(MyNettyMsg.Request req) {
//            log.info("<{}> received {}", new Object[]{selfAddress, req, req.getVodSource()});
//
//            msgProc.trigger(req.getVodSource(), req.payload);
//        }
//    };
//
//    Handler<MyNettyMsg.Response> handleNetResponse = new Handler<MyNettyMsg.Response>() {
//        @Override
//        public void handle(MyNettyMsg.Response resp) {
//            log.info("<{}> received {}", new Object[]{selfAddress, resp, resp.getVodSource()});
//
//            msgProc.trigger(resp.getVodSource(), resp.payload);
//        }
//    };
//
//    MsgProcessor.Handler<MsgA.Request> handleMsgARequest = new MsgProcessor.Handler<MsgA.Request>(MsgA.Request.class) {
//        @Override
//        public void handle(VodAddress src, MsgA.Request req) {
//            log.info("<{}> processing {}", new Object[]{selfAddress, req});
//
//            Set<String> b = new HashSet<String>();
//            b.add("word1");
//            b.add("word2");
//            MsgA.Response resp = new MsgA.Response(UUID.randomUUID(), b);
//
//            log.info("<{}> sending {} to {}", new Object[]{selfAddress, resp, src});
//            trigger(new MyNettyMsg.Response (selfAddress, src,  resp), network);
//        }
//    };
//
//    MsgProcessor.Handler<MsgA.Response> handleMsgAResponse = new MsgProcessor.Handler<MsgA.Response>(MsgA.Response.class) {
//        @Override
//        public void handle(VodAddress src, MsgA.Response resp) {
//            log.info("<{}> processing {}", new Object[]{selfAddress, resp});
//
//            MsgB.Request req = new MsgB.Request(UUID.randomUUID(), 3);
//
//            log.info("<{}> sending {} to {}", new Object[]{selfAddress, req, src});
//            trigger(new MyNettyMsg.Request(selfAddress, src, req), network);
//        }
//    };
//
//    MsgProcessor.Handler<MsgB.Request> handleMsgBRequest = new MsgProcessor.Handler<MsgB.Request>(MsgB.Request.class) {
//        @Override
//        public void handle(VodAddress src, MsgB.Request req) {
//            log.info("<{}> processing {}", new Object[]{selfAddress, req});
//
//            Set<String> b = new HashSet<String>();
//            b.add("some1");
//            b.add("some2");
//            MsgB.Response resp = new MsgB.Response(UUID.randomUUID(), b);
//
//            log.info("<{}> sending {} to {}", new Object[]{selfAddress, resp, src});
//            trigger(new MyNettyMsg.Response(selfAddress, src, resp), network);
//        }
//    };
//
//    MsgProcessor.Handler<MsgB.Response> handleMsgBResponse = new MsgProcessor.Handler<MsgB.Response>(MsgB.Response.class) {
//        @Override
//        public void handle(VodAddress src, MsgB.Response req) {
//            log.info("<{}> processing {}", new Object[]{selfAddress, req});
//        }
//    };
//
//    public static class HostInit extends Init<HostComp> {
//
//        public VodAddress selfAddress;
//        public VodAddress partnerAddress;
//
//        public HostInit(VodAddress selfAddress, VodAddress partnerAddress) {
//            this.selfAddress = selfAddress;
//            this.partnerAddress = partnerAddress;
//        }
//    }
//}

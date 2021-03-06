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
//package se.sics.p2ptoolbox.nettytest.msg;
//
//import java.util.UUID;
//import se.sics.kompics.KompicsEvent;
//
///**
// * @author Alex Ormenisan <aaor@sics.se>
// */
//public class TestMsg {
//
//    public static abstract class Base implements KompicsEvent {
//
//        public final UUID id;
//
//        public Base(UUID id) {
//            this.id = id;
//        }
//    }
//
//    public static class Request extends Base {
//
//        public Request(UUID id) {
//            super(id);
//        }
//    }
//
//    public static class Response extends Base {
//
//        public Response(UUID id) {
//            super(id);
//        }
//    }
//
//    public static class Oneway extends Base {
//
//        public Oneway(UUID id) {
//            super(id);
//        }
//    }
//}

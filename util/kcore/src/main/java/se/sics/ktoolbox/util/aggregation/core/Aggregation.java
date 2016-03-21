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
//package se.sics.ktoolbox.util.aggregation.core;
//
//import se.sics.kompics.Direct;
//import se.sics.ktoolbox.util.identifiable.Identifiable;
//import se.sics.ktoolbox.util.identifiable.Identifier;
//import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;
//import se.sics.ktoolbox.util.other.Container;
//
///**
// * @author Alex Ormenisan <aaor@kth.se>
// */
//public class Aggregation {
//    public static class Request extends Direct.Request<Response> implements AggregationEvent {
//        
//    }
//
//    public static class Indication implements AggregationEvent, Container<Identifiable, Identifiable> {
//        public final Identifier eventId;
//        public final Identifiable source;
//        public final Identifiable content;
//
//        public Indication(Identifier eventId, Identifiable source, Identifiable content) {
//            this.eventId = eventId;
//            this.source = source;
//            this.content = content;
//        }
//
//        public Aggregation(Identifiable source, Identifiable content) {
//            this(UUIDIdentifier.randomId(), source, content);
//        }
//
//        @Override
//        public final Identifier getId() {
//            return eventId;
//        }
//
//        @Override
//        public Identifiable getSource() {
//            return source;
//        }
//
//        @Override
//        public Identifiable getContent() {
//            return content;
//        }
//
//        @Override
//        public Container<Identifiable, Identifiable> copy() {
//            return 
//        }
//    }
//}

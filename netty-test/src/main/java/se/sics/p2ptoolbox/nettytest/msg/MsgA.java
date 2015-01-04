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
//import java.util.Set;
//import java.util.UUID;
//
//import com.google.common.base.Objects;
//import se.sics.p2ptoolbox.serialization.api.Payload;
//
///**
// * @author Alex Ormenisan <aaor@sics.se>
// */
//public class MsgA {
//
//    public static class Request extends TestMsg.Request implements Payload {
//        public final int a;
//        
//        public Request(UUID id, int a) {
//            super(id);
//            this.a = a;
//        }
//        
//        @Override
//        public String toString() {
//            return "MsgA.Request<" + id + ">";
//        }
//        
//        @Override
//        public int hashCode() {
//            int hash = 7;
//            hash = 59 * hash + Objects.hashCode(this.id);
//            hash = 59 * hash + this.a;
//            return hash;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (obj == null) {
//                return false;
//            }
//            if (getClass() != obj.getClass()) {
//                return false;
//            }
//            final Request other = (Request) obj;
//            if (!Objects.equal(this.id, other.id)) {
//                return false;
//            }
//            if (this.a != other.a) {
//                return false;
//            }
//            return true;
//        }
//    }
//    
//    public static class Response extends TestMsg.Response implements Payload {
//        public final Set<String> b;
//        
//        public Response(UUID id, Set<String> b) {
//            super(id);
//            this.b = b;
//        }
//        
//        @Override
//        public String toString() {
//            return "MsgA.Response<" + id + ">";
//        }
//        
//        @Override
//        public int hashCode() {
//            int hash = 5;
//            hash = 37 * hash + Objects.hashCode(this.id);
//            hash = 37 * hash + Objects.hashCode(this.b);
//            return hash;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (obj == null) {
//                return false;
//            }
//            if (getClass() != obj.getClass()) {
//                return false;
//            }
//            final Response other = (Response) obj;
//            if (!Objects.equal(this.id, other.id)) {
//                return false;
//            }
//            if (!Objects.equal(this.b, other.b)) {
//                return false;
//            }
//            return true;
//        }
//    }
//}

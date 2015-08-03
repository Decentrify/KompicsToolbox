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

package se.sics.p2ptoolbox.caracalclient.common.op;

import se.sics.caracaldb.Key;
import se.sics.caracaldb.operations.CaracalOp;
import se.sics.kompics.Direct;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CCOpEvent {
    public static class Request extends Direct.Request {
        public final CaracalOp opReq;
        public final Key forwardTo;
        
        public Request(CaracalOp opReq, Key forwardTo) {
            this.opReq = opReq;
            this.forwardTo = forwardTo;
        }
        
        @Override
        public String toString() {
            return opReq.toString();
        }
    }
    
    public static class Response implements Direct.Response {
        public final CaracalOp opResp;
        
        public Response(CaracalOp opResp) {
            this.opResp = opResp;
        }
        
        @Override
        public String toString() {
            return opResp.toString();
        }
    }
    
    public static class Timeout implements Direct.Response {
        public final CaracalOp opReq;
        
        public Timeout(CaracalOp opReq) {
            this.opReq = opReq;
        }
        
        @Override
        public String toString() {
            return "Client_Timeout:" + opReq.toString();
        }
    }
}

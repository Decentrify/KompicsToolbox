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
package se.sics.ktoolbox.netmngr.nxnet;

import se.sics.kompics.Direct;
import se.sics.kompics.id.Identifier;
import se.sics.ktoolbox.netmngr.NetMngrEvent;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.network.KAddress;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NxNetBind {
    public static class Request extends Direct.Request<Response> implements NetMngrEvent {
        public final Identifier eventId;
        public final KAddress bindAdr;
        
        public Request(Identifier eventId, KAddress bindAdr) {
            this.eventId = eventId;
            this.bindAdr = bindAdr;
        }
        
        public Request(KAddress bindAdr) {
            this(BasicIdentifiers.eventId(), bindAdr);
        }

        @Override
        public Identifier getId() {
            return eventId;
        }
        
        @Override
        public String toString() {
            return "NxNetBindReq<" + eventId + ">";
        }
        
        public Response answer() {
            return new Response(this);
        }
    }
    
    public static class Response implements Direct.Response, NetMngrEvent {
        public final Request req;
        
        public Response(Request req) {
            this.req = req;
        }
        
        @Override
        public Identifier getId() {
            return req.getId();
        }
        
        @Override
        public String toString() {
            return "NxNetBindResp<" + req.getId() + ">";
        }
    }
}

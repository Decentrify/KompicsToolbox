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
package se.sics.ktoolbox.util.aggregation.core;

import java.util.Map;
import se.sics.kompics.Direct;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class AggregationMembers {
    public static class Request extends Direct.Request<Response> implements AggregationEvent  {
        public final Identifier eventId;
        
        public Request(Identifier eventId) {
            super();
            this.eventId = eventId;
        }
        
        @Override
        public Identifier getId() {
            return eventId;
        }
        
        @Override
        public String toString() {
            return "AggregationMembersRequest<" + getId() + ">";
        }
        
        public Response answer(Map<Identifier, String> members) {
            return new Response(this, members);
        }
    }
    
    public static class Response implements AggregationEvent, Direct.Response {
        public final Request req;
        public final Map<Identifier, String> members;
        
        public Response(Request req, Map<Identifier, String> members) {
            this.req = req;
            this.members = members;
        }

        @Override
        public Identifier getId() {
            return req.getId();
        }
        
        @Override
        public String toString() {
            return "AggregationMembersResponse<" + getId() + ">";
        }
    }
}

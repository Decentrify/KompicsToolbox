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
package se.sics.ktoolbox.util.stream.events;

import se.sics.kompics.Direct;
import se.sics.ktoolbox.util.identifiable.Identifiable;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;
import se.sics.ktoolbox.util.result.Result;
import se.sics.ktoolbox.util.stream.ranges.KBlock;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class StreamRead {
    public static class Request extends Direct.Request<Response> implements Identifiable {
        public final Identifier eventId;
        public final KBlock readRange;
        
        public Request(Identifier eventId, KBlock readRange) {
            this.eventId = eventId;
            this.readRange = readRange;
        }
        
        public Request(KBlock readRange) {
            this(UUIDIdentifier.randomId(), readRange);
        }
        
        @Override
        public Identifier getId() {
            return eventId;
        }
        
        public Response respond(Result<byte[]> result) {
            return new Response(this, result);
        }
    }
    
    public static class Response implements Direct.Response, Identifiable {
        public final Request req;
        public final Result<byte[]> result;
        
        public Response(Request req, Result<byte[]> result) {
            this.req = req;
            this.result = result;
        }
        
        @Override
        public Identifier getId() {
            return req.getId();
        }
    }
}

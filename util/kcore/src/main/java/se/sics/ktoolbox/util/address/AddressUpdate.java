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

package se.sics.ktoolbox.util.address;

import se.sics.kompics.Direct;
import se.sics.kompics.KompicsEvent;
import se.sics.ktoolbox.util.identifiable.Identifiable;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class AddressUpdate {
    public static class Request extends Direct.Request<Response> implements Identifiable {
        public final Identifier eventId;
        
        public Request(Identifier eventId) {
            super();
            this.eventId = eventId;
        }
        
        public Request() {
            this(UUIDIdentifier.randomId());
        }
        
        @Override
        public Identifier getId() {
            return eventId;
        }
        
        public Response answer(KAddress localAddress) {
            return new Response(eventId, localAddress);
        }
    }
    
    public static class Indication implements KompicsEvent, Identifiable {
        public final Identifier eventId;
        public final KAddress localAddress;
        
        public Indication(Identifier eventId, KAddress localAddress) {
            this.eventId = eventId;
            this.localAddress = localAddress;
        }
        
        public Indication(KAddress localAddress) {
            this(UUIDIdentifier.randomId(), localAddress);
        }

        @Override
        public Identifier getId() {
            return eventId;
        }
    }
    
    public static class Response extends Indication implements Direct.Response {
        public Response(Identifier id, KAddress localAddress) {
            super(id, localAddress);
        }
    }
}

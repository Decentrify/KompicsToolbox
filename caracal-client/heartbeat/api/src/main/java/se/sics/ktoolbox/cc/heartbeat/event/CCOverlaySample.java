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
package se.sics.ktoolbox.cc.heartbeat.event;

import java.util.Set;
import java.util.UUID;
import se.sics.kompics.Direct;
import se.sics.ktoolbox.cc.event.CCEvent;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CCOverlaySample {

    public static class Request extends Direct.Request<Response> implements CCEvent {
        public final UUID id;
        public final byte[] overlayId;
        
        public Request(UUID id, byte[] overlayId) {
            this.id = id;
            this.overlayId = overlayId;
        }
        
        public Response answer(Set<DecoratedAddress> overlaySample) {
            return new Response(this, overlaySample);
        }
    }

    public static class Response implements Direct.Response {
        public final Request req;
        public final Set<DecoratedAddress> overlaySample;
        
        public Response(Request req, Set<DecoratedAddress> overlaySample) {
            this.req = req;
            this.overlaySample = overlaySample;
        }
    }
}
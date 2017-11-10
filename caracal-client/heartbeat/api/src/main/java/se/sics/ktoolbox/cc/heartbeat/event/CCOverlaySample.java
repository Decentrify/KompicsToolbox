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

import java.util.List;
import se.sics.kompics.Direct;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.cc.event.CCEvent;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CCOverlaySample {

    public static class Request extends Direct.Request<Response> implements CCEvent {
        public final Identifier eventId;
        public final OverlayId overlayId;
        
        public Request(Identifier eventId, OverlayId overlayId) {
            this.eventId = eventId;
            this.overlayId = overlayId;
        }
        
        public Request(OverlayId overlayId) {
            this(BasicIdentifiers.eventId(), overlayId);
        }
        
        public Response answer(List<KAddress> overlaySample) {
            return new Response(this, overlaySample);
        }
        

        @Override
        public Identifier getId() {
            return eventId;
        }
    }

    public static class Response implements Direct.Response, CCEvent {
        public final Request req;
        public final List<KAddress> overlaySample;
        
        public Response(Request req, List<KAddress> overlaySample) {
            this.req = req;
            this.overlaySample = overlaySample;
        }

        @Override
        public Identifier getId() {
            return req.getId();
        }
    }
}

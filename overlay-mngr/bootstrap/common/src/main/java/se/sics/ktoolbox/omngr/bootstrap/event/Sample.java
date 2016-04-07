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
package se.sics.ktoolbox.omngr.bootstrap.event;

import java.util.List;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class Sample {

    public static class Request implements BootstrapEvent {

        public final Identifier eventId;
        public final Identifier overlayId;

        public Request(Identifier eventId, Identifier overlayId) {
            this.eventId = eventId;
            this.overlayId = overlayId;
        }

        public Request(Identifier overlayId) {
            this(UUIDIdentifier.randomId(), overlayId);
        }

        @Override
        public Identifier getId() {
            return eventId;
        }

        @Override
        public String toString() {
            return "Sample<" + overlayId + ">Request<" + eventId + ">";
        }
        
        public Response answer(List<KAddress> sample) {
            return new Response(eventId, overlayId, sample);
        }
    }

    public static class Response implements BootstrapEvent {
        public final Identifier eventId;
        public final Identifier overlayId;
        public final List<KAddress> sample;

        Response(Identifier eventId, Identifier overlayId, List<KAddress> sample) {
            this.eventId = eventId;
            this.overlayId = overlayId;
            this.sample = sample;
        }

        @Override
        public Identifier getId() {
            return eventId;
        }

        @Override
        public String toString() {
            return "Sample<" + overlayId + ">Response<" + eventId + ">";
        }
    }
}

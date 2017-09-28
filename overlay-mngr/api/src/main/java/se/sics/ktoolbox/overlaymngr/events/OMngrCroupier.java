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
package se.sics.ktoolbox.overlaymngr.events;

import se.sics.kompics.Direct;
import se.sics.kompics.id.Identifier;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OMngrCroupier {

    public static class ConnectRequest extends Direct.Request<ConnectResponse> implements OverlayMngrEvent {

        public final Identifier eventId;
        public final OverlayId croupierId;
        public final boolean observer;

        public ConnectRequest(Identifier eventId, OverlayId croupierId, boolean observer) {
            this.eventId = eventId;
            this.croupierId = croupierId;
            this.observer = observer;
        }
        
        public ConnectRequest(OverlayId croupierId, boolean observer) {
            this(BasicIdentifiers.eventId(), croupierId, observer);
        }
        
        public ConnectResponse answer() {
            return new ConnectResponse(this);
        }

        @Override
        public Identifier getId() {
            return eventId;
        }
        
        @Override
        public String toString() {
            return "OM_Croupier<" + croupierId + ">ConnectRequest<" + getId() + ">"; 
        }
    }

    public static class ConnectResponse implements Direct.Response, OverlayMngrEvent {

        public final ConnectRequest req;

        public ConnectResponse(ConnectRequest req) {
            this.req = req;
        }

        @Override
        public Identifier getId() {
            return req.getId();
        }
        
        @Override
        public String toString() {
            return "OM_Croupier<" + req.croupierId + ">ConnectResponse<" + getId() + ">"; 
        }
    }

    public static class Disconnect implements OverlayMngrEvent {

        public final Identifier eventId;
        public final OverlayId croupierId;

        public Disconnect(Identifier eventId, OverlayId croupierId) {
            this.eventId = eventId;
            this.croupierId = croupierId;
        }

        @Override
        public Identifier getId() {
            return eventId;
        }
        
        @Override
        public String toString() {
            return "OM_Croupier<" + croupierId + ">ConnectResponse<" + getId() + ">"; 
        }
    }
}

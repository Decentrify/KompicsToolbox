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

import java.util.Comparator;
import se.sics.kompics.Direct;
import se.sics.ktoolbox.gradient.GradientFilter;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OMngrTGradient {

    public static class ConnectRequest extends Direct.Request<ConnectResponse> implements OverlayMngrEvent {

        public final Identifier eventId;
        public final OverlayId tgradientId;
        public final Comparator utilityComparator;
        public final GradientFilter gradientFilter;

        public ConnectRequest(Identifier eventId, OverlayId tgradientId,
                Comparator utilityComparator, GradientFilter gradientFilter) {
            this.eventId = eventId;
            this.tgradientId = tgradientId;
            this.utilityComparator = utilityComparator;
            this.gradientFilter = gradientFilter;
        }
        
        public ConnectRequest(OverlayId tgradientId, Comparator utilityComparator, GradientFilter gradientFilter) {
            this(BasicIdentifiers.eventId(), tgradientId, utilityComparator, gradientFilter);
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
            return "OM_Gradient<" + tgradientId + ">ConnectRequest<" + getId() + ">"; 
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
            return "OM_Gradient<" + req.tgradientId + ">ConnectResponse<" + getId() + ">"; 
        }
    }

    public static class Disconnect implements OverlayMngrEvent {
        public final Identifier eventId;
        public final Identifier gradientId;

        public Disconnect(Identifier id, Identifier croupierId) {
            this.eventId = id;
            this.gradientId = croupierId;
        }

        @Override
        public Identifier getId() {
            return eventId;
        }
        
        @Override
        public String toString() {
            return "OM_Gradient<" + gradientId + ">Disconnect<" + getId() + ">"; 
        }
    }
}

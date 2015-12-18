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
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.ktoolbox.gradient.GradientFilter;
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.update.view.ViewUpdatePort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OMngrTGradient {

    public static class ConnectRequest extends Direct.Request<ConnectResponse> implements OverlayMngrEvent {

        public final Identifier id;
        public final Identifier parentId;
        public final Identifier croupierId;
        public final Identifier gradientId;
        public final Identifier tgradientId;
        public final Negative<GradientPort> tgradient;
        public final Positive<ViewUpdatePort> viewUpdate;
        public final Comparator utilityComparator;
        public final GradientFilter gradientFilter;

        public ConnectRequest(Identifier id, Identifier parentId, Identifier croupierId, Identifier gradientId, 
                Identifier tgradientId, Negative<GradientPort> tgradient, Positive<ViewUpdatePort> viewUpdate,
                Comparator utilityComparator, GradientFilter gradientFilter) {
            this.id = id;
            this.parentId = parentId;
            this.croupierId = croupierId;
            this.gradientId = gradientId;
            this.tgradientId = tgradientId;
            this.tgradient = tgradient;
            this.viewUpdate = viewUpdate;
            this.utilityComparator = utilityComparator;
            this.gradientFilter = gradientFilter;
        }

        public ConnectResponse answer() {
            return new ConnectResponse(this);
        }

        @Override
        public Identifier getId() {
            return id;
        }
    }

    public static class ConnectRequestBuilder {

        public final Identifier id;
        private Identifier parentId;
        private Identifier croupierId;
        private Identifier gradientId;
        private Identifier tgradientId;
        private Negative<GradientPort> tgradient;
        private Positive<ViewUpdatePort> viewUpdate;
        private Comparator utilityComparator;
        private GradientFilter gradientFilter;

        public ConnectRequestBuilder(Identifier id) {
            this.id = id;
        }

        public void setIdentifiers(Identifier parentId, Identifier croupierId, Identifier gradientId, Identifier tgradientId) {
            this.parentId = parentId;
            this.croupierId = croupierId;
            this.gradientId = gradientId;
            this.tgradientId = tgradientId;
        }

        public void connectTo(Negative<GradientPort> tgradient, Positive<ViewUpdatePort> viewUpdate) {
            this.tgradient = tgradient;
            this.viewUpdate = viewUpdate;
        }
        
        public void setupGradient(Comparator utilityComparator, GradientFilter gradientFilter) {
            this.utilityComparator = utilityComparator;
            this.gradientFilter = gradientFilter;
        }
        
        public ConnectRequest build() throws IllegalArgumentException {
            if(parentId == null || croupierId == null || gradientId == null || tgradientId == null) {
                throw new IllegalArgumentException("identifiers not set");
            }
            if(tgradient == null || viewUpdate == null) {
                throw new IllegalArgumentException("connection not set");
            }
            if(utilityComparator == null || gradientFilter == null) {
                throw new IllegalArgumentException("gradient not set");
            }
            return new ConnectRequest(id, parentId, croupierId, gradientId, tgradientId, tgradient, 
                    viewUpdate, utilityComparator, gradientFilter);
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
    }

    public static class Disconnect implements OverlayMngrEvent {
        public final Identifier id;
        public final Identifier croupierId;

        public Disconnect(Identifier id, Identifier croupierId) {
            this.id = id;
            this.croupierId = croupierId;
        }

        @Override
        public Identifier getId() {
            return id;
        }
    }
}

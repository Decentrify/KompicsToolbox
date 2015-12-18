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
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.ktoolbox.croupier.CroupierPort;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.update.view.ViewUpdatePort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OMngrCroupier {

    public static class ConnectRequest extends Direct.Request<ConnectResponse> implements OverlayMngrEvent {

        public final Identifier id;
        public final Identifier parentId;
        public final Identifier croupierId;
        public final Negative<CroupierPort> croupier;
        public final Positive<ViewUpdatePort> viewUpdate;
        public final boolean observer;

        public ConnectRequest(Identifier id, Identifier parentId, Identifier croupierId, 
                Negative<CroupierPort> croupier, Positive<ViewUpdatePort> viewUpdate, boolean observer) {
            this.id = id;
            this.parentId = parentId;
            this.croupierId = croupierId;
            this.croupier = croupier;
            this.viewUpdate = viewUpdate;
            this.observer = observer;
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
        private Negative<CroupierPort> croupier;
        private Positive<ViewUpdatePort> viewUpdate;
        private Boolean observer;

        public ConnectRequestBuilder(Identifier id) {
            this.id = id;
        }

        public void setIdentifiers(Identifier parentId, Identifier croupierId) {
            this.parentId = parentId;
            this.croupierId = croupierId;
        }

        public void connectTo(Negative<CroupierPort> croupier, Positive<ViewUpdatePort> viewUpdate) {
            this.croupier = croupier;
            this.viewUpdate = viewUpdate;
        }

        public void setupCroupier(boolean observer) {
            this.observer = observer;
        }

        public ConnectRequest build() throws IllegalArgumentException {
            if (parentId == null || croupierId == null) {
                throw new IllegalArgumentException("identifiers not set");
            }
            if (croupier == null || viewUpdate == null) {
                throw new IllegalArgumentException("connection not set");
            }
            if (observer == null) {
                throw new IllegalArgumentException("croupier not set");
            }
            return new ConnectRequest(id, parentId, croupierId, croupier, viewUpdate, observer);
        }
    }

    public static class ConnectResponse implements Direct.Response, OverlayMngrEvent {

        public final ConnectRequest req;

        public ConnectResponse(ConnectRequest req) {
            this.req = req;
        }

        @Override
        public Identifier getId() {
            return req.id;
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

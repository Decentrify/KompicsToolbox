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
package se.sics.ktoolbox.util.overlays.view;

import se.sics.kompics.Direct;
import se.sics.kompics.PatternExtractor;
import se.sics.kompics.id.Identifier;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.overlays.OverlayEvent;
import se.sics.ktoolbox.util.update.View;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OverlayViewUpdate {

    public static class Request extends Direct.Request<Response> implements OverlayEvent {
        public final Identifier eventId;
        public final OverlayId overlayId;
        
        public Request(Identifier eventId, OverlayId overlayId) {
            this.eventId = eventId;
            this.overlayId = overlayId;
        }

        public Response observer() {
            return new Response(eventId, overlayId, true, null);
        }

        public Response update(View view) {
            return new Response(eventId, overlayId, false, view);
        }

        @Override
        public OverlayId overlayId() {
            return overlayId;
        }

        @Override
        public Identifier getId() {
            return eventId;
        }
        
        @Override
        public String toString() {
            return "Overlay<" + overlayId() + ">ViewUpdate.Request<" + getId() + ">";
        }
    }

    public static class Indication<V extends View> implements OverlayEvent, PatternExtractor<Class<V>, V>  {
        public final Identifier eventId;
        public final OverlayId overlayId;
        public final boolean observer;
        public final V view;

        public Indication(Identifier eventId, OverlayId overlayId, boolean observer, V view) {
            this.eventId = eventId;
            this.overlayId = overlayId;
            this.observer = observer;
            this.view = view;
        }
        
        public Indication(OverlayId overlayId, boolean observer, V view) {
            this(BasicIdentifiers.eventId(), overlayId, observer, view);
        }

        @Override
        public Identifier getId() {
            return eventId;
        }
        
        @Override
        public OverlayId overlayId() {
            return overlayId;
        }

        @Override
        public Class<V> extractPattern() {
            return (Class<V>)view.getClass();
        }

        @Override
        public V extractValue() {
            return view;
        }
        
        public Indication changeOverlay(OverlayId overlayId) {
            return new Indication(eventId, overlayId, observer, view);
        }
        
        @Override
        public String toString() {
            return "Overlay<" + overlayId() + ">ViewUpdate.Indication<" + getId() + ">";
        }
    }
    
    public static class Response<V extends View> extends Indication<V> implements Direct.Response {
        public Response(Identifier eventId, OverlayId overlayId, boolean observer, V view) {
            super(eventId, overlayId, observer, view);
        }
        
        @Override
        public String toString() {
            return "Overlay<" + overlayId() + ">ViewUpdate.Response<" + getId() + ">";
        }
    }
}

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
package se.sics.ktoolbox.aggregator.server.event;

import java.util.UUID;
import se.sics.kompics.Direct;
import se.sics.kompics.PatternExtractor;
import se.sics.ktoolbox.aggregator.event.AggregatorEvent;
import se.sics.ktoolbox.aggregator.server.util.VisualizerPacket;

/**
 * Event indicating the processing of the windows being held by the visualizer.
 *
 * Created by babbar on 2015-09-04.
 */
public class VisualizerWindow {

    public static class Request extends Direct.Request<Response> implements AggregatorEvent {

        public final UUID id;
        public final Class processor;
        public final int startLoc;
        public final int endLoc;

        public Request(UUID id, Class processor, int startLoc, int endLoc) {

            this.id = id;
            this.processor = processor;
            this.startLoc = startLoc;
            this.endLoc = endLoc;
        }

        @Override
        public String toString() {
            return getClass() + "<" + id + ">";
        }

        @Override
        public UUID getId() {
            return id;
        }
        
        public <P extends VisualizerPacket> Response answer(P visualizationWindow) {
            return new Response(this, visualizationWindow);
        }
    }

    public static class Response<P extends VisualizerPacket>
            implements Direct.Response, AggregatorEvent, PatternExtractor<Class<P>, P> {

        public final Request req;
        public final P window;

        public Response(Request req, P window) {
            this.req = req;
            this.window = window;
        }

        @Override
        public Class<P> extractPattern() {
            return (Class<P>) this.window.getClass();
        }

        @Override
        public P extractValue() {
            return this.window;
        }

        @Override
        public UUID getId() {
            return req.getId();
        }
        
        @Override
        public String toString() {
            return getClass() + "<" + req.id + ">";
        }
    }
}

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
package se.sics.ledbat.system;

import java.util.UUID;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.overlays.OverlayEvent;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ExMsg {
    public static class Request implements OverlayEvent {
        public final UUID eventId;
        
        public Request(UUID eventId) {
            this.eventId = eventId;
        }
        
        public Response answer(byte[] payload) {
            return new Response(eventId, payload);
        }

        @Override
        public OverlayId overlayId() {
            return null;
        }

        @Override
        public Identifier getId() {
            return null;
        }
    }
    
    public static class Response implements OverlayEvent {
        public final UUID eventId;
        public final byte[] payload;
        
        protected Response(UUID eventId, byte[] payload) {
            this.eventId = eventId;
            this.payload = payload;
        }

        @Override
        public OverlayId overlayId() {
            return null;
        }

        @Override
        public Identifier getId() {
            return null;
        }
    }
    
    public static class Timeout extends se.sics.kompics.timer.Timeout {
        public Timeout(ScheduleTimeout st) {
            super(st);
        }
    }
}

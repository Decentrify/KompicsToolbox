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
package se.sics.ktoolbox.cc.mngr.event;

import se.sics.caracaldb.global.SchemaData;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CCMngrStatus {

    public static class Ready implements CCMngrEvent {

        public final Identifier eventId;
        public final SchemaData schemas;

        public Ready(Identifier eventId, SchemaData schemas) {
            this.eventId = eventId;
            this.schemas = schemas;
        }

        public Ready(SchemaData schemas) {
            this(BasicIdentifiers.eventId(), schemas);
        }

        @Override
        public Identifier getId() {
            return eventId;
        }

        @Override
        public String toString() {
            return "CCMngrReady<" + eventId + ">";
        }
    }

    public static class Disconnected implements CCMngrEvent {

        public final Identifier eventId;

        public Disconnected(Identifier eventId) {
            this.eventId = eventId;
        }

        public Disconnected() {
            this(BasicIdentifiers.eventId());
        }
        
        @Override
        public Identifier getId() {
            return eventId;
        }

        @Override
        public String toString() {
            return "CCMngrDisconnected<" + eventId + ">";
        }
    }
}

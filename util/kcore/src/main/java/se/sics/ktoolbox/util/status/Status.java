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
package se.sics.ktoolbox.util.status;

import se.sics.kompics.PatternExtractor;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.Identifiable;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class Status {

    public static class Internal<S extends Object> implements PatternExtractor<Class<Object>, S>, Identifiable {
        private final Identifier eventId; 
        public final S status;

        public Internal(Identifier eventId, S status) {
            this.eventId = eventId;
            this.status = status;
        }
        
        public Internal(S status) {
            this(BasicIdentifiers.eventId(), status);
        }

        @Override
        public Class<Object> extractPattern() {
            return (Class)status.getClass();
        }

        @Override
        public S extractValue() {
            return status;
        }

        @Override
        public Identifier getId() {
            return eventId;
        }
    }
    
    public static class External<S extends Object> implements PatternExtractor<Class<S>, S>, Identifiable {
        private final Identifier eventId;
        public final S status;

        public External(Identifier eventId, S status) {
            this.eventId = eventId;
            this.status = status;
        }
        
        public External(S status) {
            this(BasicIdentifiers.eventId(), status);
        }

        @Override
        public Class<S> extractPattern() {
            return (Class<S>)status.getClass();
        }

        @Override
        public S extractValue() {
            return status;
        }

        @Override
        public Identifier getId() {
            return eventId;
        }
    }
}

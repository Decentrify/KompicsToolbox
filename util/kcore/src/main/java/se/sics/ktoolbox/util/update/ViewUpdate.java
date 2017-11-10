/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
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

package se.sics.ktoolbox.util.update;

import se.sics.kompics.KompicsEvent;
import se.sics.kompics.PatternExtractor;
import se.sics.kompics.util.Identifiable;
import se.sics.kompics.util.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ViewUpdate {
    public static abstract class Request implements Identifiable {
        public final Identifier eventId;
        
        public Request(Identifier id) {
            this.eventId = id;
        }
        
        @Override
        public Identifier getId() {
            return eventId;
        }
    }
    
    public static abstract class Indication<V extends View> implements KompicsEvent, PatternExtractor<Class<V>, V>, Identifiable {
        public final Identifier id;
        public final V view;
        
        public Indication(Identifier id, V view) {
            this.id = id;
            this.view = view;
        }

        @Override
        public Class<V> extractPattern() {
            return (Class<V>)view.getClass();
        }

        @Override
        public V extractValue() {
            return view;
        }

        @Override
        public Identifier getId() {
            return id;
        }
    }
}

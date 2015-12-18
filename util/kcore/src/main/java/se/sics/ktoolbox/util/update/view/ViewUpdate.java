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

package se.sics.ktoolbox.util.update.view;

import se.sics.kompics.Direct;
import se.sics.kompics.PatternExtractor;
import se.sics.ktoolbox.util.identifiable.Identifiable;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ViewUpdate {
    public static abstract class Request extends Direct.Request<Indication> implements Identifiable {
        public final Identifier id;
        
        public Request(Identifier id) {
            this.id = id;
        }
        
        @Override
        public Identifier getId() {
            return id;
        }
    }
    
    public static abstract class Indication<V extends View> implements Direct.Response, PatternExtractor<Class<V>, V>, Identifiable {
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

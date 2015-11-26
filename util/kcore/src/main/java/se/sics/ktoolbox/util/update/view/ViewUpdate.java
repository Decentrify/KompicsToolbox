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

import java.util.UUID;
import se.sics.kompics.Direct;
import se.sics.kompics.PatternExtractor;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ViewUpdate {
    public static class Request extends Direct.Request<Indication> {
        public final UUID id;
        
        public Request(UUID id) {
            this.id = id;
        }
        
        public Indication answer(View view) {
            return new Indication(id, view);
        }
    }
    
    public static class Indication<V extends View> implements Direct.Response, PatternExtractor<Class<V>, V> {
        public final UUID id;
        public final V view;
        
        public Indication(UUID id, V view) {
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
    }
}
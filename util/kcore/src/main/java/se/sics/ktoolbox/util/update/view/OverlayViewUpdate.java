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
package se.sics.ktoolbox.util.update.view;

import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OverlayViewUpdate {

    public static class Request extends ViewUpdate.Request {

        public Request(Identifier id) {
            super(id);
        }

        public Indication observer() {
            return new Indication(id, true, null);
        }

        public Indication update(View view) {
            return new Indication(id, false, view);
        }
    }

    public static class Indication<V extends View> extends ViewUpdate.Indication<V> {

        public final boolean observer;

        public Indication(Identifier id, boolean observer, V view) {
            super(id, view);
            this.observer = observer;
        }
    }
}

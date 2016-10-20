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
package se.sics.ktoolbox.omngr.bootstrap.event;

import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class Heartbeat implements BootstrapEvent {
    public final Identifier eventId;
    public final OverlayId overlayId;
    public final int position;

    public Heartbeat(Identifier eventId, OverlayId overlayId, int position) {
        this.eventId = eventId;
        this.overlayId = overlayId;
        this.position = position;
    }
    
    public Heartbeat(OverlayId overlayId, int position) {
        this(BasicIdentifiers.eventId(), overlayId, position);
    }
    
    @Override
    public Identifier getId() {
        return eventId;
    }
    
    @Override
    public String toString() {
        return "Heartbeat<" + overlayId + ", " + eventId + ">";
    }
}

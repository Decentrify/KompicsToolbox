/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Croupier is free software; you can redistribute it and/or
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

package se.sics.ktoolbox.croupier.event;

import se.sics.ktoolbox.croupier.util.CroupierSpeed;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierControl implements CroupierEvent {
    public final Identifier eventId;
    public final Identifier overlayId;
    public final CroupierSpeed speed;
    
    public CroupierControl(Identifier overlayId, CroupierSpeed speed) {
        this.eventId = UUIDIdentifier.randomId();
        this.overlayId = overlayId;
        this.speed = speed;
    }

    @Override
    public Identifier getId() {
        return eventId;
    }

    @Override
    public Identifier overlayId() {
        return overlayId;
    }
    
    @Override
    public String toString() {
        return "CROUPIER<"+ overlayId + ">DISCONNECTED<" + eventId + ">";
    }
}

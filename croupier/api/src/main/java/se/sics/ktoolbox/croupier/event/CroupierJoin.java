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

import java.util.List;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.network.nat.NatAwareAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CroupierJoin implements CroupierEvent {
    public final Identifier eventId;
    public final OverlayId overlayId;
    public final List<NatAwareAddress> bootstrap;
    
    public CroupierJoin(Identifier eventId, OverlayId overlayId, List bootstrap) {
        this.eventId = eventId;
        this.overlayId = overlayId;
        this.bootstrap = bootstrap;
    }

    @Override
    public Identifier getId() {
        return eventId;
    }

    @Override
    public OverlayId overlayId() {
        return overlayId;
    }
    
    @Override
    public String toString() {
        return "Croupier<" + overlayId + ">Join<" + eventId + ">";
    }
}

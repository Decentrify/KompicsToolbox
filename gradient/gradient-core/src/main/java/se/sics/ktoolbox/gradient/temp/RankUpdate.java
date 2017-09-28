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

package se.sics.ktoolbox.gradient.temp;

import se.sics.kompics.id.Identifier;
import se.sics.ktoolbox.gradient.event.GradientEvent;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class RankUpdate implements GradientEvent {
    public final Identifier eventId;
    public final OverlayId overlayId;
    public final int rank;
    
    public RankUpdate(Identifier eventId, OverlayId overlayId, int rank) {
        this.eventId = eventId;
        this.overlayId = overlayId;
        this.rank = rank;
    }

    public RankUpdate(OverlayId overlayId, int rank) {
        this(BasicIdentifiers.eventId(), overlayId, rank);
    }
    
    @Override
    public Identifier getId() {
        return eventId;
    }

    @Override
    public OverlayId overlayId() {
        return overlayId;
    }
}

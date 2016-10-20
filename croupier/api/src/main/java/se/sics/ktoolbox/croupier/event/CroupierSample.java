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

import java.util.Map;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.other.AgingAdrContainer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierSample<C extends Object> implements CroupierEvent {

    public final Identifier eventId;
    public final OverlayId overlayId;
    public final Map<Identifier, AgingAdrContainer<KAddress, C>> publicSample;
    public final Map<Identifier, AgingAdrContainer<KAddress, C>> privateSample;
    
    public CroupierSample(Identifier eventId, OverlayId overlayId, Map publicSample, Map privateSample) {
        this.eventId = eventId;
        this.overlayId = overlayId;
        this.publicSample = publicSample;
        this.privateSample = privateSample;
    }
    
    public CroupierSample(OverlayId overlayId, Map publicSample, Map privateSample) {
        this(BasicIdentifiers.eventId(), overlayId, publicSample, privateSample);
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
        return "CROUPIER<"+ overlayId + ">SAMPLE<" + eventId + ">";
    }
}

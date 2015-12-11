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
import java.util.UUID;
import se.sics.kompics.network.Address;
import se.sics.kompics.simutil.identifiable.Identifier;
import se.sics.ktoolbox.util.other.AgingContainer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierSample<C extends Object> implements CroupierEvent {

    public final UUID id;
    public final Identifier overlayId;
    public final Map<Address, AgingContainer<Address, C>> publicSample;
    public final Map<Address, AgingContainer<Address, C>> privateSample;
    
    public CroupierSample(UUID id, Identifier overlayId, Map publicSample, Map privateSample) {
        this.id = id;
        this.overlayId = overlayId;
        this.publicSample = publicSample;
        this.privateSample = privateSample;
    }

    @Override
    public String toString() {
        return "CROUPIER_SAMPLE<" + id + ">";
    }
}

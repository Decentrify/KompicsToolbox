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
package se.sics.ktoolbox.util.network.basic;

import se.sics.kompics.network.Transport;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KHeader;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class DecoratedHeader<Adr extends KAddress> implements KHeader<Adr> {

    final BasicHeader<Adr> base;
    final OverlayId overlayId; 

    public DecoratedHeader(BasicHeader<Adr> base, OverlayId overlayId) {
        this.base = base;
        this.overlayId = overlayId;
    }
    
    @Override
    public Adr getSource() {
        return base.getSource();
    }

    @Override
    public Adr getDestination() {
        return base.getDestination();
    }

    @Override
    public Transport getProtocol() {
        return base.getProtocol();
    }

    public BasicHeader getBase() {
        return base;
    }
    
    public OverlayId getOverlayId() {
        return overlayId;
    }
    
    @Override
    public DecoratedHeader<Adr> withSource(Adr source) {
        return new DecoratedHeader<>(base.withSource(source), overlayId);
    }

    @Override
    public DecoratedHeader<Adr> withDestination(Adr destination) {
        return new DecoratedHeader<>(base.withDestination(destination), overlayId);
    }
    
    @Override
    public DecoratedHeader<Adr> answer() {
        return new DecoratedHeader<>(base.answer(), overlayId);
    }
}

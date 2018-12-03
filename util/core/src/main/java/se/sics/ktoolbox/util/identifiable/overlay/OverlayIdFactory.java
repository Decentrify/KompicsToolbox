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
package se.sics.ktoolbox.util.identifiable.overlay;

import se.sics.ktoolbox.util.identifiable.IdentifierBuilder;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OverlayIdFactory implements IdentifierFactory<OverlayId> {

    private final IdentifierFactory baseFactory;
    private final OverlayId.Type overlayType;
    private final byte overlayOwner;
    private final OverlayId.TypeComparator typeComparator;

    public OverlayIdFactory(IdentifierFactory baseFactory, OverlayId.Type overlayType, byte overlayOwner) {
        this.baseFactory = baseFactory;
        this.overlayType = overlayType;
        this.overlayOwner = overlayOwner;
        this.typeComparator = OverlayRegistryV2.getTypeComparator();
    }

    @Override
    public OverlayId randomId() {
        return new OverlayId(baseFactory.randomId(), overlayType, overlayOwner, typeComparator);
    }

    @Override
    public OverlayId id(IdentifierBuilder builder) {
        return new OverlayId(baseFactory.id(builder), overlayType, overlayOwner, typeComparator);
    }

    @Override
    public Class<OverlayId> idType() {
        return OverlayId.class;
    }

    @Override
    public void setRegisteredName(String name) {
        throw new RuntimeException("wrappers are not to be registered");
    }

    @Override
    public String getRegisteredName() {
        return baseFactory.getRegisteredName();
    }
}

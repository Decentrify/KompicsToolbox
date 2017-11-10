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

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import se.sics.kompics.util.Identifier;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistry;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OverlayIdSerializer implements Serializer {
    public final int id;
    public final Class baseIdType;
    public final OverlayId.TypeComparator overlayTypeComparator;
    public final OverlayId.TypeFactory overlayTypeFactory;
    
    public OverlayIdSerializer(int id) {
        this.id = id;
        this.baseIdType = IdentifierRegistry.lookup(BasicIdentifiers.Values.OVERLAY.toString()).idType();
        this.overlayTypeComparator = OverlayRegistry.getTypeComparator();
        this.overlayTypeFactory = OverlayRegistry.getTypeFactory();
    }
    
    @Override
    public int identifier() {
        return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        OverlayId obj = (OverlayId)o;
        Serializers.lookupSerializer(baseIdType).toBinary(obj.baseId, buf);
        try {
            buf.writeByte(overlayTypeFactory.toByte(obj.type));
        } catch (OverlayId.UnknownTypeException ex) {
            throw new RuntimeException(ex);
        }
        buf.writeByte(obj.owner);
    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        Identifier baseId  = (Identifier)Serializers.lookupSerializer(baseIdType).fromBinary(buf, hint);
        OverlayId.Type overlayType;
        try {
            overlayType = overlayTypeFactory.fromByte(buf.readByte());
        } catch (OverlayId.UnknownTypeException ex) {
            throw new RuntimeException(ex);
        }
        byte overlayOwner = buf.readByte();
        return new OverlayId(baseId, overlayType, overlayOwner, overlayTypeComparator);
    }
}

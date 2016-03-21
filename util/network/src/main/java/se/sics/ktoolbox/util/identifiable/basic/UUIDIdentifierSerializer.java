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
package se.sics.ktoolbox.util.identifiable.basic;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class UUIDIdentifierSerializer implements Serializer {
    private final int id;
    
    public UUIDIdentifierSerializer(int id) {
        this.id = id;
    }
    
    @Override
    public int identifier() {
        return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        UUIDIdentifier obj = (UUIDIdentifier)o;
        Serializers.lookupSerializer(UUID.class).toBinary(obj.id, buf);
    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        UUID identifier = (UUID)Serializers.lookupSerializer(UUID.class).fromBinary(buf, hint);
        return new UUIDIdentifier(identifier);
    }
}

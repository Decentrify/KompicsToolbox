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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import se.sics.ktoolbox.util.identifiable.BasicBuilders;
import se.sics.ktoolbox.util.identifiable.IdentifierBuilder;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class UUIDIdFactory implements IdentifierFactory<UUIDId> {

    private String registeredName;

    @Override
    public synchronized UUIDId randomId() {
        return new UUIDId(UUID.randomUUID());
    }

    @Override
    public UUIDId id(IdentifierBuilder builder) {
        UUID base;
        if (builder instanceof BasicBuilders.UUIDBuilder) {
            base = ((BasicBuilders.UUIDBuilder) builder).base;
        } else if (builder instanceof BasicBuilders.ByteBuilder) {
            BasicBuilders.ByteBuilder aux = (BasicBuilders.ByteBuilder) builder;
            ByteBuf buf = Unpooled.wrappedBuffer(aux.base);
            long mostSignificat = buf.readLong();
            long leastSignificant = buf.readLong();
            base = new UUID(mostSignificat, leastSignificant);
        } else if (builder instanceof BasicBuilders.StringBuilder) {
            BasicBuilders.StringBuilder aux = (BasicBuilders.StringBuilder) builder;
            base = UUID.fromString(aux.base);
        } else if (builder instanceof BasicBuilders.IntBuilder) {
            throw new UnsupportedOperationException("UUIDIdFactory does not support int builder");
        } else {
            throw new UnsupportedOperationException("UUIDIdFactory does not support builder:" + builder.getClass());
        }
        return new UUIDId(base);
    }

    @Override
    public Class<UUIDId> idType() {
        return UUIDId.class;
    }

    @Override
    public void setRegisteredName(String name) {
        this.registeredName = name;
    }
    
    @Override
    public String getRegisteredName() {
        return registeredName;
    }
}

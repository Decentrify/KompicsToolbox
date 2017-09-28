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

import com.google.common.base.Optional;
import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.id.Identifier;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistry;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BasicAddressSerializer implements Serializer {

    private static final Logger log = LoggerFactory.getLogger(Serializer.class);

    private final int id;
    private final Class nodeIdType;

    public BasicAddressSerializer(int id) {
        this.id = id;
        this.nodeIdType = IdentifierRegistry.lookup(BasicIdentifiers.Values.NODE.toString()).idType();
    }

    @Override
    public int identifier() {
        return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        BasicAddress adr = (BasicAddress) o;
        if (adr == null) {
            buf.writeInt(0); //simply put four 0 bytes since 0.0.0.0 is not a valid host ip
            return;
        }

        buf.writeBytes(adr.getIp().getAddress());
        // Write ports as 2 bytes instead of 4
        byte[] portBytes = Ints.toByteArray(adr.getPort());
        buf.writeByte(portBytes[2]);
        buf.writeByte(portBytes[3]);
        // Id
        Serializers.lookupSerializer(nodeIdType).toBinary(adr.getId(), buf);
    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        byte[] ipBytes = new byte[4];
        buf.readBytes(ipBytes);
        if ((ipBytes[0] == 0) && (ipBytes[1] == 0) && (ipBytes[2] == 0) && (ipBytes[3] == 0)) {
            return null; // IP 0.0.0.0 is not valid but null Address encoding
        }
        InetAddress addressIp;
        try {
            addressIp = InetAddress.getByAddress(ipBytes);
        } catch (UnknownHostException ex) {
            log.error("AddressSerializer: Could not create InetAddress.", ex);
            throw new RuntimeException("BasicAddressSerializer: Could not create InetAddress.", ex);
        }
        byte portUpper = buf.readByte();
        byte portLower = buf.readByte();
        int addressPort = Ints.fromBytes((byte) 0, (byte) 0, portUpper, portLower);
        Identifier addressId = (Identifier)Serializers.lookupSerializer(nodeIdType).fromBinary(buf, hint);

        //TODO Alex - this should be handled in  NatAwareAddress
//        if (hint.isPresent() && hint.get() instanceof InetSocketAddress) {
//            InetSocketAddress adr = (InetSocketAddress) hint.get();
//            addressIp = adr.getAddress();
//            addressPort = adr.getPort();
//        }

        return new BasicAddress(addressIp, addressPort, addressId);
    }
}

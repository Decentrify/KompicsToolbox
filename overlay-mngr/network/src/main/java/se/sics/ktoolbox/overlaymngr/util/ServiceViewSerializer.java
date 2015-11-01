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
package se.sics.ktoolbox.overlaymngr.util;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import se.sics.kompics.network.netty.serialization.Serializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ServiceViewSerializer implements Serializer {

    private final int id;

    public ServiceViewSerializer(int id) {
        this.id = id;
    }

    @Override
    public int identifier() {
        return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        ServiceView obj = (ServiceView) o;

        //more than 255 services means you are doing it wrong
        assert obj.runningServices.size() < 256;
        buf.writeByte(obj.runningServices.size()); 
        for (ByteBuffer serviceId : obj.runningServices) {
            assert serviceId.array().length == 4; //only using Integer for overlayId - 4 byte
            buf.writeBytes(serviceId.array());
        }
    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        List<ByteBuffer> services = new ArrayList<>();
        
        int serviceSize = buf.readByte();
        for(int i = 0; i < serviceSize; i++) {
            byte[] service = new byte[4];
            buf.readBytes(service);
            services.add(ByteBuffer.wrap(service));
        }
        return new ServiceView(services); 
    }
}
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
package se.sics.p2ptoolbox.util.network.impl;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class RouteSerializer implements Serializer {

    private final int id;

    public RouteSerializer(int id) {
        this.id = id;
    }

    @Override
    public int identifier() {
        return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
        Route obj = (Route) o;

        buf.writeByte(obj.size());
        //routes are never empty
        //routes are homogeneus
        Iterator it = obj.getRoute().iterator();
        Object template = it.next();
        //Serializer interface does not allow lookup by id, so i use first element as template
        Serializers.toBinary(template, buf);
        Serializer templateSerializer = Serializers.lookupSerializer(template.getClass());
        while(it.hasNext()) {
            templateSerializer.toBinary(it.next(), buf);
        }
    }

    @Override
    public Object fromBinary(ByteBuf buf, Optional<Object> hint) {
        int routeSize = buf.readByte();
        List route = new ArrayList();
        Object template = Serializers.fromBinary(buf, hint);
        Serializer templateSerializer = Serializers.lookupSerializer(template.getClass());
        route.add(template);
        for(int i = 1; i < routeSize; i++) {
            route.add(templateSerializer.fromBinary(buf, hint));
        }
        return new Route(route);
    }

}

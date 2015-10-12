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
package se.sics.ktoolbox.networkmngr.events;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import java.net.InetAddress;
import java.util.UUID;
import se.sics.kompics.Direct;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Transport;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class Create {

    public static class Request extends Direct.Request<Response> implements NetworkMngrEvent {

        public final UUID id;
//        public final HookId hookId;
        public final Address self;
        public final Optional<InetAddress> alternateBind;
        public final ImmutableSet<Transport> protocols;

//        public Request(UUID id, HookId hookId, Address self, Optional<InetAddress> alternateBind,
//                ImmutableSet<Transport> protocols) {
         public Request(UUID id, Address self, Optional<InetAddress> alternateBind,
                ImmutableSet<Transport> protocols) {
            this.id = id;
//            this.hookId = hookId;
            this.self = self;
            this.alternateBind = alternateBind;
            this.protocols = protocols;
        }

        public Response answer(UUID networkId, int boundPort) {
            return new Response(this, networkId, boundPort);
        }
    }

    public static class Response implements Direct.Response, NetworkMngrEvent {

        public final Request req;
        public final UUID networkId;
        public final int boundPort;

        public Response(Request req, UUID networkId, int boundPort) {
            this.req = req;
            this.networkId = networkId;
            this.boundPort = boundPort;
        }
    }
}

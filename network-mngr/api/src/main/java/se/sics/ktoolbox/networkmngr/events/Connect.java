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
import java.util.UUID;
import se.sics.kompics.ChannelFilter;
import se.sics.kompics.Direct;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class Connect {
    public static class Request extends Direct.Request<Response> implements NetworkMngrEvent{
        public final UUID id;
        public final UUID networkId;
        public final Positive<Network> network;
        public final Optional<ChannelFilter> outgoingFilter;
        public final Optional<ChannelFilter> incomingFilter;
        
        public Request(UUID id, UUID networkId, Positive<Network> network, Optional<ChannelFilter> outgoingFilter,
                Optional<ChannelFilter> incomingFilter) {
            this.id = id;
            this.networkId = networkId;
            this.network = network;
            this.outgoingFilter = outgoingFilter;
            this.incomingFilter = incomingFilter;
        }
        
        public Response answer() {
            return new Response(this);
        }
    }
    
    public static class Response implements Direct.Response, NetworkMngrEvent {
        public final Request req;
        
        public Response(Request req) {
            this.req = req;
        }
    }
}

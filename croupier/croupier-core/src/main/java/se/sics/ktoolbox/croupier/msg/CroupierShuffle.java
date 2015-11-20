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
package se.sics.ktoolbox.croupier.msg;

import com.google.common.base.Optional;
import java.util.Map;
import java.util.UUID;
import org.javatuples.Pair;
import se.sics.ktoolbox.util.address.NatAwareAddress;
import se.sics.ktoolbox.croupier.event.CroupierEvent;
import se.sics.ktoolbox.util.update.view.View;
import se.sics.ktoolbox.croupier.util.CroupierContainer;
import se.sics.p2ptoolbox.util.identifiable.UUIDIdentifiable;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierShuffle {

    public static abstract class Basic implements UUIDIdentifiable, CroupierEvent {
        public final UUID id;
        public final Optional<View> selfView;
        public final Map<NatAwareAddress, CroupierContainer> publicNodes;
        public final Map<NatAwareAddress, CroupierContainer> privateNodes;

        Basic(UUID id, Optional<View>selfView, Map<NatAwareAddress, CroupierContainer> publicNodes, 
                Map<NatAwareAddress, CroupierContainer> privateNodes) {
            this.id = id;
            this.selfView = selfView;
            this.publicNodes = publicNodes;
            this.privateNodes = privateNodes;
            if(publicNodes.size() > 128 || privateNodes.size() > 128) {
                throw new RuntimeException("Croupier shuffle message is too large - limit yourself to 128 public nodes and 128 private nodes per shuffle");
            }
        }
        
        @Override
        public final UUID getId() {
            return id;
        }
    }
    
    public static class Request extends Basic {
        public Request(UUID id, Optional<View>selfView, Map<NatAwareAddress, CroupierContainer> publicNodes, 
                Map<NatAwareAddress, CroupierContainer> privateNodes) {
            super(id, selfView, publicNodes, privateNodes);
        }
        
        @Override
        public String toString() {
            return "CROUPIER_SREQUEST<" + id + ">";
        }
        
    }
    
    public static class Response extends Basic {
        public Response(UUID id, Optional<View>selfView, Map<NatAwareAddress, CroupierContainer> publicNodes, 
                Map<NatAwareAddress, CroupierContainer> privateNodes) {
            super(id, selfView, publicNodes, privateNodes);
        }
        
        @Override
        public String toString() {
            return "CROUPIER_SRESPONSE<" + id + ">";
        }
    }
}

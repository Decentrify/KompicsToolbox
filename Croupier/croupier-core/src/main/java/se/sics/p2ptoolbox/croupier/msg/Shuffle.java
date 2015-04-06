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
package se.sics.p2ptoolbox.croupier.msg;

import java.util.Set;
import java.util.UUID;
import se.sics.p2ptoolbox.croupier.util.CroupierContainer;
import se.sics.p2ptoolbox.util.identifiable.UUIDIdentifiable;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Shuffle {

    public static abstract class Basic implements UUIDIdentifiable {

        private final UUID id;
        public final Set<CroupierContainer> publicNodes;
        public final Set<CroupierContainer> privateNodes;

        Basic(UUID id, Set<CroupierContainer> publicNodes, Set<CroupierContainer> privateNodes) {
            this.id = id;
            this.publicNodes = publicNodes;
            this.privateNodes = privateNodes;
        }
        
        @Override
        public final UUID getId() {
            return id;
        }
    }
    
    public static class Request extends Basic {
        public Request(UUID id, Set<CroupierContainer> publicNodes, Set<CroupierContainer> privateNodes) {
            super(id, publicNodes, privateNodes);
        }
        
        @Override
        public String toString() {
            return "ShuffleRequest";
        }
    }
    
    public static class Response extends Basic {
        public Response(UUID id, Set<CroupierContainer> publicNodes, Set<CroupierContainer> privateNodes) {
            super(id, publicNodes, privateNodes);
        }
        
        @Override
        public String toString() {
            return "ShuffleResponse";
        }
    }
}

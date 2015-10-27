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
package se.sics.ktoolbox.overlaymngr.events;

import java.util.UUID;
import se.sics.kompics.Direct;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.p2ptoolbox.croupier.CroupierControlPort;
import se.sics.p2ptoolbox.croupier.CroupierPort;
import se.sics.p2ptoolbox.util.update.SelfViewUpdatePort;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OMngrCroupier {
    public static class ConnectRequest extends Direct.Request<ConnectResponse> implements OverlayMngrEvent {
        public final UUID id;
        public final byte[] parentId;
        public final byte[] croupierId;
        public final Negative<CroupierPort> croupier;
        public final Negative<CroupierControlPort> croupierControl;
        public final Positive<SelfViewUpdatePort> viewUpdate;
        public final boolean observer;
        
        public ConnectRequest(UUID id, byte[] parentId, byte[] croupierId, Negative<CroupierPort> croupier, 
                Negative<CroupierControlPort> croupierControl, Positive<SelfViewUpdatePort> viewUpdate,
                boolean observer) {
            this.id = id;
            this.parentId = parentId;
            this.croupierId = croupierId;
            this.croupier = croupier;
            this.croupierControl = croupierControl;
            this.viewUpdate = viewUpdate;
            this.observer = observer;
        }
        
        public ConnectResponse answer() {
            return new ConnectResponse(this);
        }
    }
    
    public static class ConnectResponse implements Direct.Response, OverlayMngrEvent {
        public final ConnectRequest req;
        
        public ConnectResponse(ConnectRequest req) {
            this.req = req;
        }
    }
    
    public static class Disconnect implements OverlayMngrEvent {
        public final byte[] croupierId;
        
        public Disconnect(byte[] croupierId) {
            this.croupierId = croupierId;
        }
    }
}
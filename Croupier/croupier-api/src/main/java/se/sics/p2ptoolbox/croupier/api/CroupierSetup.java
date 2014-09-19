/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Croupier is free software; you can redistribute it and/or
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
package se.sics.p2ptoolbox.croupier.api;

import org.javatuples.Pair;
import static org.junit.Assert.*;
import se.sics.p2ptoolbox.croupier.api.net.PeerViewSerializer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierSetup {

    public final byte CROUPIER_NET_REQ;
    public final byte CROUPIER_NET_RESP;
    public final SerializationContext;
    public final Pair<Byte, Byte> peerViewSerializerId;

    private CroupierSetup(byte croupierNetReq, byte croupierNetResp, Pair<Byte, Byte> peerViewSerializerId) {
        this.CROUPIER_NET_REQ = croupierNetReq;
        this.CROUPIER_NET_RESP = croupierNetResp;
        this.peerViewSerializerId = peerViewSerializerId;
    }
    
    public static class CroupierSetupBuilder {
        private Byte croupierNetReq = null;
        private Byte croupierNetResp = null;
        
        private Pair<Byte, Byte> peerViewSerializerId = null;
        
        public CroupierSetupBuilder setCroupierNetCodes(byte croupierNetReq, byte croupierNetResp) {
            this.croupierNetReq = croupierNetReq;
            this.croupierNetResp = croupierNetResp;
            return this;
        }
        
        public CroupierSetupBuilder setPeerViewAdapter(Pair<Byte, Byte> peerViewSerializerId) {
            this.peerViewSerializerId = peerViewSerializerId;
            return this;
        }
        
        public CroupierSetup finalise() {
            assertNotNull(croupierNetReq);
            assertNotNull(croupierNetResp);
            assertNotNull(peerViewSerializerId);
            
            return new CroupierSetup(croupierNetReq, croupierNetResp, peerViewSerializerId);
        }
    }
}

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

package se.sics.ktoolbox.overlaymngr.event;

import java.util.UUID;
import org.javatuples.Pair;
import se.sics.kompics.Direct;
import se.sics.kompics.Negative;
import se.sics.p2ptoolbox.croupier.CroupierPort;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class Croupier {
    
    public static class Start extends Direct.Request implements OverlayMngrEvent {
        public final UUID id;
        public final Pair<Byte, Byte> overlayId;
        
        public Start(UUID id, Pair<Byte, Byte> overlayId) {
            super();
            this.id = id;
            this.overlayId = overlayId;
        }
        
        public Started answer(Negative<CroupierPort> croupierPort) {
            return new Started(id, overlayId, croupierPort);
        }
    }
    
    public static class Started implements Direct.Response, OverlayMngrEvent {
        public final UUID id;
        public final Pair<Byte, Byte> overlyaId;
        public final Negative<CroupierPort> croupierPort;
        
        private Started(UUID id, Pair<Byte, Byte> overlayId, Negative<CroupierPort> croupierPort) {
            this.id = id;
            this.overlyaId = overlayId;
            this.croupierPort = croupierPort;
        }
    }
    
    public static class Stop extends Direct.Request implements OverlayMngrEvent {
        public final UUID id;
        public final Pair<Byte, Byte> overlayId;
        
        public Stop(UUID id, Pair<Byte, Byte> overlayId) {
            super();
            this.id = id;
            this.overlayId = overlayId;
        }
        
        public Stopped answer() {
            return new Stopped(id, overlayId);
        }
    }
    
    public static class Stopped implements Direct.Response, OverlayMngrEvent {
        public final UUID id;
        public final Pair<Byte, Byte> overlayId;
        
        public Stopped(UUID id, Pair<Byte, Byte> overlayId) {
            this.id = id;
            this.overlayId = overlayId;
        }
    }
}

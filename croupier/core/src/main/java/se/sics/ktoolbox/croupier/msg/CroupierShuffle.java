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
import se.sics.kompics.id.Identifier;
import se.sics.ktoolbox.croupier.event.CroupierEvent;
import se.sics.ktoolbox.croupier.util.CroupierContainer;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.update.View;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierShuffle {

    public static abstract class Basic implements CroupierEvent {

        public final Identifier msgId;
        public final OverlayId overlayId;
        public final Optional<View> selfView;
        public final Map<Identifier, CroupierContainer> publicNodes;
        public final Map<Identifier, CroupierContainer> privateNodes;

        Basic(Identifier msgId, OverlayId overlayId,
                Optional<View> selfView, Map<Identifier, CroupierContainer> publicNodes,
                Map<Identifier, CroupierContainer> privateNodes) {
            this.msgId = msgId;
            this.overlayId = overlayId;
            this.selfView = selfView;
            this.publicNodes = publicNodes;
            this.privateNodes = privateNodes;
            if (publicNodes.size() > 128 || privateNodes.size() > 128) {
                throw new RuntimeException("Croupier shuffle message is too large - limit yourself to 128 public nodes and 128 private nodes per shuffle");
            }
        }

        Basic(OverlayId overlayId,
                Optional<View> selfView, Map<Identifier, CroupierContainer> publicNodes,
                Map<Identifier, CroupierContainer> privateNodes) {
            this(BasicIdentifiers.msgId(), overlayId, selfView, publicNodes, privateNodes);
        }

        @Override
        public OverlayId overlayId() {
            return overlayId;
        }

        @Override
        public Identifier getId() {
            return msgId;
        }
    }

    public static class Request extends Basic {

        public Request(Identifier msgId, OverlayId overlayId,
                Optional<View> selfView, Map<Identifier, CroupierContainer> publicNodes,
                Map<Identifier, CroupierContainer> privateNodes) {
            super(msgId, overlayId, selfView, publicNodes, privateNodes);
        }

        public Request(OverlayId overlayId,
                Optional<View> selfView, Map<Identifier, CroupierContainer> publicNodes,
                Map<Identifier, CroupierContainer> privateNodes) {
            super(overlayId, selfView, publicNodes, privateNodes);
        }

        @Override
        public String toString() {
            return "Croupier<" + overlayId() + ">ShuffleReq<" + msgId + ">";
        }
        
        public Response answer(Optional<View> selfView, Map<Identifier, CroupierContainer> publicNodes,
                Map<Identifier, CroupierContainer> privateNodes) {
            return new Response(msgId, overlayId, selfView, publicNodes, privateNodes);
        }
    }

    public static class Response extends Basic {

        public Response(Identifier msgId, OverlayId overlayId,
                Optional<View> selfView, 
                Map<Identifier, CroupierContainer> publicNodes,
                Map<Identifier, CroupierContainer> privateNodes) {
            super(msgId, overlayId, selfView, publicNodes, privateNodes);
        }
        
         public Response(OverlayId overlayId,
                Optional<View> selfView, Map<Identifier, 
                        CroupierContainer> publicNodes,
                Map<Identifier, CroupierContainer> privateNodes) {
            super(overlayId, selfView, publicNodes, privateNodes);
        }

        @Override
        public String toString() {
            return "Croupier<" + overlayId() + ">ShuffleResp<" + msgId + ">";
        }
    }
}

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
package se.sics.ktoolbox.omngr.bootstrap.msg;

import java.util.List;
import se.sics.kompics.id.Identifier;
import se.sics.ktoolbox.omngr.bootstrap.event.BootstrapEvent;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class Sample {

    public static class Request implements BootstrapEvent {

        public final Identifier msgId;
        public final OverlayId overlayId;

        public Request(Identifier msgId, OverlayId overlayId) {
            this.msgId = msgId;
            this.overlayId = overlayId;
        }

        public Request(OverlayId overlayId) {
            this(BasicIdentifiers.msgId(), overlayId);
        }

        @Override
        public Identifier getId() {
            return msgId;
        }

        @Override
        public String toString() {
            return "Sample<" + overlayId + ">Request<" + msgId + ">";
        }
        
        public Response answer(List<KAddress> sample) {
            return new Response(msgId, overlayId, sample);
        }
    }

    public static class Response implements BootstrapEvent {
        public final Identifier msgId;
        public final OverlayId overlayId;
        public final List<KAddress> sample;

        Response(Identifier msgId, OverlayId overlayId, List<KAddress> sample) {
            this.msgId = msgId;
            this.overlayId = overlayId;
            this.sample = sample;
        }

        @Override
        public Identifier getId() {
            return msgId;
        }

        @Override
        public String toString() {
            return "Sample<" + overlayId + ">Response<" + msgId + ">";
        }
    }
}

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
package se.sics.ledbat.ncore.msg;

import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.overlays.OverlayEvent;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LedbatMsg {

    public static class Request<P extends OverlayEvent> implements OverlayEvent {

        public final P payload;
        public final long leecherAppReqSendT;

        protected Request(P payload, long leecherAppReqSendT) {
            this.payload = payload;
            this.leecherAppReqSendT = leecherAppReqSendT;
        }

        public Request(P payload) {
            this(payload, System.currentTimeMillis());
        }

        public <PP extends OverlayEvent> Response answer(PP answerPayload) {
            return new Response(this, answerPayload);
        }

        @Override
        public Identifier overlayId() {
            return payload.overlayId();
        }

        @Override
        public Identifier getId() {
            return payload.getId();
        }
    }

    public static class Response<P extends OverlayEvent> implements OverlayEvent {

        public final P payload;
        public final long leecherAppReqSendT;
        public final long seederNetRespSendT;
        public final long leechedNetRespT;

        protected Response(P payload, long leecherAppReqSendT, long seederNetRespSendT, long leechedNetRespT) {
            this.payload = payload;
            this.leecherAppReqSendT = leecherAppReqSendT;
            this.seederNetRespSendT = seederNetRespSendT;
            this.leechedNetRespT = leechedNetRespT;
        }

        private Response(Request req, P payload) {
            //-1 set in serializer - as close as possible to network so we best estimate network delay and not kompics delay
            this(payload, req.leecherAppReqSendT, -1, -1);
        }

        @Override
        public Identifier overlayId() {
            return payload.overlayId();
        }

        @Override
        public Identifier getId() {
            return payload.getId();
        }
    }
}

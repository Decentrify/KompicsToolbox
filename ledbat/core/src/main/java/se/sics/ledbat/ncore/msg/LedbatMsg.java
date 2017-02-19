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

import se.sics.ktoolbox.util.identifiable.Identifiable;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.nutil.ContentWrapper;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LedbatMsg {

    public static class Request<C extends Identifiable> implements Identifiable, ContentWrapper<C> {

        public final C content;
        public final long leecherAppReqSendT;

        protected Request(C content, long leecherAppReqSendT) {
            this.content = content;
            this.leecherAppReqSendT = leecherAppReqSendT;
        }

        public Request(C content) {
            this(content, System.currentTimeMillis());
        }
        
        @Override
        public Identifier getId() {
            return content.getId();
        }

        @Override
        public C getWrappedContent() {
            return content;
        }

        @Override
        public String toString() {
            return "Ledbat.Request<" + content.toString() + ">";
        }
        
        public <AC extends Identifiable> Response answer(AC answerContent) {
            return new Response(this, answerContent);
        }
    }

    public static class Response<C extends Identifiable> implements Identifiable, ContentWrapper<C> {

        public final C content;
        public final long leecherAppReqSendT;
        public final long seederNetRespSendT;
        public final long leecherNetRespT;

        protected Response(C content, long leecherAppReqSendT, long seederNetRespSendT, long leecherNetRespT) {
            this.content = content;
            this.leecherAppReqSendT = leecherAppReqSendT;
            this.seederNetRespSendT = seederNetRespSendT;
            this.leecherNetRespT = leecherNetRespT;
        }

        private Response(Request req, C content) {
            //-1 set in serializer - as close as possible to network so we best estimate network delay and not kompics delay
            this(content, req.leecherAppReqSendT, -1, -1);
        }

        @Override
        public Identifier getId() {
            return content.getId();
        }

        @Override
        public C getWrappedContent() {
            return content;
        }
        
        @Override
        public String toString() {
            return "Ledbat.Response<" + content.toString() + ">";
        }
    }
}

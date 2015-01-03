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
package se.sics.p2ptoolbox.serialization.msg;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.net.VodAddress;
import se.sics.p2ptoolbox.serialization.SerializationContext;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class NetMsg {

    public static void setContext(SerializationContext setContext) {
        Request.context = setContext;
        Response.context = setContext;
        OneWay.context = setContext;
    }

    public static boolean hasContext() {
        return Request.context != null && Response.context != null && OneWay.context != null;
    }
    
    public static abstract class Request extends DirectMsgNetty.Request {

        protected static SerializationContext context;
        public final UUID id;
        public final Map<String, HeaderField> header;

        public Request(VodAddress src, VodAddress dest, UUID id) {
            super(src, dest);
            //TODO ALEX fix later
            setTimeoutId(se.sics.gvod.timer.UUID.nextUUID());
            //fix

            this.id = id;
            this.header = new HashMap<String, HeaderField>();
        }

        public Request(VodAddress src, VodAddress dest, UUID id, Map<String, HeaderField> header) {
            super(src, dest);
            //TODO ALEX fix later
            setTimeoutId(se.sics.gvod.timer.UUID.nextUUID());
            //fix

            this.id = id;
            this.header = header;
        }

        @Override
        public byte getOpcode() {
            try {
                return context.getCode(this.getClass()).getValue0();
            } catch (SerializationContext.MissingException ex) {
                throw new RuntimeException();
            }
        }

        @Override
        public int getSize() {
            return getHeaderSize();
        }
    }

    public static abstract class Response extends DirectMsgNetty.Response {

        protected static SerializationContext context;
        public final UUID id;
        public final Map<String, HeaderField> header;

        public Response(VodAddress src, VodAddress dest, UUID id) {
            super(src, dest);
            //TODO ALEX fix later
            setTimeoutId(se.sics.gvod.timer.UUID.nextUUID());
            //fix

            this.id = id;
            this.header = new HashMap<String, HeaderField>();
        }

        public Response(VodAddress src, VodAddress dest, UUID id, Map<String, HeaderField> header) {
            super(src, dest);
            //TODO ALEX fix later
            setTimeoutId(se.sics.gvod.timer.UUID.nextUUID());
            //fix

            this.id = id;
            this.header = header;
        }

        @Override
        public byte getOpcode() {
            try {
                return context.getCode(this.getClass()).getValue0();
            } catch (SerializationContext.MissingException ex) {
                throw new RuntimeException();
            }
        }

        @Override
        public int getSize() {
            return getHeaderSize();
        }
    }

    public static abstract class OneWay extends DirectMsgNetty.Oneway {

        protected static SerializationContext context;
        public final UUID id;
        public final Map<String, HeaderField> header;

        public OneWay(VodAddress src, VodAddress dest, UUID id) {
            super(src, dest);

            this.id = id;
            this.header = new HashMap<String, HeaderField>();
        }

        public OneWay(VodAddress src, VodAddress dest, UUID id, Map<String, HeaderField> header) {
            super(src, dest);

            this.id = id;
            this.header = header;
        }

        @Override
        public byte getOpcode() {
            try {
                return context.getCode(this.getClass()).getValue0();
            } catch (SerializationContext.MissingException ex) {
                throw new RuntimeException();
            }
        }

        @Override
        public int getSize() {
            return getHeaderSize();
        }
    }

}

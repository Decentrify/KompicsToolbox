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
package se.sics.p2ptoolbox.caracalclient.bootstrap.msg;

import java.util.Map;
import se.sics.kompics.Direct;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CCSchema {

    public static class Request extends Direct.Request<Response> {

        public final String name;
        public final Map<String, String> metaData;

        public Request(String name, Map<String, String> metaData) {
            this.name = name;
            this.metaData = metaData;
        }
    }

    public static class Response implements Direct.Response {
        public final String name;
        public final byte[] id;
        
        public Response(String name, byte[] id) {
            this.name = name;
            this.id = id;
        }
    }
}

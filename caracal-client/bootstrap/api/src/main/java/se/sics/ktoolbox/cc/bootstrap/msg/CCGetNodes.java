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
package se.sics.ktoolbox.cc.bootstrap.msg;

import java.util.List;
import se.sics.caracaldb.Address;
import se.sics.kompics.Direct.Request;
import se.sics.kompics.Direct.Response;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CCGetNodes {
    public static class Req extends Request {
        public Req() {
            super();
        }
    }
    
    public static class Resp implements Response {
        public final List<Address> caracalNodes;

        public Resp(List<Address> caracalNodes) {
            super();
            this.caracalNodes = caracalNodes;
        }
    }
}

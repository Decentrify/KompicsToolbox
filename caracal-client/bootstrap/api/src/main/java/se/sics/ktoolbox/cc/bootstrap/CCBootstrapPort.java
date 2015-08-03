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
package se.sics.ktoolbox.cc.bootstrap;

import se.sics.kompics.PortType;
import se.sics.ktoolbox.cc.bootstrap.msg.CCDisconnected;
import se.sics.ktoolbox.cc.bootstrap.msg.CCGetNodes;
import se.sics.ktoolbox.cc.bootstrap.msg.CCReady;
import se.sics.ktoolbox.cc.bootstrap.msg.CCUpdate;
import se.sics.ktoolbox.cc.common.op.CCOpEvent;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CCBootstrapPort extends PortType {
    {
        indication(CCReady.class);
        indication(CCDisconnected.class);
        
        request(CCOpEvent.Request.class);
        indication(CCOpEvent.Response.class);
        indication(CCOpEvent.Timeout.class);
        
        request(CCGetNodes.Req.class);
        indication(CCGetNodes.Resp.class);
        
        request(CCUpdate.class);
    }
}

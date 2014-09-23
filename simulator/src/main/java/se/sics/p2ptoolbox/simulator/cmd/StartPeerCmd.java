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

package se.sics.p2ptoolbox.simulator.cmd;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;
import se.sics.kompics.PortType;
import se.sics.p2ptoolbox.simulator.cmd.SystemCmd;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class StartPeerCmd<E extends ComponentDefinition> implements SystemCmd {
    public final int id;
    public final Class<E> peerClass;
    public final Class<? extends PortType> peerPort;
    public final Init<E> init;
    
    public StartPeerCmd(int id, Class<E> peerClass, Class<? extends PortType> peerPort, 
            Init<E> init) {
        this.id = id;
        this.peerClass = peerClass;
        this.peerPort = peerPort;
        this.init = init;
    }
}

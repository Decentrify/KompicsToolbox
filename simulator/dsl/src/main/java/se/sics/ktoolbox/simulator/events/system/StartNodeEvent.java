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

package se.sics.ktoolbox.simulator.events.system;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;
import se.sics.kompics.network.Address;
import se.sics.ktoolbox.simulator.events.SystemEvent;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 * @param <E>
 */
public abstract class StartNodeEvent<E extends ComponentDefinition, A extends Address> extends SystemEvent {

    public StartNodeEvent() {
        super();
    }
    
    public abstract Integer getNodeId();
    public abstract A getAddress();
    public abstract Class<E> getComponentDefinition();
    public abstract Init<E> getComponentInit();
}
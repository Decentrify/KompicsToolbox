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
package se.sics.ktoolbox.aggregator.client.util;

import se.sics.ktoolbox.aggregator.util.PacketInfo;
import se.sics.ktoolbox.aggregator.util.Processor;

/**
 * Interface for the processing the component information and
 * generating the packet information which will be forwarded to the global aggregator.
 *
 * @param <PI_I> InputType PacketInfo
 * @param <PI_O> OutputType PacketInfo
 */
public interface ComponentInfoProcessor<PI_I extends PacketInfo, PI_O extends PacketInfo> extends Processor {


    /**
     * Each processor the component information should accept the
     * whole of the information from the component and generate a packet information object which
     * needs to be sent to the application.
     *
     * @param componentInfo Component Information
     *
     * @return Packet Information
     */
    public PI_O processComponentInfo(PI_I componentInfo);
}

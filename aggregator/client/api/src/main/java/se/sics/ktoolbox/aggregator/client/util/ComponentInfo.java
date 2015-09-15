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

/**
 * Marker Interface indicating packet information from the component
 * which needs to be captured and aggregated by the local aggregator.
 *
 * Created by babbar on 2015-08-31.
 */
public interface ComponentInfo extends PacketInfo{


    /**
     * The option instructs the component to append the
     * updated information regarding the component to the
     * original one.
     *
     * @param cInfo Component Info
     * @return updated information.
     */
    public ComponentInfo append(ComponentInfo cInfo);

}

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
package se.sics.ktoolbox.util.idextractor;

import java.util.Optional;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.IntIdFactory;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.ports.ChannelIdExtractor;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class DestinationPortIdExtractor extends ChannelIdExtractor<KContentMsg, Identifier>{
    private final IntIdFactory intIdFactory;
    public DestinationPortIdExtractor() {
        super(KContentMsg.class);
        this.intIdFactory = new IntIdFactory(Optional.empty());
    }
    
    @Override
    public Identifier getValue(KContentMsg msg) {
        Identifier portId = intIdFactory.rawId(msg.getHeader().getDestination().getPort());
        return portId;
    }
}

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
package se.sics.ktoolbox.election.aggregation;

import java.util.HashSet;
import java.util.Set;
import org.javatuples.Pair;
import se.sics.kompics.id.Identifier;
import se.sics.ktoolbox.util.aggregation.PacketReducer;
import se.sics.ktoolbox.util.aggregation.StatePacket;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LeaderHistoryReducer implements PacketReducer<LeaderHistoryPacket, LeaderUpdatePacket> {

    @Override
    public Set<Class> interestedInPackets() {
        Set<Class> packets = new HashSet<>();
        packets.add(LeaderUpdatePacket.class);
        return packets;
    }

    @Override
    public StatePacket emptySP() {
        return new LeaderHistoryPacket();
    }

    @Override
    public LeaderHistoryPacket appendSP(LeaderHistoryPacket current, LeaderUpdatePacket append) {
        Pair<Identifier, Set<Identifier>> leader = null;
        if(append.leaderId != null) {
            leader = Pair.with(append.leaderId, append.leaderGroup);
        }
        current.append(leader);
        return current;
    }

    @Override
    public LeaderHistoryPacket clearSP(LeaderHistoryPacket current) {
        current.clear();
        return current;
    }
    
    @Override
    public String toString() {
        return "LeaderHistory";
    }
}

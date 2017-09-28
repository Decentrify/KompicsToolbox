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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import se.sics.kompics.id.Identifier;
import se.sics.ktoolbox.util.aggregation.StatePacket;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LeaderUpdatePacket implements StatePacket {
    public final Identifier leaderId;
    public final Set<Identifier> leaderGroup;
    
    public LeaderUpdatePacket(Identifier leaderId, Set<Identifier> leaderGroup) {
        this.leaderId = leaderId;
        this.leaderGroup = leaderGroup;
    }
    
    public static LeaderUpdatePacket update(Identifier leaderId, ArrayList<KAddress> leaderGroup) {
        Set<Identifier> lG = new HashSet<>(); 
        for(KAddress lgMember : leaderGroup) {
            lG.add(lgMember.getId());
        }
        return new LeaderUpdatePacket(leaderId, lG);
    }

    @Override
    public String shortPrint() {
        return toString();
    }
}

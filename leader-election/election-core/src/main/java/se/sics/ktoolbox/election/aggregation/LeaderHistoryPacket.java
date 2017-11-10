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

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.javatuples.Pair;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.util.aggregation.StatePacket;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LeaderHistoryPacket implements StatePacket {

    private Pair<Identifier, Set<Identifier>> currentLeader;
    private final List<Pair<Identifier, Set<Identifier>>> pastLeaders;

    LeaderHistoryPacket(Pair<Identifier, Set<Identifier>> currentLeader, List<Pair<Identifier, Set<Identifier>>> pastLeaders) {
        this.currentLeader = currentLeader;
        this.pastLeaders = pastLeaders;
    }

    public LeaderHistoryPacket() {
        this(null, new ArrayList<Pair<Identifier, Set<Identifier>>>());
    }

    public void append(Pair<Identifier, Set<Identifier>> newLeader) {
        if (newLeader == null && currentLeader != null) {
            pastLeaders.add(currentLeader);
        } else if (newLeader != null && currentLeader != null) {
            if (!currentLeader.getValue0().equals(newLeader.getValue0())
                    || !Sets.symmetricDifference(currentLeader.getValue1(), newLeader.getValue1()).isEmpty()) {
                pastLeaders.add(currentLeader);
            }
        }
        currentLeader = newLeader;
    }

    public void clear() {
        pastLeaders.clear();
    }

    public Pair<Identifier, Set<Identifier>> currentLeader() {
        return currentLeader;
    }

    public List<Pair<Identifier, Set<Identifier>>> pastLeaders() {
        return new ArrayList<>(pastLeaders);
    }

    public LeaderHistoryPacket copy() {
        return new LeaderHistoryPacket(currentLeader, new ArrayList<>(pastLeaders));
    }

    @Override
    public String shortPrint() {
        if (currentLeader == null) {
            return "not leader";
        } else {
            return "leader:" + currentLeader.getValue0() + " leaderGroup:" + currentLeader.getValue1();
        }
    }
}

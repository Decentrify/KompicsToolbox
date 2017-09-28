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
import java.util.List;
import se.sics.kompics.id.Identifier;
import se.sics.ktoolbox.util.aggregation.StatePacket;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LeaderGroupHistoryPacket implements StatePacket {

    private Identifier currentLeader;
    private final List<Identifier> pastLeaders;

    LeaderGroupHistoryPacket(Identifier currentLeader, List<Identifier> pastLeaders) {
        this.currentLeader = currentLeader;
        this.pastLeaders = pastLeaders;
    }

    public LeaderGroupHistoryPacket() {
        this(null, new ArrayList<Identifier>());
    }

    public void append(Identifier newLeader) {
        if (newLeader == null && currentLeader != null) {
            pastLeaders.add(currentLeader);
        } else if (newLeader != null && currentLeader != null) {
            if (!currentLeader.equals(newLeader)) {
                pastLeaders.add(currentLeader);
            }
        }
        currentLeader = newLeader;
    }

    public void clear() {
        pastLeaders.clear();
    }

    public Identifier currentLeader() {
        return currentLeader;
    }

    public List<Identifier> pastLeaders() {
        return new ArrayList<>(pastLeaders);
    }

    public LeaderGroupHistoryPacket copy() {
        return new LeaderGroupHistoryPacket(currentLeader, new ArrayList<>(pastLeaders));
    }
    
    @Override
    public String shortPrint() {
        return "leader:" + (currentLeader == null ? null : currentLeader.toString()) + " changedLeaders:" + pastLeaders.size();
    }
}

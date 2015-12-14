/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Croupier is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.croupier.history;

import java.util.HashSet;
import java.util.Set;
import se.sics.ktoolbox.croupier.util.CroupierContainer;
import se.sics.ktoolbox.util.network.nat.NatAwareAddress;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ShuffleHistoryContainer {
    public final CroupierContainer container;
    private final long addedAt;
    private long sentAt;
    private final Set<NatAwareAddress> sentTo = new HashSet<>();
    
    public ShuffleHistoryContainer(CroupierContainer container) {
        this.container = container;
        this.addedAt = System.currentTimeMillis();
    }
    
    public long addedAt() {
        return addedAt;
    }
    
    public long lastSentAt() {
        return sentAt;
    }
    
    public void sendTo(NatAwareAddress peer) {
        sentAt = System.currentTimeMillis();
        sentTo.add(peer);
    }
    public boolean wasSentTo(NatAwareAddress peer) {
        return sentTo.contains(peer);
    }
}

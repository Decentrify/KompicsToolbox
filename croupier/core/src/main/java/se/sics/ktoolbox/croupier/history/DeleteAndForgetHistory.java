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

import java.util.HashMap;
import java.util.Map;
import se.sics.ktoolbox.croupier.util.CroupierContainer;
import se.sics.ktoolbox.util.address.nat.NatAwareAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class DeleteAndForgetHistory implements ShuffleHistory {

    private Map<NatAwareAddress, ShuffleHistoryContainer> history = new HashMap<>();

    @Override
    public void sendTo(CroupierContainer container, NatAwareAddress peer) {
        ShuffleHistoryContainer containerHistory = history.get(container.getSource());
        if (containerHistory == null) {
            containerHistory = new ShuffleHistoryContainer(container);
            history.put(container.getSource(), containerHistory);
        }
        containerHistory.sendTo(peer);
    }

    @Override
    public Map<NatAwareAddress, CroupierContainer> sentTo(NatAwareAddress shufflePartner) {
        Map<NatAwareAddress, CroupierContainer> sentToPeer = new HashMap<>();
        for(Map.Entry<NatAwareAddress, ShuffleHistoryContainer> historyContainer : history.entrySet()) {
            if(historyContainer.getValue().wasSentTo(shufflePartner)) {
                CroupierContainer container = historyContainer.getValue().container;
                sentToPeer.put(container.src, container);
            }
        }
        return sentToPeer;
    }

    @Override
    public boolean wasSentTo(NatAwareAddress containerOrigin, NatAwareAddress shufflePartner) {
        ShuffleHistoryContainer containerHistory = history.get(containerOrigin);
        if (containerHistory == null) {
            //TODO Alex - see to fix maybe
            //a bit fishy - with this history type you should not have a container that i don't keep a history track of
            return false;
        }
        return containerHistory.wasSentTo(shufflePartner);
    }

    @Override
    public void remove(NatAwareAddress containerOrigin) {
        history.remove(containerOrigin);
    }
}

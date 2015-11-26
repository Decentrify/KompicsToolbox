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

import java.util.Map;
import java.util.UUID;
import se.sics.ktoolbox.util.address.NatAwareAddress;
import se.sics.ktoolbox.croupier.util.CroupierContainer;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public interface ShuffleHistory {
    public void sendTo(CroupierContainer container, NatAwareAddress shufflePartner);
    public Map<NatAwareAddress, CroupierContainer> sentTo(NatAwareAddress shufflePartner);
    public boolean wasSentTo(NatAwareAddress containerOrigin, NatAwareAddress shufflePartner);
    public void remove(NatAwareAddress containerOrigin);
}
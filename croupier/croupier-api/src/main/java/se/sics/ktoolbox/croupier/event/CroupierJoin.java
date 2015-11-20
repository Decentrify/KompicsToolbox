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
package se.sics.ktoolbox.croupier.event;

import java.util.List;
import java.util.UUID;
import se.sics.ktoolbox.util.address.NatAwareAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierJoin implements CroupierEvent {
    public final UUID id;
    public final List<NatAwareAddress> peers;

    public CroupierJoin(UUID id, List<NatAwareAddress> peers) {
        this.id = id;
        this.peers = peers;
    }

    @Override
    public String toString() {
        return "CROUPIER_JOIN<" +  id + ">";
    }
}

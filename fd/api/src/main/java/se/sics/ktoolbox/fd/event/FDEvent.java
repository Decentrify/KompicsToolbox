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
package se.sics.ktoolbox.fd.event;

import java.util.UUID;
import org.javatuples.Pair;
import se.sics.kompics.KompicsEvent;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public interface FDEvent extends KompicsEvent {

    public static class Follow implements FDEvent {

        public final Pair<DecoratedAddress, byte[]> target;
        public final UUID followerId;

        public Follow(Pair<DecoratedAddress, byte[]> target, UUID followerId) {
            this.target = target;
            this.followerId = followerId;
        }
    }

    public static class Unfollow implements FDEvent {

        public final Pair<DecoratedAddress, byte[]> target;
        public final UUID followerId;

        public Unfollow(Pair<DecoratedAddress, byte[]> target, UUID followerId) {
            this.target = target;
            this.followerId = followerId;
        }
    }

    public static class Suspect implements FDEvent {
        public final Pair<DecoratedAddress, byte[]> target;
        
        public Suspect(Pair<DecoratedAddress, byte[]> target) {
            this.target = target;
        }
    }
}

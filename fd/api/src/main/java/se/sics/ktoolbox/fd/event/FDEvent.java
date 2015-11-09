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

import java.nio.ByteBuffer;
import java.util.UUID;
import se.sics.kompics.Direct;
import se.sics.kompics.KompicsEvent;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public interface FDEvent extends KompicsEvent {

    public static class Follow extends Direct.Request<Suspect> implements FDEvent {
        public final DecoratedAddress target;
        public final ByteBuffer service;
        public final UUID followerId; //typically component id
        
        public Follow(DecoratedAddress target, ByteBuffer service, UUID followerId) {
            this.target = target;
            this.service = service;
            this.followerId = followerId;
        }

        public Suspect answer() {
            return new Suspect(target, service, followerId);
        }
    }

    public static class Unfollow implements FDEvent {

        public final DecoratedAddress target;
        public final ByteBuffer service;
        public final UUID followerId; //typically component id

        public Unfollow(DecoratedAddress target, ByteBuffer service, UUID followerId) {
            this.target = target;
            this.service = service;
            this.followerId = followerId;
        }
    }

    public static class Suspect implements Direct.Response, FDEvent {

        public final DecoratedAddress target;
        public final ByteBuffer service;
        public final UUID followerId; //typically component id

        public Suspect(DecoratedAddress target, ByteBuffer service, UUID followerId) {
            this.target = target;
            this.service = service;
            this.followerId = followerId;
        }
    }
}

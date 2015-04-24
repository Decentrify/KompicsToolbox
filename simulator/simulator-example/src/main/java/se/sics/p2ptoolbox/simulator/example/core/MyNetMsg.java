/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
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

package se.sics.p2ptoolbox.simulator.example.core;

import java.util.UUID;
import se.sics.kompics.network.Transport;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class MyNetMsg {
    private static abstract class MyBasicNetMsg<C extends Object> extends BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, C> {
        public MyBasicNetMsg(DecoratedAddress src, DecoratedAddress dst, C content) {
            super(new DecoratedHeader(src, dst, Transport.UDP), content);
        }
        
    }
    
    public static class NetPing extends MyBasicNetMsg<Ping> {
        public NetPing(DecoratedAddress src, DecoratedAddress dst, UUID pingId) {
            super(src, dst, new Ping(pingId));
        }
    }
    
    public static class NetPong extends MyBasicNetMsg<Pong> {
        public NetPong(DecoratedAddress src, DecoratedAddress dst, UUID pingId) {
            super(src, dst, new Pong(pingId));
        }
    }
}
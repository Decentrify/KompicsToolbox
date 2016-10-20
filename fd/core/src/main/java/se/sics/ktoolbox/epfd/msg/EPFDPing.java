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
package se.sics.ktoolbox.epfd.msg;

import java.util.UUID;
import se.sics.ktoolbox.epfd.event.EPFDEvent;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class EPFDPing implements EPFDEvent {
    public final Identifier msgId;
    public final UUID timeoutId;
    public final long ts;
    
    public EPFDPing(Identifier msgId, UUID timeoutId, long ts) {
        this.msgId = msgId;
        this.timeoutId = timeoutId;
        this.ts = ts;
    }
    
    public EPFDPing(UUID timeoutId, long ts) {
        this(BasicIdentifiers.msgId(), timeoutId, ts);
    }
    
    @Override
    public String toString() {
        return "EPFDPing<" + msgId + ">";
    }
    
    public EPFDPong pong() {
        return new EPFDPong(this);
    }

    @Override
    public Identifier getId() {
        return msgId;
    }
}

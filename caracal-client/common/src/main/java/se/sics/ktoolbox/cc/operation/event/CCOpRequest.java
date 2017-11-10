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
package se.sics.ktoolbox.cc.operation.event;

import se.sics.caracaldb.Key;
import se.sics.caracaldb.operations.CaracalOp;
import se.sics.kompics.Direct;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.util.identifiable.BasicBuilders.UUIDBuilder;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierFactory;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistry;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CCOpRequest extends Direct.Request<CCOperationIndication> implements CCOperationEvent {

    public final CaracalOp opReq;
    public final Key forwardTo;
    public final Identifier eventId;

    public CCOpRequest(CaracalOp opReq, Key forwardTo) {
        this.opReq = opReq;
        this.forwardTo = forwardTo;
        //TODO Alex - temp fix till i remove this
        IdentifierFactory factory = IdentifierRegistry.lookup(BasicIdentifiers.Values.EVENT.toString());
        eventId = factory.id(new UUIDBuilder(opReq.id));
    }

    @Override
    public String toString() {
        return opReq.toString();
    }

    public CCOpResponse success(CaracalOp opResp) {
        return new CCOpResponse(this, opResp);
    }

    public CCOpTimeout timeout() {
        return new CCOpTimeout(this);
    }

    @Override
    public Identifier getId() {
        return eventId;
    }
}

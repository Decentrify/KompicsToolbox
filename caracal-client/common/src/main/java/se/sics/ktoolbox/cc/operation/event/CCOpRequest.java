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
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CCOpRequest extends Direct.Request<CCOperationIndication> implements CCOperationEvent {

    public final Identifier id;
    public final CaracalOp opReq;
    public final Key forwardTo;

    public CCOpRequest(CaracalOp opReq, Key forwardTo) {
        this.id = UUIDIdentifier.randomId();
        this.opReq = opReq;
        this.forwardTo = forwardTo;
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
        return id;
    }
}

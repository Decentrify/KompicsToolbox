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
package se.sics.p2ptoolbox.caracalclient.heartbeat.op;

import java.util.Set;
import java.util.UUID;
import se.sics.caracaldb.operations.CaracalOp;
import se.sics.caracaldb.operations.RangeQuery;
import se.sics.caracaldb.operations.ResponseCode;
import se.sics.p2ptoolbox.caracalclient.common.op.CCOpEvent;
import se.sics.p2ptoolbox.caracalclient.common.op.CCOpFailed;
import se.sics.p2ptoolbox.caracalclient.common.op.CCOpManager;
import se.sics.p2ptoolbox.caracalclient.common.op.CCOperation;
import se.sics.p2ptoolbox.caracalclient.heartbeat.msg.CCOverlaySample;
import se.sics.p2ptoolbox.caracalclient.heartbeat.util.CCValueFactory;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CCOverlaySampleOp implements CCOperation {

    private final UUID opId;
    private final CCOpManager opMngr;
    private final CCOverlaySample.Request req;

    private UUID pendingResp;
    private RangeQuery.Request range;

    public CCOverlaySampleOp(UUID opId, CCOpManager opMngr, CCOverlaySample.Request req, RangeQuery.Request range) {
        this.opId = opId;
        this.opMngr = opMngr;
        this.req = req;
        this.range = range;
        this.pendingResp = null;
    }

    @Override
    public UUID getId() {
        return opId;
    }

    @Override
    public void start() {
        this.pendingResp = range.id;
        opMngr.send(new CCOpEvent.Request(range, range.initRange.begin));
    }

    @Override
    public boolean ownResp(UUID respId) {
        return (pendingResp == null ? false : pendingResp.equals(respId));
    }

    @Override
    public void handle(CaracalOp resp) {
        pendingResp = null;
        if (resp instanceof RangeQuery.Response) {
            RangeQuery.Response rangeResp = (RangeQuery.Response)resp;
            if(rangeResp.code.equals(ResponseCode.SUCCESS)) {
                Set<DecoratedAddress> sample = CCValueFactory.getHeartbeatValue(rangeResp.data.values());
                opMngr.completed(opId, req, new CCOverlaySample.Response(req.overlay, sample));
                return;
            } else {
                opMngr.completed(opId, req, new CCOpFailed.DirectResponse(req));
                return;
            }
        }
        throw new RuntimeException("unexpected caracal response" + resp);
    }

    @Override
    public void fail() {
        pendingResp = null;
        opMngr.completed(opId, req, new CCOpFailed.DirectResponse(req));
    }
}

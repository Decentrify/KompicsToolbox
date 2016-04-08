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
package se.sics.ktoolbox.cc.heartbeat.op;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.caracaldb.operations.CaracalOp;
import se.sics.caracaldb.operations.RangeQuery;
import se.sics.caracaldb.operations.ResponseCode;
import se.sics.ktoolbox.cc.heartbeat.event.CCOverlaySample;
import se.sics.ktoolbox.cc.heartbeat.util.CCValueFactory;
import se.sics.ktoolbox.cc.op.CCOpManager;
import se.sics.ktoolbox.cc.op.CCOperation;
import se.sics.ktoolbox.cc.operation.event.CCOpRequest;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CCOverlaySampleOp implements CCOperation {
    private final static Logger LOG = LoggerFactory.getLogger("LOGIC_TODO");

    private final UUID opId;
    private final KAddress self;
    private final CCOpManager opMngr;
    private final CCOverlaySample.Request req;

    private UUID pendingResp;
    private RangeQuery.Request range;

    public CCOverlaySampleOp(UUID opId, KAddress self, CCOpManager opMngr, 
            CCOverlaySample.Request req, RangeQuery.Request range) {
        this.opId = opId;
        this.self = self;
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
        opMngr.send(new CCOpRequest(range, range.initRange.begin));
    }

    @Override
    public boolean ownResp(UUID respId) {
        return (pendingResp == null ? false : pendingResp.equals(respId));
    }

    @Override
    public void handle(CaracalOp resp) {
        pendingResp = null;
        if (resp instanceof RangeQuery.Response) {
            RangeQuery.Response rangeResp = (RangeQuery.Response) resp;
            if (rangeResp.code.equals(ResponseCode.SUCCESS)) {
                List<KAddress> sample = CCValueFactory.extractHeartbeatSrc(rangeResp.data.values());
                sample.remove(self);
                opMngr.completed(opId, req, req.answer(sample));
                return;
            } else {
                //TODO Alex - CaracalRead failed - answer with empty and wait for retry - later answer with something to issue a retry
                LOG.warn("caracal answer:{} - delivering empty answer - hope to succeed in the future", rangeResp.code);
                opMngr.completed(opId, req, req.answer(new ArrayList<KAddress>()));
                return;
            }
        }
        throw new RuntimeException("unexpected caracal response" + resp);
    }

    @Override
    public void fail() {
        pendingResp = null;
        //TODO Alex - CaracalRead failed - answer with empty and wait for retry - later answer with something to issues a retry
        LOG.warn("caracal answer failed - delivering empty answer - hope to succeed in the future");
        opMngr.completed(opId, req, req.answer(new ArrayList<KAddress>()));
    }
}

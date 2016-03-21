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

import java.util.UUID;
import se.sics.caracaldb.operations.CaracalOp;
import se.sics.caracaldb.operations.PutRequest;
import se.sics.ktoolbox.cc.op.CCOpManager;
import se.sics.ktoolbox.cc.op.CCOperation;
import se.sics.ktoolbox.cc.operation.event.CCOpRequest;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CCHeartbeatOp implements CCOperation {

    private final UUID opId;
    private final CCOpManager opMngr;

    private UUID pendingResp;
    private PutRequest put;
    
    public CCHeartbeatOp(UUID opId, CCOpManager opMngr, PutRequest put) {
        this.opId = opId;
        this.opMngr = opMngr;
        this.put = put;
        this.pendingResp = null;
    }

    @Override
    public UUID getId() {
        return opId;
    }

    @Override
    public void start() {
        this.pendingResp = put.id;
        opMngr.send(new CCOpRequest(put, put.key));
    }

    @Override
    public boolean ownResp(UUID respId) {
        return (pendingResp == null ? false : pendingResp.equals(respId));
    }

    @Override
    public void handle(CaracalOp resp) {
        pendingResp = null;
        opMngr.completed(opId, null);
    }

    @Override
    public void fail() {
        pendingResp = null;
        opMngr.completed(opId, null);
    }
}
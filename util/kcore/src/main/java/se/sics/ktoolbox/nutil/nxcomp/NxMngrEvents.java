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
package se.sics.ktoolbox.nutil.nxcomp;

import se.sics.kompics.Direct;
import se.sics.kompics.util.Identifiable;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.network.portsv2.SelectableEventV2;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NxMngrEvents {
  public static final String EVENT_TYPE = "NX_MNGR_EVENT";
  
  public static interface Base extends SelectableEventV2, Identifiable {
    public Identifier stackId();
  }

  public static class CreateReq extends Direct.Request<CreateAck>
    implements Base {

    public final Identifier eventId;
    public final Identifier stackId;
    public final NxStackInit stackInit;

    public CreateReq(Identifier eventId, Identifier stackId, NxStackInit stackInit) {
      this.eventId = eventId;
      this.stackId = stackId;
      this.stackInit = stackInit;
    }

    @Override
    public Identifier getId() {
      return eventId;
    }

    public CreateAck ack() {
      return new CreateAck(this);
    }

    @Override
    public String toString() {
      return "CreateReq{" + "eventId=" + eventId + ", compId=" + stackId + '}';
    }

    @Override
    public String eventType() {
      return EVENT_TYPE;
    }
    
    @Override
    public Identifier stackId() {
      return stackId;
    }
  }

  public static class CreateAck implements Direct.Response, Base {

    public final CreateReq req;

    public CreateAck(CreateReq req) {
      this.req = req;
    }

    @Override
    public Identifier getId() {
      return req.getId();
    }

    @Override
    public String toString() {
      return "CreateAck{" + "eventId=" + req.eventId + ", compId=" + req.stackId + '}';
    }
    
    @Override
    public String eventType() {
      return EVENT_TYPE;
    }
    
    @Override
    public Identifier stackId() {
      return req.stackId;
    }
  }

  public static class KillReq extends Direct.Request<KillAck> implements Base {
    public final CreateReq req;

    public KillReq(CreateReq req) {
      this.req = req;
    }

    @Override
    public Identifier getId() {
      return req.getId();
    }

    @Override
    public String toString() {
      return "KillReq{" + "eventId=" + req.getId() + ", compId=" + req.stackId() + '}';
    }

    public KillAck ack() {
      return new KillAck(this);
    }
    
    @Override
    public String eventType() {
      return EVENT_TYPE;
    }
    
    @Override
    public Identifier stackId() {
      return req.stackId();
    }
  }

  public static class KillAck implements Direct.Response, Base {

    public final KillReq req;

    public KillAck(KillReq req) {
      this.req = req;
    }

    @Override
    public Identifier getId() {
      return req.getId();
    }

    @Override
    public String toString() {
      return "KillAck{" + "eventId=" + req.getId() + ", compId=" + req.stackId() + '}';
    }
    
    @Override
    public String eventType() {
      return EVENT_TYPE;
    }
    
    @Override
    public Identifier stackId() {
      return req.stackId();
    }
  }
}

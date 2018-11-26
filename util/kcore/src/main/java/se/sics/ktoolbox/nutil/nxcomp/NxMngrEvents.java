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

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Direct;
import se.sics.kompics.Init;
import se.sics.kompics.util.Identifiable;
import se.sics.kompics.util.Identifier;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NxMngrEvents {

  public static class CreateReq<D extends ComponentDefinition> extends Direct.Request<CreateAck>
    implements Identifiable {

    public final Identifier eventId;
    public final Identifier compId;
    public final Init<D> compInit;

    public CreateReq(Identifier eventId, Identifier compId, Init<D> compInit) {
      this.eventId = eventId;
      this.compId = compId;
      this.compInit = compInit;
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
      return "CreateReq{" + "eventId=" + eventId + ", compId=" + compId + '}';
    }
  }

  public static class CreateAck implements Direct.Response, Identifiable {

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
      return "CreateAck{" + "eventId=" + req.eventId + ", compId=" + req.compId + '}';
    }
  }

  public static class KillReq extends Direct.Request<KillAck> implements Identifiable {

    public final Identifier eventId;
    public final Identifier compId;

    public KillReq(Identifier eventId, Identifier compId) {
      this.eventId = eventId;
      this.compId = compId;
    }

    @Override
    public Identifier getId() {
      return eventId;
    }

    @Override
    public String toString() {
      return "KillReq{" + "eventId=" + eventId + ", compId=" + compId + '}';
    }
    
    public KillAck ack() {
      return new KillAck(this);
    }
  }

  public static class KillAck implements Direct.Response, Identifiable {

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
      return "KillAck{" + "eventId=" + req.eventId + ", compId=" + req.compId + '}';
    }
  }
}

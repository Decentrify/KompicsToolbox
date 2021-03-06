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
package se.sics.ktoolbox.netmngr.nxnet;

import se.sics.kompics.Direct;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.netmngr.NetMngrEvent;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NxNetUnbind {

  public static class Request extends Direct.Request<Response> implements NetMngrEvent {

    public final Identifier eventId;
    public final int port;

    public Request(Identifier eventId, int port) {
      this.eventId = eventId;
      this.port = port;
    }

    @Override
    public Identifier getId() {
      return eventId;
    }

    @Override
    public String toString() {
      return "NxNetUnbindReq<" + eventId + ">";
    }

    public Response answer() {
      return new Response(this);
    }
  }

  public static class Response implements Direct.Response, NetMngrEvent {

    public final Request req;

    public Response(Request req) {
      this.req = req;
    }

    @Override
    public Identifier getId() {
      return req.getId();
    }

    @Override
    public String toString() {
      return "NxNetUnbindResp<" + req.getId() + ">";
    }
  }

  public static class LedbatRequest extends Direct.Request<LedbatResponse> implements NetMngrEvent {

    public final Identifier eventId;
    public final int port;

    public LedbatRequest(Identifier eventId, int port) {
      this.eventId = eventId;
      this.port = port;
    }

    @Override
    public Identifier getId() {
      return eventId;
    }

    @Override
    public String toString() {
      return "NxNetLedbatUnbindReq<" + eventId + ">";
    }

    public LedbatResponse answer() {
      return new LedbatResponse(this);
    }
  }

  public static class LedbatResponse implements Direct.Response, NetMngrEvent {

    public final LedbatRequest req;

    public LedbatResponse(LedbatRequest req) {
      this.req = req;
    }

    @Override
    public Identifier getId() {
      return req.getId();
    }

    @Override
    public String toString() {
      return "NxNetLedbatUnbindResp<" + req.getId() + ">";
    }
  }
}

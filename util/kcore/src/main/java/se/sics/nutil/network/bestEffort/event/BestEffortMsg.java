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
package se.sics.nutil.network.bestEffort.event;

import se.sics.ktoolbox.util.identifiable.Identifiable;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.nutil.ContentWrapper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BestEffortMsg {

  public static abstract class Base<C extends Identifiable> implements ContentWrapper<C>, Identifiable {

    public final C content;

    public Base(C content) {
      this.content = content;
    }

    @Override
    public C getWrappedContent() {
      return content;
    }

    @Override
    public Identifier getId() {
      return content.getId();
    }
  }

  public static class Request<C extends Identifiable> extends Base<C> {

    public final int retries;
    public final long rto;

    public Request(C content, int retries, long rto) {
      super(content);
      this.retries = retries;
      this.rto = rto;
    }

    public Timeout timeout() {
      return new Timeout(this, content);
    }

    @Override
    public String toString() {
      return "BE_Request<" + content.toString() + ">";
    }
  }

  public static class Timeout<C extends Identifiable> extends Base<C> {

    public final Request req;

    private Timeout(Request req, C content) {
      super(content);
      this.req = req;
    }

    @Override
    public String toString() {
      return "BE_Timeout<" + content.toString() + ">";
    }
  }

  public static class Cancel<C extends Identifiable> extends Base<C> {

    public Cancel(C content) {
      super(content);
    }
  }
}

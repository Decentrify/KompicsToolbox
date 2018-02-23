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
package se.sics.ktoolbox.util.mngrcomp;

import java.util.UUID;
import se.sics.kompics.Init;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.util.Identifiable;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class MngrEvents {

  public static abstract class Base implements KompicsEvent, Identifiable {

    public final Identifier eventId;

    public Base(Identifier eventId) {
      this.eventId = eventId;
    }

    @Override
    public Identifier getId() {
      return eventId;
    }
  }

  public static class Create extends Base {

    public final Init init;
    public final int compType;

    public Create(int compType, Init init) {
      super(BasicIdentifiers.eventId());
      this.compType = compType;
      this.init = init;
    }

    public Created success(UUID compId) {
      return new Created(eventId, compId);
    }
  }

  public static class Created extends Base {

    public final UUID compId;

    public Created(Identifier eventId, UUID compId) {
      super(eventId);
      this.compId = compId;
    }
  }

  public static class Kill extends Base {

    public final UUID compId;

    public Kill(UUID compId) {
      super(BasicIdentifiers.eventId());
      this.compId = compId;
    }
    
    public Killed ack() {
      return new Killed(eventId, compId);
    }
  }

  public static class Killed extends Base {

    public final UUID compId;

    public Killed(Identifier eventId, UUID compId) {
      super(eventId);
      this.compId = compId;
    }
    
    public Killed(UUID compId) {
      this(BasicIdentifiers.eventId(), compId);
    }
  }
}

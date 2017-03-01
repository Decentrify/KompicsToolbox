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
package se.sics.ktoolbox.nutil.fsm.events;

import se.sics.ktoolbox.nutil.fsm.api.FSMEvent;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class Event1 {

  public static class E1 implements FSMEvent {

    public final Identifier baseId;

    public E1(Identifier baseId) {
      this.baseId = baseId;
    }

    @Override
    public Identifier getBaseId() {
      return baseId;
    }

    @Override
    public String toString() {
      return "E1<" + baseId + ">";
    }
  }

  public static class E2 implements FSMEvent {

    public final Identifier baseId;

    public E2(Identifier baseId) {
      this.baseId = baseId;
    }

    @Override
    public Identifier getBaseId() {
      return baseId;
    }

    @Override
    public String toString() {
      return "E2<" + baseId + ">";
    }
  }

  public static class E3 implements FSMEvent {

    public final Identifier baseId;

    public E3(Identifier baseId) {
      this.baseId = baseId;
    }

    @Override
    public Identifier getBaseId() {
      return baseId;
    }

    @Override
    public String toString() {
      return "E3<" + baseId + ">";
    }
  }

  public static class E4 implements FSMEvent {

    public final Identifier baseId;

    public E4(Identifier baseId) {
      this.baseId = baseId;
    }

    @Override
    public Identifier getBaseId() {
      return baseId;
    }

    @Override
    public String toString() {
      return "E4<" + baseId + ">";
    }
  }
}

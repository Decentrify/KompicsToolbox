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

import se.sics.kompics.Direct;
import se.sics.ktoolbox.nutil.fsm.api.FSMEvent;
import se.sics.ktoolbox.util.identifiable.Identifier;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class Event2 {
  public static class Req extends Direct.Request<Resp> implements FSMEvent {
    public final Identifier baseId;
    
    public Req(Identifier baseId) {
      this.baseId = baseId;
    }
    
    @Override
    public Identifier getBaseId() {
      return baseId;
    }

    public Resp answer() {
      return new Resp(baseId);
    }
    
    @Override
    public String toString() {
      return "Req<" + baseId + ">";
    }
  }
  
  public static class Resp implements Direct.Response, FSMEvent {
    public final Identifier baseId;
    
    public Resp(Identifier baseId) {
      this.baseId = baseId;
    }
    
    @Override
    public Identifier getBaseId() {
      return baseId;
    }

    @Override
    public String toString() {
      return "Resp<" + baseId + ">";
    }
  }
}

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
package se.sics.ktoolbox.nledbat.event.external;

import se.sics.kompics.KompicsEvent;
import se.sics.kompics.util.Identifiable;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NLedbatMngrEvents {
  public static abstract class Base implements KompicsEvent, Identifiable {
    public final Identifier eventId;
    public final Identifier dataId;
    public final Identifier senderId;
    public final Identifier receiverId;
    
    public Base(Identifier eventId, Identifier dataId, Identifier senderId, Identifier receiverId) {
      this.eventId = eventId;
      this.dataId = dataId;
      this.senderId = senderId;
      this.receiverId = receiverId;
    }
    
    @Override
    public Identifier getId() {
      return eventId;
    }
  }
  
  public static class CreateSender extends Base {
    public CreateSender(Identifier dataId, Identifier senderId, Identifier receiverId) {
      super(BasicIdentifiers.eventId(), dataId, senderId, receiverId);
    }
  }
  
  public static class CreateSenderSuccess extends Base {
    public CreateSenderSuccess(Identifier dataId, Identifier senderId, Identifier receiverId) {
      super(BasicIdentifiers.eventId(), dataId, senderId, receiverId);
    }
  }
  
  public static class CreateReceiver extends Base {
    public CreateReceiver(Identifier dataId, Identifier senderId, Identifier receiverId) {
      super(BasicIdentifiers.eventId(), dataId, senderId, receiverId);
    }
  }
  
  public static class CreateReceiverSuccess extends Base {
    public CreateReceiverSuccess(Identifier dataId, Identifier senderId, Identifier receiverId) {
      super(BasicIdentifiers.eventId(), dataId, senderId, receiverId);
    }
  }
  
  public static class KillSender extends Base {

    public KillSender(Identifier dataId, Identifier senderId, Identifier receiverId) {
      super(BasicIdentifiers.eventId(), dataId, senderId, receiverId);
    }
  }
  
  public static class KillSenderAck extends Base {

    public KillSenderAck(Identifier dataId, Identifier senderId, Identifier receiverId) {
      super(BasicIdentifiers.eventId(), dataId, senderId, receiverId);
    }
  }
  
  public static class KillReceiver extends Base {

    public KillReceiver(Identifier dataId, Identifier senderId, Identifier receiverId) {
      super(BasicIdentifiers.eventId(), dataId, senderId, receiverId);
    }
  }
  
  public static class KillReceiverAck extends Base {

    public KillReceiverAck(Identifier dataId, Identifier senderId, Identifier receiverId) {
      super(BasicIdentifiers.eventId(), dataId, senderId, receiverId);
    }
  }
}

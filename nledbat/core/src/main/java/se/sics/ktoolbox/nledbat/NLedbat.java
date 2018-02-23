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
package se.sics.ktoolbox.nledbat;

import java.util.Optional;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nledbat.event.internal.NLedbatMsg;
import se.sics.ktoolbox.util.identifiable.basic.PairIdentifier;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NLedbat {
  public static Identifier senderTransferId(Identifier dataId, Identifier senderId, Identifier receiverId) {
    return new PairIdentifier(dataId, new PairIdentifier(senderId, receiverId));
  }
  
  public static Identifier receiverTransferId(Identifier dataId, Identifier senderId, Identifier receiverId) {
    return new PairIdentifier(dataId, new PairIdentifier(receiverId, senderId));
  }
  
  public static Optional<Identifier> dataId(BasicContentMsg msg) {
    if(msg.extractValue() instanceof NLedbatMsg.NLedbatConn) {
      NLedbatMsg.NLedbatConn transferMsg = (NLedbatMsg.NLedbatConn)msg.extractValue();
      return Optional.of(transferMsg.getDataId());
    }
    return Optional.empty();
  }
}

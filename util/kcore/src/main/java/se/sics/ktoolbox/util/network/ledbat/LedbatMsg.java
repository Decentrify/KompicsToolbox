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
 * along with this program; if not, loss to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.ktoolbox.util.network.ledbat;

import java.util.List;
import se.sics.kompics.util.Identifiable;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.network.portsv2.SelectableMsgV2;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public interface LedbatMsg extends Identifiable, SelectableMsgV2 {

  public static final String MSG_TYPE = "LEDBAT_MSG";

  public static abstract class Basic implements LedbatMsg {

    public final Identifier id;

    public Basic(Identifier id) {
      this.id = id;
    }

    @Override
    public Identifier getId() {
      return id;
    }

    @Override
    public String eventType() {
      return MSG_TYPE;
    }
  }

  public static class Datum<D extends Identifiable<Identifier>> extends Basic {

    public final D datum;
    public long senderT1;
    public long receiverT1;

    public Datum(Identifier msgId, D datum) {
      super(msgId);
      this.datum = datum;
    }

    public void setSenderT1(long senderT1) {
      this.senderT1 = senderT1;
    }

    public void setReceiverT1(long receiverT1) {
      this.receiverT1 = receiverT1;
    }
    
    @Override
    public String toString() {
      return "LowLedbatMsgDatum{" + "datumId=" + datum.getId() + '}';
    }
  }

  public static class MultiAck extends Basic {

    public final BatchAckVal acks;

    public MultiAck(Identifier id, BatchAckVal acks) {
      super(id);
      this.acks = acks;
    }

    @Override
    public String toString() {
      return "LowLedbatMsgMultiAck{" + "datumId=" + getId() + '}';
    }
  }

  public static class AckVal {

    public final Identifier msgId;
    public long st1;
    public long rt1;
    public long rt2;

    public AckVal(Identifier msgId) {
      this.msgId = msgId;
    }

    public void setSt1(long st1) {
      this.st1 = st1;
    }

    public void setRt1(long rt1) {
      this.rt1 = rt1;
    }
    
    public void setRt2(long rt2) {
      this.rt2 = rt2;
    }
  }

  public static class BatchAckVal {

    public final List<AckVal> acks;
    public long rt3;
    public long st2;

    public BatchAckVal(List<AckVal> acks) {
      this.acks = acks;
    }

    public void setRt3(long rt3) {
      this.rt3 = rt3;
    }

    public void setSt2(long st2) {
      this.st2 = st2;
    }
  }
}

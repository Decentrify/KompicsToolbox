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
package se.sics.ktoolbox.nutil.network.ledbat;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.util.LinkedList;
import java.util.List;
import se.sics.kompics.network.netty.serialization.Serializer;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.kompics.util.Identifiable;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.nxcomp.NxStackId;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.IdentifierRegistryV2;
import se.sics.ktoolbox.util.identifiable.basic.SimpleByteIdFactory;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LedbatMsgSerializer {

  final static Identifier RECEIVER
    = LedbatMsg.StackType.RECEIVER.getId(new SimpleByteIdFactory(java.util.Optional.empty(), 0));
  final static Identifier SENDER
    = LedbatMsg.StackType.SENDER.getId(new SimpleByteIdFactory(java.util.Optional.empty(), 0));

  public static class Datum implements Serializer {

    private final int id;
    private final Class msgIdType;

    public Datum(int id) {
      this.id = id;
      this.msgIdType = IdentifierRegistryV2.idType(BasicIdentifiers.Values.MSG);
    }

    @Override
    public int identifier() {
      return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
      LedbatMsg.Datum obj = (LedbatMsg.Datum) o;
      Serializers.lookupSerializer(msgIdType).toBinary(obj.getId(), buf);
      Serializers.toBinary(obj.dataStreamId.id, buf);
      Serializers.toBinary(obj.datum, buf);
      long sendTime = System.currentTimeMillis();
      buf.writeLong(sendTime);
    }

    @Override
    public LedbatMsg.Datum fromBinary(ByteBuf buf, Optional<Object> hint) {
      Identifier msgId = (Identifier) Serializers.lookupSerializer(msgIdType).fromBinary(buf, hint);
      Identifier baseDataStreamId = (Identifier)Serializers.fromBinary(buf, hint);
      NxStackId dataStreamId = new NxStackId(RECEIVER, baseDataStreamId);
      Identifiable datum = (Identifiable) Serializers.fromBinary(buf, hint);
      LedbatMsg.Datum msg = new LedbatMsg.Datum(msgId, dataStreamId, datum);
      msg.setSenderT1(buf.readLong());
      msg.setReceiverT1(System.currentTimeMillis());
      return msg;
    }
  }

  public static class MultiAck implements Serializer {

    private final int id;
    private final Class msgIdType;

    public MultiAck(int id) {
      this.id = id;
      this.msgIdType = IdentifierRegistryV2.idType(BasicIdentifiers.Values.MSG);
    }

    @Override
    public int identifier() {
      return id;
    }

    @Override
    public void toBinary(Object o, ByteBuf buf) {
      LedbatMsg.MultiAck obj = (LedbatMsg.MultiAck) o;
      Serializers.lookupSerializer(msgIdType).toBinary(obj.getId(), buf);
      Serializers.toBinary(obj.dataStreamId.id, buf);
      buf.writeLong(System.currentTimeMillis());
      buf.writeInt(obj.acks.acks.size());
      for (LedbatMsg.AckVal ack : obj.acks.acks) {
        Serializers.toBinary(ack.msgId, buf);
        buf.writeLong(ack.rt1);
        buf.writeLong(ack.rt2);
        buf.writeLong(ack.st1);
      }
    }

    @Override
    public LedbatMsg.MultiAck fromBinary(ByteBuf buf, Optional<Object> hint) {
      Identifier msgId = (Identifier) Serializers.lookupSerializer(msgIdType).fromBinary(buf, hint);
      Identifier baseDataStreamId = (Identifier)Serializers.fromBinary(buf, hint);
      NxStackId dataStreamId = new NxStackId(SENDER, baseDataStreamId);
      List<LedbatMsg.AckVal> acks = new LinkedList<>();
      LedbatMsg.BatchAckVal batch = new LedbatMsg.BatchAckVal(acks);
      batch.setRt3(buf.readLong());
      batch.setSt2(System.currentTimeMillis());
      int nrAcks = buf.readInt();
      for (int i = 0; i < nrAcks; i++) {
        Identifier ackMsgId = (Identifier) Serializers.fromBinary(buf, hint);
        LedbatMsg.AckVal ack = new LedbatMsg.AckVal(ackMsgId);
        ack.setRt1(buf.readLong());
        ack.setRt2(buf.readLong());
        ack.setSt1(buf.readLong());
        acks.add(ack);
      }
      return new LedbatMsg.MultiAck(msgId, dataStreamId, batch);
    }
  }
}

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
package se.sics.ktoolbox.nledbat.event.internal;

import java.util.Map;
import se.sics.kompics.PatternExtractor;
import se.sics.kompics.util.Identifiable;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nledbat.util.OneWayDelay;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NLedbatMsg {
  public static interface NLedbatConn {
    public Identifier getDataId();
  }
  
  public static class Data<P extends Identifiable> implements PatternExtractor, NLedbatConn {

    public final Identifier dataId;
    public final P data;
    public final OneWayDelay dataDelay;

    public Data(Identifier dataId, P payload) {
      this.dataId = dataId;
      this.data = payload;
      this.dataDelay = new OneWayDelay();
    }

    @Override
    public Object extractPattern() {
      return data.getClass();
    }

    @Override
    public Object extractValue() {
      return data;
    }

    public Ack answer() {
      return new Ack(data.getId(), dataId, dataDelay);
    }

    @Override
    public Identifier getDataId() {
      return dataId;
    }

  }

  public static class Ack implements Identifiable, NLedbatConn {

    public final Identifier eventId;
    public final Identifier dataId;
    public final OneWayDelay dataDelay;
    public final OneWayDelay ackDelay;

    public Ack(Identifier eventId, Identifier dataId, OneWayDelay data) {
      this.eventId = eventId;
      this.dataId = dataId;
      this.dataDelay = data;
      this.ackDelay = new OneWayDelay();
    }

    @Override
    public Identifier getId() {
      return eventId;
    }

    @Override
    public Identifier getDataId() {
      return dataId;
    }
  }

  public static class BulkAck {

    public final Map<Identifier, OneWayDelay> data;
    public final OneWayDelay ack;

    public BulkAck(Map<Identifier, OneWayDelay> data) {
      this.data = data;
      this.ack = new OneWayDelay();
    }
  }
}

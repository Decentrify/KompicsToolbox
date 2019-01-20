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
package se.sics.ktoolbox.nutil.network.ledbat;

import java.util.List;
import se.sics.kompics.PortType;
import se.sics.kompics.util.Identifiable;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.nutil.network.portsv2.SelectableEventV2;
import se.sics.ktoolbox.nutil.nxcomp.NxStackId;
import se.sics.ktoolbox.util.network.KContentMsg;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LedbatStatus {

  public static abstract class Event implements Identifiable, SelectableEventV2 {

    public static final String EVENT_TYPE = "LOW_LEDBAT_EVENT";
    public final Identifier id;
    public final NxStackId dataStreamId;

    public Event(Identifier id, NxStackId dataStreamId) {
      this.id = id;
      this.dataStreamId = dataStreamId;
    }

    @Override
    public Identifier getId() {
      return id;
    }

    @Override
    public String eventType() {
      return EVENT_TYPE;
    }
  }

  public static class Timeout extends Event {
    public final List<KContentMsg<?,?,LedbatMsg.Datum>> timedOut;
    public final int maxInFlight;
    public Timeout(Identifier id, NxStackId dataStreamId, List<KContentMsg<?,?,LedbatMsg.Datum>> timedOut, int maxInFlight) {
      super(id, dataStreamId);
      this.timedOut = timedOut;
      this.maxInFlight = maxInFlight;
    }
  }
  
  public static class Ack extends Event {
    public final List<KContentMsg<?,?,LedbatMsg.Datum>> acked;
    public final int maxInFlight;
    public Ack(Identifier id, NxStackId dataStreamId, List<KContentMsg<?,?,LedbatMsg.Datum>> acked, int maxInFlight) {
      super(id, dataStreamId);
      this.acked = acked;
      this.maxInFlight = maxInFlight;
    }
  }
  

  public static class Port extends PortType {
    {
      indication(Timeout.class);
      indication(Ack.class);
    }
  }

}

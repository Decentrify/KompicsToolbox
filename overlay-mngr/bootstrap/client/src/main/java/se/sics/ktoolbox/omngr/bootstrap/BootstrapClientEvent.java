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
package se.sics.ktoolbox.omngr.bootstrap;

import java.util.List;
import se.sics.kompics.Direct;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.util.Identifiable;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BootstrapClientEvent {
  public static class Start extends Direct.Request<Sample> implements Identifiable {
    public final Identifier eventId;
    public final OverlayId overlay;
    
    public Start(OverlayId overlay) {
      this.eventId = BasicIdentifiers.eventId();
      this.overlay = overlay;
    }

    @Override
    public Identifier getId() {
      return eventId;
    }
    
    public Sample sample(List<KAddress> sample) {
      return new Sample(this, sample);
    }
  }
  
  public static class Sample implements Identifiable, Direct.Response {
    public final Start req;
    public final List<KAddress> sample;
    
    public Sample(Start req, List<KAddress> sample) {
      this.req = req;
      this.sample = sample;
    }
    
    @Override
    public Identifier getId() {
      return req.getId();
    }
  }
  
  public static class Stop implements Identifiable, KompicsEvent {
    public final Identifier eventId;
    public final OverlayId overlay;

    public Stop(OverlayId overlay) {
      this.eventId = BasicIdentifiers.eventId();
      this.overlay = overlay;
    }

    @Override
    public Identifier getId() {
      return eventId;
    }
  }
}

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
package se.sics.ktoolbox.gradient.event;

import java.util.List;
import se.sics.kompics.id.Identifier;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.identifiable.overlay.OverlayId;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.other.Container;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TGradientSample<C extends Object> extends GradientSample<C>{
    public final List<Container<KAddress, C>> gradientFingers;
    
    public TGradientSample(Identifier eventId, OverlayId overlayId, C selfView, 
            List<Container<KAddress, C>> gradientNeighbours, List<Container<KAddress, C>> gradientFingers) {
        super(eventId, overlayId, selfView, gradientNeighbours);
        this.gradientFingers = gradientFingers;
    }
    
    public TGradientSample(OverlayId overlayId, C selfView, 
            List<Container<KAddress, C>> gradientNeighbours, List<Container<KAddress, C>> gradientFingers) {
        this(BasicIdentifiers.eventId(), overlayId, selfView, gradientNeighbours, gradientFingers);
    }

    public List<Container<KAddress, C>> getGradientFingers() {
        return gradientFingers;
    }
    
    @Override
    public String toString() {
        return "TGradient<" + overlayId() + ">Sample<" + getId() + ">";
    }
}

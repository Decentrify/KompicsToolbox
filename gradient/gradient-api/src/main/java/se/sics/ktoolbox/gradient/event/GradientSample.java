/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
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
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.identifiable.basic.UUIDIdentifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.other.Container;

/**
 * Set of peer views published by the gradient periodically.
 *
 * Created by babbarshaer on 2015-02-26.
 */
public class GradientSample<C extends Object> implements GradientEvent {

    public final Identifier eventId;
    public final Identifier overlayId;
    public final C selfView;
    public final List<Container<KAddress, C>> gradientNeighbours;

    public GradientSample(Identifier eventId, Identifier overlayId,
            C selfView, List<Container<KAddress, C>> gradientNeighbours) {
        this.eventId = eventId;
        this.overlayId = overlayId;
        this.selfView = selfView;
        this.gradientNeighbours = gradientNeighbours;
    }

    public GradientSample(Identifier overlayId, C selfView, List<Container<KAddress, C>> gradientNeighbours) {
        this(UUIDIdentifier.randomId(), overlayId, selfView, gradientNeighbours);
    }

    public List<Container<KAddress, C>> getGradientNeighbours() {
        return gradientNeighbours;
    }

    @Override
    public Identifier getId() {
        return eventId;
    }

    @Override
    public Identifier overlayId() {
        return overlayId;
    }

    @Override
    public String toString() {
        return "Gradient<" + overlayId() + ">Sample<" + getId() + ">";
    }
}

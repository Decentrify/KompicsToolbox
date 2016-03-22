/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Croupier is free software; you can redistribute it and/or
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
package se.sics.ktoolbox.croupier.behaviour;

import com.google.common.base.Optional;
import se.sics.ktoolbox.util.network.nat.NatAwareAddress;
import se.sics.ktoolbox.util.network.nat.NatType;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdate;
import se.sics.ktoolbox.util.update.View;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CroupierObserver implements CroupierBehaviour {

    private boolean observer;
    private View view;

    public CroupierObserver() {
        this.view = null;
        this.observer = true;
    }

    @Override
    public CroupierBehaviour processView(OverlayViewUpdate.Indication viewUpdate) {
        observer = viewUpdate.observer;
        view = viewUpdate.view;
        return build();
    }

    @Override
    public Optional<View> getView() {
        return Optional.absent();
    }

    private CroupierBehaviour build() {
        if (observer || view == null) {
            return this;
        }
        return new CroupierParticipant(view);
    }
}

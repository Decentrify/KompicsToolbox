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
package se.sics.p2ptoolbox.croupier.behaviour;

import com.google.common.base.Optional;
import se.sics.ktoolbox.util.address.resolution.AddressUpdate;
import se.sics.ktoolbox.util.update.view.View;
import se.sics.ktoolbox.util.update.view.impl.OverlayView;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class CroupierObserver implements CroupierBehaviour {
    private boolean observer;
    private View view;
    private DecoratedAddress 
    @Override
    public CroupierBehaviour process(OverlayView viewUpdate) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public CroupierBehaviour process(AddressUpdate update) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}

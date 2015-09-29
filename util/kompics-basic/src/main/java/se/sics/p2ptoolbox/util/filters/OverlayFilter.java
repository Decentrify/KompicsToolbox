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

package se.sics.p2ptoolbox.util.filters;

import com.google.common.primitives.Ints;
import java.util.Arrays;
import se.sics.kompics.ChannelFilter;
import se.sics.kompics.network.Msg;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;
import se.sics.p2ptoolbox.util.traits.OverlayMember;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OverlayFilter extends ChannelFilter<Msg, Boolean> {
    private final byte[] overlayId;
    
    public OverlayFilter(byte[] overlayId) {
        super(Msg.class, true, true);
        this.overlayId = overlayId;
    }
    
    @Override
    public Boolean getValue(Msg msg) {
        if(!(msg.getHeader() instanceof DecoratedHeader)) {
            return false;
        }
        DecoratedHeader header = (DecoratedHeader) msg.getHeader();
        if(!header.hasTrait(OverlayMember.class)) {
            return false;
        }
        byte[] msgOverlayId = Ints.toByteArray(((OverlayMember)header.getTrait(OverlayMember.class)).getOverlayId());
        return Arrays.equals(overlayId, msgOverlayId);
    }
}

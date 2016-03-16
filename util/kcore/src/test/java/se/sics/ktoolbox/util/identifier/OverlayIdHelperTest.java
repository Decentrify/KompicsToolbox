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
package se.sics.ktoolbox.util.identifier;

import se.sics.ktoolbox.util.overlays.id.OverlayIdHelper;
import org.junit.Assert;
import org.junit.Test;
import se.sics.ktoolbox.util.identifiable.basic.IntIdentifier;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class OverlayIdHelperTest {
    @Test
    public void test1() {
        byte owner = 0x10;
        byte[] bId1 = new byte[]{1,2,3};
        IntIdentifier id1 = OverlayIdHelper.getIntIdentifier(owner, OverlayIdHelper.Type.CROUPIER, bId1);
        IntIdentifier id2 = OverlayIdHelper.getIntIdentifier(owner, OverlayIdHelper.Type.GRADIENT, bId1);
        IntIdentifier id3 = OverlayIdHelper.getIntIdentifier(owner, OverlayIdHelper.Type.TGRADIENT, bId1);
        Assert.assertEquals(id2, OverlayIdHelper.changeOverlayType(id1, OverlayIdHelper.Type.GRADIENT));
        Assert.assertEquals(id3, OverlayIdHelper.changeOverlayType(id2, OverlayIdHelper.Type.TGRADIENT));
        Assert.assertEquals(id1, OverlayIdHelper.changeOverlayType(id3, OverlayIdHelper.Type.CROUPIER));
    }
}

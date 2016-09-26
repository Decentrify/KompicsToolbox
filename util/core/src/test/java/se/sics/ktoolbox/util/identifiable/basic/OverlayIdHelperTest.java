///*
// * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
// * 2009 Royal Institute of Technology (KTH)
// *
// * KompicsToolbox is free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation; either version 2
// * of the License, or (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package se.sics.ktoolbox.util.identifiable.basic;
//
//import org.junit.Assert;
//import org.junit.Test;
//import se.sics.ktoolbox.util.identifiable.Identifier;
//
///**
// * @author Alex Ormenisan <aaor@kth.se>
// */
//public class OverlayIdHelperTest {
//    @Test
//    public void test1() {
//        byte owner = 0x10;
//        byte[] bId1 = new byte[]{1,2,3};
//        Identifier id1 = OverlayIdFactory.getId(owner, OverlayIdFactory.Type.CROUPIER, bId1);
//        Identifier id2 = OverlayIdFactory.getId(owner, OverlayIdFactory.Type.GRADIENT, bId1);
//        Identifier id3 = OverlayIdFactory.getId(owner, OverlayIdFactory.Type.TGRADIENT, bId1);
//        Assert.assertEquals(id2, OverlayIdFactory.changeType(id1, OverlayIdFactory.Type.GRADIENT));
//        Assert.assertEquals(id3, OverlayIdFactory.changeType(id2, OverlayIdFactory.Type.TGRADIENT));
//        Assert.assertEquals(id1, OverlayIdFactory.changeType(id3, OverlayIdFactory.Type.CROUPIER));
//    }
//}

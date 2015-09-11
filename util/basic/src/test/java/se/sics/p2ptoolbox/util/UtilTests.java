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
package se.sics.p2ptoolbox.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class UtilTests {

    @Test
    public void testInnerEnums() {
        A a1 = new A();
        A a2 = new A();
        Assert.assertEquals(0, a1.enumA.E.phase);
        Assert.assertEquals(0, a2.enumA.E.phase);
        a1.enumA.setPhase(1);
        Assert.assertEquals(1, a1.enumA.E.phase);
        Assert.assertEquals(1, a2.enumA.E.phase);
    }

    public static class A {
        public EnumA enumA = EnumA.E;

        public enum EnumA {

            E(0);

            int phase;

            EnumA(int phase) {
                this.phase = phase;
            }

            public void setPhase(int phase) {
                this.phase = phase;
            }
        }
    }
}

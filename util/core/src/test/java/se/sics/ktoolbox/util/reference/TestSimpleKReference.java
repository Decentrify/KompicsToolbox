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
package se.sics.ktoolbox.util.reference;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class TestSimpleKReference {
    @Test
    public void simpleTest() {
        KReference<Boolean> ref = KReferenceFactory.getReference(true);
        Assert.assertTrue(ref.isValid());
        Assert.assertTrue(ref.getValue().isPresent());
        Assert.assertTrue(ref.retain());
        try {
            ref.release();
        } catch (KReferenceException ex) {
            Assert.fail();
            return;
        }
        Assert.assertTrue(ref.isValid());
        Assert.assertTrue(ref.getValue().isPresent());
        try {
            //at this point ref should become invalid
            ref.release();
        } catch (KReferenceException ex) {
            Assert.fail();
            return;
        }
        Assert.assertFalse(ref.isValid());
        Assert.assertFalse(ref.getValue().isPresent());
        try {
            ref.release();
            Assert.fail();
            return;
        } catch (KReferenceException ex) {
            //expected exception
        }
        Assert.assertFalse(ref.retain());
    }
}

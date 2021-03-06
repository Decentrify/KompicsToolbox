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
package se.sics.ktoolbox.gradient.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import se.sics.ktoolbox.gradient.util.GradientContainer;
import se.sics.ktoolbox.gradient.util.GradientContainerAgeComparator;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
@RunWith(JUnit4.class)
public class GradientAgeComparatorTest {

    @Test
    public void testEqualPreference() {
        List<GradientContainer> myList;

        myList = new ArrayList<GradientContainer>();
        myList.add(new GradientContainer(null, null, 3, 1));
        myList.add(new GradientContainer(null, null, 0, 2));
        myList.add(new GradientContainer(null, null, 2, 3));
        Collections.sort(myList, new GradientContainerAgeComparator());
        Assert.assertEquals(0, myList.get(0).getAge());
        Assert.assertEquals(2, myList.get(1).getAge());
    }
}

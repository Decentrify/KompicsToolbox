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
package se.sics.p2ptoolbox.util.network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.traits.Nated;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class DecoratedAddressTest {

    @Test
    public void test() throws UnknownHostException {
        InetAddress localHost = InetAddress.getByName("localhost");
        DecoratedAddress testAdr;
        
        testAdr = new DecoratedAddress(localHost, 1234, 1);
        Assert.assertFalse(testAdr.hasTrait(Nated.class));
        
        Set<DecoratedAddress> parents = new HashSet<DecoratedAddress>();
        parents.add(new DecoratedAddress(localHost, 1234, 2));
        testAdr = DecoratedAddress.addNatedTrait(testAdr, parents);
        Assert.assertTrue(testAdr.hasTrait(Nated.class));
    }
}

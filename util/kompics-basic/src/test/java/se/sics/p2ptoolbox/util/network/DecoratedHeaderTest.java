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
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Test;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Transport;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.BasicHeader;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;
import se.sics.p2ptoolbox.util.network.impl.Route;
import se.sics.p2ptoolbox.util.traits.Forwardable;
import se.sics.p2ptoolbox.util.traits.OverlayMember;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class DecoratedHeaderTest {

    @Test
    public void test() throws UnknownHostException {
        InetAddress localHost = InetAddress.getByName("localhost");
        DecoratedHeader<DecoratedAddress> testHeader;
        DecoratedAddress src1 = new DecoratedAddress(localHost, 1234, 1);
        DecoratedAddress src2 = new DecoratedAddress(localHost, 1234, 2);
        DecoratedAddress src3 = new DecoratedAddress(localHost, 1234, 3);
        DecoratedAddress src4 = new DecoratedAddress(localHost, 1234, 4);
        DecoratedAddress src5 = new DecoratedAddress(localHost, 1234, 5);
        BasicHeader<DecoratedAddress> baseHeader = new BasicHeader(src1, src2, Transport.UDP);
        ArrayList<DecoratedAddress> route;
                
        testHeader = DecoratedHeader.addOverlayMemberTrait(baseHeader, 10);
        Assert.assertFalse(testHeader.hasTrait(Forwardable.class));
        Assert.assertTrue(testHeader.hasTrait(OverlayMember.class));

        route = new ArrayList<DecoratedAddress>();
        route.add(src1);
        route.add(src3);
        route.add(src4);
        testHeader = DecoratedHeader.addForwardableTrait(baseHeader, new Route(route));
        Assert.assertTrue(testHeader.hasTrait(Forwardable.class));
        Assert.assertFalse(testHeader.hasTrait(OverlayMember.class));
        Assert.assertEquals(src1, testHeader.getSource());
        Assert.assertEquals(src3, testHeader.getDestination());
        
        testHeader = testHeader.getTrait(Forwardable.class).next();
        Assert.assertEquals(src3, testHeader.getSource());
        Assert.assertEquals(src4, testHeader.getDestination());
        
        testHeader = testHeader.getTrait(Forwardable.class).next();
        Assert.assertEquals(src4, testHeader.getSource());
        Assert.assertEquals(src2, testHeader.getDestination());
        
        route = new ArrayList<DecoratedAddress>();
        route.add(src5);
        testHeader = testHeader.getTrait(Forwardable.class).prependRoute(route);
        Assert.assertEquals(src4, testHeader.getSource());
        Assert.assertEquals(src5, testHeader.getDestination());
        
        testHeader = testHeader.getTrait(Forwardable.class).next();
        Assert.assertEquals(src5, testHeader.getSource());
        Assert.assertEquals(src2, testHeader.getDestination());
        
        testHeader = testHeader.getTrait(Forwardable.class).next();
        Assert.assertEquals(src1, testHeader.getSource());
        Assert.assertEquals(src2, testHeader.getDestination());
    }
}

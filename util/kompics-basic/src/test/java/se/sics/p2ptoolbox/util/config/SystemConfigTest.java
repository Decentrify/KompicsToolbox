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

package se.sics.p2ptoolbox.util.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SystemConfigTest {
    @Test
    public void test1() throws UnknownHostException {
        Config config = ConfigFactory.load("application1.conf");
        SystemConfig systemConfig = new SystemConfig(config);
        
        long seed = 1234;
        Assert.assertEquals(seed, systemConfig.seed);
        
        InetAddress selfIp = InetAddress.getByName("193.10.67.178");
        int selfPort = 22222;
        int selfId = 2;
        DecoratedAddress self = new DecoratedAddress(new BasicAddress(selfIp, selfPort, selfId));
        Assert.assertEquals(self, systemConfig.self);
        
        InetAddress aggregatorIp = InetAddress.getByName("193.10.67.178");
        int aggregatorPort = 33333;
        int aggregatorId = 201;
        DecoratedAddress aggregator = new DecoratedAddress(new BasicAddress(aggregatorIp, aggregatorPort, aggregatorId));
        Assert.assertEquals(aggregator, systemConfig.aggregator);
    }
    
    @Test
    public void test2() throws UnknownHostException {
        Config config = ConfigFactory.load("application2.conf");
        SystemConfig systemConfig = new SystemConfig(config);
        
        long seed = 1234;
        Assert.assertEquals(seed, systemConfig.seed);
        
        InetAddress selfIp = InetAddress.getByName("193.10.67.178");
        int selfPort = 22222;
        int selfId = 2;
        DecoratedAddress self = new DecoratedAddress(new BasicAddress(selfIp, selfPort, selfId));
        Assert.assertEquals(self, systemConfig.self);
        Assert.assertEquals(null, systemConfig.aggregator);
    }
    
    @Test
    public void test3() throws UnknownHostException {
        Config config = ConfigFactory.load("application3.conf");
        SystemConfig systemConfig = new SystemConfig(config);
        
        long seed = 1234;
        Random rand = new Random(seed);
        Assert.assertEquals(seed, systemConfig.seed);
        
        InetAddress selfIp = InetAddress.getByName("193.10.67.178");
        int selfPort = 22222;
        int selfId = rand.nextInt();
        DecoratedAddress self = new DecoratedAddress(new BasicAddress(selfIp, selfPort, selfId));
        Assert.assertEquals(self, systemConfig.self);
        Assert.assertEquals(null, systemConfig.aggregator);
    }
}

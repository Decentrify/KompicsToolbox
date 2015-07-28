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

package se.sics.p2ptoolbox.croupier;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class CroupierConfigTest {
    @Test
    public void test1() {
        Config config = ConfigFactory.load("application1.conf");
        CroupierConfig croupierConfig = new CroupierConfig(config);
        
        Assert.assertEquals(CroupierSelectionPolicy.RANDOM, croupierConfig.policy);
        Assert.assertEquals(10, croupierConfig.viewSize);
        Assert.assertEquals(5, croupierConfig.shuffleSize);
        Assert.assertEquals(2000, croupierConfig.shufflePeriod);
        Assert.assertEquals(1000, croupierConfig.shuffleTimeout);
        Assert.assertEquals(500, croupierConfig.softMaxTemperature, 0.0001);
    }
}

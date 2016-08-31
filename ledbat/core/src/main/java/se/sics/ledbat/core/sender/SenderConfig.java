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
package se.sics.ledbat.core.sender;

import se.sics.kompics.config.Config;
import se.sics.kompics.config.TypesafeConfig;
import se.sics.ledbat.core.LedbatConfig;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SenderConfig {
    public static class Names {
        public static String MAX_DUPLICATE_ACKS = "reliableUDP.max_duplicate_acks";
    }
    /**
     * max number of times that if the same lastAckNumber is received, it should
     * be considered a loss for next seq#.
     */
    public final int maxDuplicateAcks;
    public final LedbatConfig ledbatConfig;
    
    public SenderConfig(LedbatConfig ledbatConfig) {
        this.ledbatConfig = ledbatConfig;
        Config config = TypesafeConfig.load();
        maxDuplicateAcks = config.getValue(Names.MAX_DUPLICATE_ACKS, Integer.class);
    }
}

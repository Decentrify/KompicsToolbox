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
package se.sics.ktoolbox.util.tracking.load;

import se.sics.kompics.config.Config;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class QueueLoadConfig {
    public static final long seed = 1234;

    public static class Names {
        public static String TARGET_QUEUE_DELAY = "load.queue.target_queue_delay";
        public static String MAX_QUEUE_DELAY = "load.queue.max_queue_delay";
    }
    /**
     * queue delay - low delay means lower throughput, high delay means higher latency
     */
    /**
     * we strive to stay around this targetQueueDelay - allowing us for a good throughput and an addition of this much to the latency
     */
    public final long targetQueueDelay; //ms
    /**
     * at this point we force a slow down
     */
    public final long maxQueueDelay; //ms
    
    public QueueLoadConfig(Config config) {
        this.targetQueueDelay = config.getValue(Names.TARGET_QUEUE_DELAY, Long.class);
        this.maxQueueDelay = config.getValue(Names.MAX_QUEUE_DELAY, Long.class);
    }
}

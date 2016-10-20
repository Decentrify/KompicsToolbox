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
package se.sics.ledbat.core.receiver;

import se.sics.kompics.config.Config;
import se.sics.kompics.config.TypesafeConfig;
import se.sics.ledbat.core.LedbatConfig;

/**
 *
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class ReceiverConfig {

    public static class Names {
        public static String CUM_ACK_TIMEOUT = "reliableUDP.cumulative_ack_timeout";
        public static String MAX_CUM_ACK = "reliableUDP.max_cumulative_ack";
//        public static String MAX_OUT_SEQ_CUM_ACK = "reliableUDP.max_outOfSeq_cumulative_ack";
        public static String CUMULATE_ACKS = "reliableUDP.cumulate_acks";
    }

    public final LedbatConfig ledbatConfig;
    /**
     * maximum number of acks that can be accumulated before sending an ack.
     */
    public final int maxCumulativeAck;
     /**
     * if true, it means that this connection's client would like its acks to be
     * accumulated
     */
    public final boolean cumulateAck;
    /**
     * Amount of time that cumulativeAckTimer may wait until it sends an eack.
     */
    public final long cumulativeAckTimeout;

    public ReceiverConfig(LedbatConfig ledbatConfig) {
        this.ledbatConfig = ledbatConfig;
        Config config = TypesafeConfig.load();
        maxCumulativeAck = config.getValue(Names.MAX_CUM_ACK, Integer.class);
        cumulateAck = config.getValue(Names.CUMULATE_ACKS, Boolean.class);
        cumulativeAckTimeout = config.getValue(Names.CUM_ACK_TIMEOUT, Long.class);
    }
}

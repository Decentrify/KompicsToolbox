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
package se.sics.ledbat.core;

import se.sics.kompics.config.Config;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LedbatConfig {

    public static class Names {

        public static String RETRANSMISSION_TIMEOUT = "ledbat.retransmission_timeout";
        public static String BASE_HISTORY_SIZE = "ledbat.base_history_size";
        public static String CURRENT_HISTORY_SIZE = "ledbat.current_history_size";
        public static String TARGET = "ledbat.target";
        public static String GAIN = "ledbat.gain";
        public static String RECEIVER_ADVERTISED_WINDOW = "ledbat.raw";
        public static String ALLOWED_INCREASE = "ledbat.allowed_increase";
        public static String TETHER = "ledbat.tether";
        public static String MIN_CWND = "ledbat.min_cwnd";
        public static String INIT_CWND = "ledbat.init_cwnd";
        public static String MSS = "ledbat.MSS";
        public static String PACKET_SEND_MODE = "ledbat.send_only_full_packets";
        public static String SLOW_START_ENABLED = "ledbat.slow_start_enabled";
        public static String SSTHRESHOLD = "ledbat.ss_threshold";
        public static String SLOW_START_ALWAYS = "ledbat.slow_start_always";
        public static String ACCEPTABLE_LOSS = "ledbat.acceptable_loss";
    }

    public final long retransmission_timeout;
    public final int base_history_size;
    public final int current_history_size;
    /**
     * maximum queing_delay that LEDBAT introduces in the network in
     * milliseconds
     */
    public final int target;
    /**
     * the rate at which cwnd responds to the changes in queuing_delay
     */
    public final int gain;
    public final int allowed_increase;
    /**
     * minimum congestion window
     */
    public final double minCwnd;
    public final double initCwnd;
    /**
     * Maximum Segment Size which is read from config file
     */
    public final int mss;
    public final boolean slowStartEnabled;
    public final boolean slowStartAlways;
    public final long ssThreshold;
    /**
     ** Limits the size of the CWND
     */
    public final int raw;
    /**
     * Roberto's DTL paper, AIMD
     */
    public final double beta = 0.99;
    public final double acceptableLoss;
    
    public LedbatConfig(Config config) {
        retransmission_timeout = config.getValue(Names.RETRANSMISSION_TIMEOUT, Long.class);
        base_history_size = config.getValue(Names.BASE_HISTORY_SIZE, Integer.class);
        current_history_size = config.getValue(Names.CURRENT_HISTORY_SIZE, Integer.class);
        target = config.getValue(Names.TARGET, Integer.class);
        gain = config.getValue(Names.GAIN, Integer.class);
        allowed_increase = config.getValue(Names.ALLOWED_INCREASE, Integer.class);
        minCwnd = config.getValue(Names.MIN_CWND, Double.class);
        initCwnd = config.getValue(Names.INIT_CWND, Double.class);
        mss = config.getValue(Names.MSS, Integer.class);
        slowStartEnabled = config.getValue(Names.SLOW_START_ENABLED, Boolean.class);
        slowStartAlways = config.getValue(Names.SLOW_START_ALWAYS, Boolean.class);
        ssThreshold = config.getValue(Names.SSTHRESHOLD, Long.class);
        raw = config.getValue(Names.RECEIVER_ADVERTISED_WINDOW, Integer.class);
        acceptableLoss = config.getValue(Names.ACCEPTABLE_LOSS, Double.class);
    }
}

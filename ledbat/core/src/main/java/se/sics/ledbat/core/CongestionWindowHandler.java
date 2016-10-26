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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main algorithm for calculating congestion window. Each connection has one
 * instance of CongestionWindowHandler.
 *
 * @author serveh & Ahmad
 */
public class CongestionWindowHandler {

    private Logger LOG = LoggerFactory.getLogger(CongestionWindowHandler.class);

    private final LedbatConfig ledbatConfig;
    /**
     * maximum queing_delay that LEDBAT introduces in the network in
     * milliseconds
     */
    private final double target;
    private final double allowedIncrease;
    /**
     * if cwnd is lower than this threshold, we are in the slow_start phase, if
     * not we are in LEDBAT phase
     */
    private long ssThreshold;
    
    private long[] base_history;
    private long[] current_history;
    private long startingTime;
    private long last_rollover;
    private long lastTimeCwndHalved;
    private int base_lastUpdatedIndex;
    private int current_lastUpdatedIndex;

    /**
     * congestion window in bytes
     */
    private double cwnd;
    
    private boolean thresholdIsSet;


    public CongestionWindowHandler(LedbatConfig ledbatConfig) {
        this.ledbatConfig = ledbatConfig;

        this.target = ledbatConfig.target;
        this.allowedIncrease = ledbatConfig.allowed_increase;
        this.ssThreshold = ledbatConfig.ssThreshold;

        initialize();
    }

    //todo : add considerations for clock skew...
    /**
     * initializes all thevariables and populates all the lists.
     */
    private void initialize() {
        cwnd = getInitialCwnd();

        startingTime = System.currentTimeMillis();
        last_rollover = Long.MIN_VALUE;
        lastTimeCwndHalved = Long.MIN_VALUE;
        
        current_lastUpdatedIndex = 0;
        base_lastUpdatedIndex = 0;
        current_history = new long[ledbatConfig.current_history_size];
        base_history = new long[ledbatConfig.base_history_size];
        for (int i = 0; i < ledbatConfig.current_history_size; i++) {
            current_history[i] = Long.MAX_VALUE;
        }
        for (int i = 0; i < ledbatConfig.base_history_size; i++) {
            base_history[i] = Long.MAX_VALUE;
        }
        thresholdIsSet = false;
        LOG.debug("cwnd:init - time (in ms), cwnd (in bytes), queuing_delay(in miliseconds), flight_size(in bytes)");
    }

    /**
     * @return initial amount of congestion window in bytes
     */
    public double getInitialCwnd() {
        return ledbatConfig.initCwnd * ledbatConfig.mss;
    }

    public double getMinCwnd() {
        return ledbatConfig.minCwnd * ledbatConfig.mss;
    }

    public double getRawCwnd() {
        return ledbatConfig.raw * ledbatConfig.mss;
    }

    /**
     * This should be called each time that an ack is received. calculates new
     * cwnd according to the queing_delay of the received packet, flight size
     * and number of bytes that is newly acked.
     *
     * @param one_way_delay : delay from client to server in milliseconds
     * @param flightSize : number of bytes on the link before this last ack came
     * @param bytes_newly_acked : number of bytes that is acked in this last ack
     */
    public void updateCWND(long one_way_delay, long flightSize, long bytes_newly_acked) {
        updateBaseDelay(one_way_delay);
        updateCurrentDelay(one_way_delay);
        long queuing_delay = currentDelay() - baseDelay();

        if ((ledbatConfig.slowStartEnabled && cwnd < ssThreshold * ledbatConfig.mss && !thresholdIsSet)
                || (ledbatConfig.slowStartEnabled && cwnd < ssThreshold * ledbatConfig.mss && ledbatConfig.slowStartAlways && queuing_delay < target / 2)) {
            //cwnd is increased each time by the number of bytes acknowledged
            //cwnd += bytes_newly_acked;
            cwnd += ledbatConfig.mss;
            //consider raw in slow start
            cwnd = Math.min(cwnd, getRawCwnd());
        } else {
            //slow start is not enabled or in congestion avoidance phase
            //once out of the slow start phase, out forever, even if no loss happened.
            //Not doing this causes increase in cwnd even though queueing delay is greater than target!(before first loss)
            if (!thresholdIsSet) {
                finishSlowStart();
            }
            double off_target = (target - queuing_delay) / target;
            if (off_target < 0) {
                cwnd = cwnd * ledbatConfig.beta;
                LOG.debug("cwnd:slow - value:{}", cwnd);
                LOG.trace("cwnd:slow -  history current:{} base:{}", current_history, base_history);
            } else {
                double cwndGain = ((ledbatConfig.gain * off_target * bytes_newly_acked * ledbatConfig.mss) / cwnd);
                cwnd = cwnd + cwndGain;
                LOG.trace("cwnd:gain - value:{} offTarget:{} gain:{}", new Object[]{cwnd, off_target, cwndGain});
            }
            cwnd = Math.max(cwnd, getMinCwnd());
        }
    }

    /**
     * This should be called if loss occurs. It updates cwnd size in this way:
     * If we are using slow start, and we are still in this phase, ssthreshold
     * should be halved and cwnddecresed too minimum value. If we don't use slow
     * start or we are not in this phase, cwnd is halved.
     */
    public double handleLoss(long rtt) {
        long now = System.currentTimeMillis();
        if (ledbatConfig.slowStartEnabled && (cwnd <= ssThreshold * ledbatConfig.mss) && !thresholdIsSet) { // if in slow-start phase and first time loss happens
            lossHalving(now);
            finishSlowStart();
        } else if (now - lastTimeCwndHalved >= rtt) { //At most once per RTT
            lossHalving(now);
        } else {
            LOG.debug("cwnd:loss - nothing");
        }
        return cwnd;
    }

    public void lossHalving(long now) {
        lastTimeCwndHalved = now;
        cwnd = cwnd * 0.5;
        cwnd = Math.max(cwnd, getMinCwnd());
        LOG.info("cwnd:half - value:{}", cwnd);
    }

    public void finishSlowStart() {
        thresholdIsSet = true;
        ssThreshold = (long) (cwnd / ledbatConfig.mss);
        LOG.info("cwnd:slow start - value:{}", cwnd);
    }

    //todo: after a long silent period, replace all entries older than 1 RTT with +INFINITY
    /**
     * updates current_delay history list with the new delay received
     *
     * @param delay : the new one_way_delay received
     */
    private void updateCurrentDelay(long delay) {
        if (current_lastUpdatedIndex + 1 == ledbatConfig.current_history_size) {
            current_history[0] = delay;
            current_lastUpdatedIndex = 0;
        } else {
            current_history[current_lastUpdatedIndex + 1] = delay;
            current_lastUpdatedIndex += 1;
        }
    }

    /**
     * returns the minimum of the delays in current_delay_history
     * @return current delay
     */
    private long currentDelay() {
        return getMinimum(current_history);
    }

    //todo:After a long peiod of silence ....
    /**
     * updates base_delay history list with the new delay received
     * @param delay : the new one_way_delay received
     */
    private void updateBaseDelay(long delay) {
        //todo: If the connection is idle for a given minute, no data is available for the one-way delay and,
        //therefore, a value of +INFINITY is stored in the list.
        // we are not anymore in the minute in which we updated base_history, so insert new base_delay
        long now = System.currentTimeMillis();
        long nowMinute = roundToMinute(now);
        long lastRolloverMinute = roundToMinute(last_rollover);
        if (nowMinute != lastRolloverMinute) {
            LOG.info("cwnd:{} history current:{} base:{}", new Object[]{cwnd, current_history, base_history});
            if (base_lastUpdatedIndex + 1 == ledbatConfig.base_history_size) {
                base_history[0] = delay;
                base_lastUpdatedIndex = 0;
            } else {
                base_history[base_lastUpdatedIndex + 1] = delay;
                base_lastUpdatedIndex += 1;
            }

        } else { // we are in the same minute, just update the last base_delay you inserted if necessary
            long lastOfBaseDelay = base_history[base_lastUpdatedIndex];
            base_history[base_lastUpdatedIndex] = Math.min(delay, lastOfBaseDelay);
        }
        last_rollover = now;
    }

    /**
     * returns the minimum of the delays in base_delay_history
     *
     * @return base delay
     */
    private long baseDelay() {
        return getMinimum(base_history);
    }

    private long roundToMinute(long timeInMillis) {
        return timeInMillis / 60000;
    }

    private long getMinimum(long[] list) {
        long min = list[0];
        for (long currentLong : list) {
            if (currentLong < min) {
                min = currentLong;
            }
        }
        return min;
    }

    public double getCwnd() {
        return cwnd;
    }
    
    public int getMss() {
        return ledbatConfig.mss;
    }
}

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

import java.util.Random;
import org.javatuples.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.util.tracking.load.util.FuzzyTimeoutCounter;

/**
 * Main algorithm for calculating congestion window. Each connection has one
 * instance of CongestionWindowHandler.
 *
 * @author serveh & Ahmad
 */
public class CongestionWindowHandler {

    private Logger logger = LoggerFactory.getLogger(CongestionWindowHandler.class);

    private final LedbatConfig ledbatConfig;
    private final FuzzyTimeoutCounter timeoutCounter;
    /**
     * maximum queing_delay that LEDBAT introduces in the network in
     * milliseconds
     */
    private double target;
    /**
     * if cwnd is lower than this threshold, we are in the slow_start phase, if
     * not we are in LEDBAT phase
     */
    private long ssThreshold;

    private long[] base_history;
    private long[] current_history;
    private long last_rollover;
    private int base_lastUpdatedIndex;
    private int current_lastUpdatedIndex;

    long sumOfTotalSentBytes;
    private double allowedIncrease;
    /**
     * congestion window in bytes
     */
    private double cwnd;
    private double startingTime;

    private boolean thresholdIsSet;

    private long lastTimeCwndHalved = 0;

    public CongestionWindowHandler(LedbatConfig ledbatConfig) {
        this.ledbatConfig = ledbatConfig;
        this.timeoutCounter = FuzzyTimeoutCounter.getInstance(Triplet.with(0.01, 0.005, 0.02), new Random(1234));
        
        this.target = ledbatConfig.target;
        this.allowedIncrease = ledbatConfig.allowed_increase;
        this.ssThreshold = ledbatConfig.ssThreshold;

        initialize();
    }

    static long counter = 0;

    //todo : add considerations for clock skew...
    /**
     * initializes all thevariables and populates all the lists.
     */
    private void initialize() {
        cwnd = getInitialCwnd();

        startingTime = System.currentTimeMillis();
        current_lastUpdatedIndex = 0;
        base_lastUpdatedIndex = 0;
        current_history = new long[ledbatConfig.current_history_size];
        base_history = new long[ledbatConfig.base_history_size];
        last_rollover = Long.MIN_VALUE;
        for (int i = 0; i < ledbatConfig.current_history_size; i++) {
            current_history[i] = Long.MAX_VALUE;
        }
        for (int i = 0; i < ledbatConfig.base_history_size; i++) {
            base_history[i] = Long.MAX_VALUE;
        }

        thresholdIsSet = false;
        sumOfTotalSentBytes = 0;
        logger.debug(" Time (in seconds), " + "cwnd (in bytes), queuing_delay(in miliseconds), flight_size(in bytes), bytes_to_send(in bytes)");
    }

    /**
     * @return initial amount of congestion window in bytes
     */
    public double getInitialCwnd() {
        return ledbatConfig.initCwnd * ledbatConfig.mss;
    }

    public void dumpState() {
        logger.error("cwnd size :" + cwnd);
    }

    /* private boolean doLogSlowstart = false;

     public void setDoLogSlowstart(boolean doLogSlowstart) {
     this.doLogSlowstart = doLogSlowstart;
     }*/
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
        timeoutCounter.success();

        long queuing_delay = currentDelay() - baseDelay();
        /* if (queuing_delay > 10) {
         logger.warn("queue delay is: " + queuing_delay);
         }*/

        if ((ledbatConfig.slowStartEnabled && cwnd < ssThreshold * ledbatConfig.mss && !thresholdIsSet)
                || (ledbatConfig.slowStartEnabled && cwnd < ssThreshold * ledbatConfig.mss && ledbatConfig.slowStartAlways && queuing_delay < target / 2)) {

            //&& cwnd < raw * mss) { // so in the slow_start phase
            // cwnd is increased each time by the number of bytes acknowledged
            //cwnd += bytes_newly_acked;
            cwnd += ledbatConfig.mss;
            //consider raw in slow start
            cwnd = Math.min(cwnd, ledbatConfig.raw * ledbatConfig.mss);
            //logger.warn("------------In SlowStart Mode------------- cwnd: " + cwnd + "queuing delay :" + queuing_delay + " and target :" + target);
            logCwndChanges(queuing_delay, flightSize, bytes_newly_acked);
        } else { //slow start is not enabled or in congestion avoidance phase
            //once out of the slow start phase, out forever, even if no loss happened.
            //Not doing this causes increase in cwnd even though queueing delay is greater than target!(before first loss)
            if (!thresholdIsSet) {
                ssThreshold = (long) (cwnd / ledbatConfig.mss);
                thresholdIsSet = true;
                logger.warn("Initial ssthreshold is reached !   threshold set to  " + ssThreshold);
            }
            double off_target = (target - queuing_delay) / target;

            if (off_target < 0) {
                cwnd = cwnd * ledbatConfig.beta;
            } else {
                cwnd = cwnd + ((ledbatConfig.gain * off_target * bytes_newly_acked * ledbatConfig.mss) / cwnd);
            }

            //double max_allowed_cwnd = flightSize + allowedIncrease * mss;
            //cwnd = cwnd > max_allowed_cwnd ? max_allowed_cwnd : cwnd;
            cwnd = cwnd > ledbatConfig.minCwnd * ledbatConfig.mss ? cwnd : ledbatConfig.minCwnd * ledbatConfig.mss;

            cwnd = Math.min(cwnd, ledbatConfig.raw * ledbatConfig.mss);

            logCwndChanges(queuing_delay, flightSize, bytes_newly_acked);
        }
    }

    /**
     * gets the current cwnd size and compares it with the current flightSize.
     * so it returns the amount of bytes that we can send .
     *
     * @param flightSize : number of bytes on the fly
     * @return number of bytes that can be sent according to the flightSize
     */
    public double getNumberOfByteToSend(long flightSize) {

        //todo : Senders maximum window = Min (advertised window, cwnd)
        double amountToSend;
        if ((cwnd - flightSize) > 0) {
            amountToSend = cwnd - flightSize;
        } else {
            //logger.trace("additional flight size check: updated flight size would be bigger than cwnd");
            amountToSend = 0;
        }
        return amountToSend;
    }

    /**
     * This should be called if loss occurs. It updates cwnd size in this way:
     * If we are using slow start, and we are still in this phase, ssthreshold
     * should be halved and cwnddecresed too minimum value. If we don't use slow
     * start or we are not in this phase, cwnd is halved.
     */
    public double handleLoss(long rtt) {
        timeoutCounter.timeout();
        if(!timeoutCounter.trigger()) {
            return cwnd;
        }
        //todo : at most once per RTT
        if (ledbatConfig.slowStartEnabled && (cwnd <= ssThreshold * ledbatConfig.mss) && !thresholdIsSet) { // if in slow-start phase and first time loss happens
            thresholdIsSet = true;
            cwnd = cwnd * 0.5;
            lastTimeCwndHalved = System.currentTimeMillis();
            ssThreshold = (long) (cwnd / ledbatConfig.mss);
            logger.warn("First loss!   threshold set to  " + ssThreshold);
            // This will work for the first flow, but not for subsequent flows (if there are
            // existing flows).
//                target = currentDelay();
//                //logger.error("   TARGET UPDATED TO " + target);

        } else if (System.currentTimeMillis() - lastTimeCwndHalved >= rtt) { //At most once per RTT

            cwnd = cwnd * 0.5;
            lastTimeCwndHalved = System.currentTimeMillis();
            cwnd = cwnd > ledbatConfig.minCwnd * ledbatConfig.mss ? cwnd : ledbatConfig.minCwnd * ledbatConfig.mss;
            //logger.info(" Loss !! and cwnd is halved: " + cwnd);

        } else {
            //logger.info("Loss !! and cwnd NOT halved: " + cwnd);
        }
        return cwnd;
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
     *
     * @return current delay
     */
    private long currentDelay() {
        return getMinimum(current_history);
    }

    //todo:After a long peiod of silence ....
    /**
     * updates base_delay history list with the new delay received
     *
     * @param delay : the new one_way_delay received
     */
    private void updateBaseDelay(long delay) {
        //todo: If the connection is idle for a given minute, no data is available for the one-way delay and,
        //therefore, a value of +INFINITY is stored in the list.
        // we are not anymore in the minute in which we updated base_history, so insert new base_delay
        long nowMinute = roundToMinute(System.currentTimeMillis());
        long lastRolloverMinute = roundToMinute(last_rollover);
        if (nowMinute != lastRolloverMinute) {

// base_delay_idx = (base_delay_idx + 1) % BASE_DELAY_SIZE;
            if (base_lastUpdatedIndex + 1 == ledbatConfig.base_history_size) {
                base_history[0] = delay;
                base_lastUpdatedIndex = 0;
            } else {
                base_history[base_lastUpdatedIndex + 1] = delay;
                base_lastUpdatedIndex += 1;
            }

        } else { // we are in the same minute, just update the last base_delay you inserted if necessary
            long lastOfBaseDelay = base_history[base_lastUpdatedIndex];
            base_history[base_lastUpdatedIndex] = delay > lastOfBaseDelay ? lastOfBaseDelay : delay;
        }
        last_rollover = System.currentTimeMillis();
    }

    /**
     * returns the minimum of the delays in base_delay_history
     *
     * @return base delay
     */
    private long baseDelay() {
        return getMinimum(base_history);
    }

    /**
     * writes time in milliseconds and cwnd size in packet number in a log file
     * and then plots a chart for cwnd changes.
     *
     * @param queueing_delay : queueing_delay
     * @param flightSize : flightSize
     * @param bytes_newly_acked : number of bytes that are newly acked
     */
    private void logCwndChanges(long queueing_delay, long flightSize, long bytes_newly_acked) {
        // Time (in seconds), cwnd (# of packets), cwnd (in bytes), queuing_delay(in miliseconds), flight_size(in bytes), bytes_newly_acked(in bytes)
        long now = System.currentTimeMillis();

        // if (counter++ % 100 == 0) {
        StringBuffer sb = new StringBuffer();
        sb = sb
                //              .append("\t").append(cwnd / mss)
                .append("\t").append(cwnd)
                .append("\t").append(queueing_delay)
                .append("\t").append(flightSize) //                .append("\t").append(cwnd - (flightSize - bytes_newly_acked))
                //              .append("\t").append(bytes_newly_acked)
                //
                ;
        //fileLogger.debug(sb.toString());
        //}
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

    //just for testcases
    public void setCwnd(double cwnd) {
        this.cwnd = cwnd;
    }

    /**
     * Returns the min(cwnd,rwnd) as cwnd
     *
     * @return
     */
    public double getCwnd() {
        return cwnd;
    }

    public void setSsThreshold(long ssThreshold) {
        this.ssThreshold = ssThreshold;
    }

    public int getMss() {
        return ledbatConfig.mss;
    }

    public void setThresholdIsSet(boolean thresholdIsSet) {
        this.thresholdIsSet = thresholdIsSet;
    }

    public void updateCWNDTest(long queuing_delay, long flightSize, long bytes_newly_acked) {

        if (ledbatConfig.slowStartEnabled && cwnd < ssThreshold * ledbatConfig.mss && !thresholdIsSet) { // so in the slow_start phase

            // cwnd is increased each time by the number of bytes acknowledged
            //cwnd += bytes_newly_acked;
            cwnd += ledbatConfig.mss;
        } else { //slow start is not enabled or in congestion avoidance phase

            double off_target = (target - queuing_delay) / target;

            cwnd = cwnd + ((ledbatConfig.gain * off_target * bytes_newly_acked * ledbatConfig.mss) / cwnd);

            double max_allowed_cwnd = flightSize + allowedIncrease * ledbatConfig.mss;

            cwnd = cwnd > max_allowed_cwnd ? max_allowed_cwnd : cwnd;

            cwnd = cwnd > ledbatConfig.minCwnd * ledbatConfig.mss ? cwnd : ledbatConfig.minCwnd * ledbatConfig.mss;

            /* if (cwnd > receiverAdvertisedWindow * mss) {
             // if (counter % 100 == 0) {

             //logger.warn("\t\tCalculated CWND is greater than receiver advertised Window !!!" + cwnd);
             //}
             cwnd = receiverAdvertisedWindow * mss;
             }*/
            //logger.trace("after all new cwnd is " + cwnd);
            //logger.debug("======================");
            logCwndChanges(queuing_delay, flightSize, bytes_newly_acked);
        }
    }

    public void updateCWND_TestDTL(long queuing_delay, long flightSize, long bytes_newly_acked) {
        if (ledbatConfig.slowStartEnabled && cwnd < ssThreshold * ledbatConfig.mss) { // so in the slow_start phase

            // cwnd is increased each time by the number of bytes acknowledged
            //cwnd += bytes_newly_acked;
            cwnd += ledbatConfig.mss;
        } else { //slow start is not enabled or in congestion avoidance phase

            double off_target = target - queuing_delay;

            cwnd = cwnd + (off_target / (target * cwnd));

            //double max_allowed_cwnd = flightSize + allowedIncrease * mss;
            //cwnd = cwnd > max_allowed_cwnd ? max_allowed_cwnd : cwnd;
            cwnd = cwnd > ledbatConfig.minCwnd * ledbatConfig.mss ? cwnd : ledbatConfig.minCwnd * ledbatConfig.mss;

            /* if (cwnd > receiverAdvertisedWindow * mss) {
             // if (counter % 100 == 0) {

             //logger.warn("\t\tCalculated CWND is greater than receiver advertised Window !!!" + cwnd);
             //}
             cwnd = receiverAdvertisedWindow * mss;
             }*/
            //logger.trace("after all new cwnd is " + cwnd);
            //logger.debug("======================");
            logCwndChanges(queuing_delay, flightSize, bytes_newly_acked);
        }
    }

    public void setTarget(double target) {
        logger.warn("Target set from " + this.target + " to " + target);
        this.target = target;
    }

    public void setAllowed_increase(double allowedIncrease) {
        logger.warn("Allowed Increase set from " + this.allowedIncrease + " to " + allowedIncrease);
        this.allowedIncrease = allowedIncrease;
    }
}
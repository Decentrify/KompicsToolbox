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
 * @Author : Ahmad & Serveh
 */
public class RTTEstimator {

    private static final Logger LOG = LoggerFactory.getLogger(RTTEstimator.class);

    private long retransmissionTimeout;

    private long currentSecond;
    private static final int TIME_STEP = 1;

    /*
     // parameter used for calculating SRTT
     private double alpha ;

     private double sumOfRTTDeviations;

     private double sumOfSampleRTTs;
     // smoothed value of RTT.
     private int srtt;
     private int numberOfRecAcks;

     public RTTEstimator() {
     PropertyLoader propertyLoader = PropertyLoader.getInstance();
     retransmissionTimeout = Long.valueOf(propertyLoader.getProperty(ClientTransportManager.retransmission_timeout));
     alpha = 0.9;

     }

     public void updateRTO(int sampleRTT) {
     numberOfRecAcks++;
     //Original Algorithm :
     if (srtt == 0) { // for the first time, srtt is the same as the sample
     srtt = sampleRTT;
     }
     srtt = (int) (alpha * srtt + (1 - alpha) * sampleRTT);
     //logger.debug("Estimated RTT is :" + srtt);

     // Van Jacobson algorithm : RTO = SRTT + u * Dev(RTT)
     sumOfSampleRTTs += sampleRTT;
     double xBar = sumOfSampleRTTs / numberOfRecAcks;
     sumOfRTTDeviations += Math.abs(sampleRTT - xBar);
     double meanDev = sumOfRTTDeviations / numberOfRecAcks;
     retransmissionTimeout = (long) (srtt + 4 * meanDev);

     //logger.debug("retransmission_timeout is : " + retransmissionTimeout);
     }

     public long getRetransmissionTimeout() {
     return retransmissionTimeout;
     }


     */
    private double avgRTT;

    private double varRTT;

    private double RTO;

    private double showedRTO;

    private double alpha = 0.125;

    private double beta = 0.25;

    private long K = 4;

    private long minRTO;

    private final LedbatConfig ledbatConfig;

    public RTTEstimator(LedbatConfig ledbatConfig, long rtoMin) {
        this.ledbatConfig = ledbatConfig;
        avgRTT = 0.0;
        varRTT = 0.0;
        RTO = -1.0;

        //for the first time to avoid loss for first sequence
        showedRTO = 1000;

        this.minRTO = rtoMin;
    }

    public void updateRTO(long now, long rtt) {
        if (now > currentSecond + (TIME_STEP - 1)) {
            LOG.debug("RTT:{}", showedRTO);
            currentSecond = now;
        }

        if (RTO == -1) {
             // Set RTO to RTT if it's the first time it's updated
            // this.count = 1;

            // SRTT <- R, RTTVAR <- R/2, RTO <- SRTT + max (G, KRTTVAR)
            this.avgRTT = rtt;
            this.varRTT = rtt / 2.0;

            this.RTO = avgRTT + (K * varRTT);

            ////logger.debug("Initial RTO " + RTO);
        } else {

            // log.debug("Changing RTO " + RTO);
            // log.debug("VAR " + varRTT);
            // log.debug("AVG " + avgRTT);
            // log.debug("Beta "+beta);
            // log.debug("Alpha "+alpha);
            // RTTVAR <- (1 - beta) * RTTVAR + beta * |SRTT - R'|
            this.varRTT = (1 - beta) * varRTT + beta * Math.abs((avgRTT - rtt));

            // log.debug("Variance " + varRTT);
            // SRTT <- (1 - alpha) * SRTT + alpha * R'
            this.avgRTT = (1 - alpha) * avgRTT + alpha * rtt;

            // log.debug("Average " + avgRTT);
            // RTO = AVG + K x VAR;
            this.RTO = avgRTT + K * varRTT;

             // log.debug("Result RTO " + RTO);
            // // AVG = (((AVG * CNT) + RTT) / (CNT + 1));
            // this.avgRTT = (((avgRTT * count) + RTT) / (count + 1));
            // log.debug("Average RTT " + avgRTT);
            //
            // // DIFF = (AVG - RTT)^2;
            // this.diff = pow((avgRTT - RTT), 2);
            //
            // log.debug(" DIFF " + diff);
            //
            // log.debug("Var RTT before "+ varRTT);
            //
            // // VAR = (((VAR * CNT) + DIFF) / (CNT + 1)); // variance of RTT
            // this.varRTT = (((varRTT * count) + diff) / (count + 1));
            //
            // log.debug("Variance " + varRTT);
            // // CNT++;
            // this.count++;
            //
            // // RTO = AVG + 4 x VAR;
            // this.RTO = avgRTT + K * varRTT;
        }
        // log.debug("RTO before check if between max and min value " + RTO);
        if (this.RTO < minRTO) {
            this.showedRTO = minRTO;

            // maximum does not make sense
            // } else if (this.RTO > FD_MAXIMUM_RTO) {
            // this.showedRTO = FD_MAXIMUM_RTO;
        } else {
            this.showedRTO = RTO;
        }
    }

    public void timedOut() {

    }

    public long getRetransmissionTimeout() {
        long r = (showedRTO == 0 ? (long) minRTO : (long) showedRTO);

        if (r < minRTO) {
            System.err.println("r=" + r + " min=" + minRTO);
        }

        return r;
    }

    /**
     * reads value of retransmission_timeout parameter and returns it as initial
     * value for RTO.
     *
     * @return initial value of RTO(retransmission timeout)
     */
    public long getInitialRetransmissionTimeout() {
        return ledbatConfig.retransmission_timeout;
    }

    public void doubleRto() {
        this.RTO *= 2;
    }
}

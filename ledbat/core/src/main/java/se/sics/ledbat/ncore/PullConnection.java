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
package se.sics.ledbat.ncore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ledbat.core.CongestionWindowHandler;
import se.sics.ledbat.core.LedbatConfig;
import se.sics.ledbat.core.RTTEstimator;
import se.sics.ledbat.core.util.ThroughputHandler;
import se.sics.ledbat.ncore.msg.LedbatMsg;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class PullConnection {

    private final static Logger LOG = LoggerFactory.getLogger(PullConnection.class);
    private String logPrefix = "";

    private final static long RTO_MIN = 100;

    private final LedbatConfig ledbatConfig;

    /**
     * flightsize is the amount of data outsanding .It is updated after updating
     * cwnd size on each ack by updateFlightsize();
     */
    private long flightSize;
    private CongestionWindowHandler cwndHandler;
    private RTTEstimator rttEstimator;
    private ThroughputHandler receivedThroughput;
    private ThroughputHandler requestedThroughput;
    private ThroughputHandler lateThroughput;
    private ThroughputHandler timeoutThroughput;
    //**************************************************************************
    
    public PullConnection(LedbatConfig ledbatConfig, Identifier connectionId) {
        this.ledbatConfig = ledbatConfig;
        flightSize = 0;
        cwndHandler = new CongestionWindowHandler(ledbatConfig);
        rttEstimator = new RTTEstimator(ledbatConfig, RTO_MIN);
        receivedThroughput = new ThroughputHandler(connectionId.toString() + "rec");
        requestedThroughput = new ThroughputHandler(connectionId.toString() + "req");
        lateThroughput = new ThroughputHandler(connectionId.toString() + "late");
        timeoutThroughput = new ThroughputHandler(connectionId.toString() + "time");
    }

    public int canSend() {
        int allowedToSend = (int)((cwndHandler.getCwnd() - flightSize)/ledbatConfig.mss);
        if (allowedToSend > 0) {
            return allowedToSend;
        } else {
            return 0;
        }
    }
    
    public long getRTO() {
        return rttEstimator.getRetransmissionTimeout();
    }

    public void request(int msgSize) {
        flightSize += msgSize;
        requestedThroughput.packetReceived(msgSize);
    }

    public void success(int msgSize, LedbatMsg.Response resp) {
        flightSize -= msgSize;
        receivedThroughput.packetReceived(msgSize);

        int rtt = (int) (System.currentTimeMillis() - resp.leecherAppReqSendT);
        rttEstimator.updateRTO(rtt);

        int oneWayDelay = (int) (resp.leechedNetRespT - resp.seederNetRespSendT);
        cwndHandler.updateCWND(oneWayDelay, flightSize, msgSize);
        
//        LOG.info("on time rtt:{} oneWayDelay:{}", rtt, oneWayDelay);
    }
    
    public void late(int msgSize, LedbatMsg.Response resp) {
        lateThroughput.packetReceived(msgSize);
        
        int rtt = (int) (System.currentTimeMillis() - resp.leecherAppReqSendT);
        rttEstimator.updateRTO(rtt);
        
        int oneWayDelay = (int) (resp.leechedNetRespT - resp.seederNetRespSendT);
        cwndHandler.updateCWND(oneWayDelay, flightSize, msgSize);
//        LOG.info("late rtt:{} oneWayDelay:{}", rtt, oneWayDelay);
    }

    public void timeout(int msgSize) {
        flightSize -= msgSize;
        timeoutThroughput.packetReceived(msgSize);
        
        cwndHandler.handleLoss(rttEstimator.getRetransmissionTimeout());
    }

    //******************************REPORTING***********************************
    public long downloadSpeed() {
        return receivedThroughput.speed();
    }
}

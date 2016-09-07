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
package se.sics.ledbat.core;

import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.tracking.load.util.StatusState;
import se.sics.ledbat.core.CongestionWindowHandler;
import se.sics.ledbat.core.LedbatConfig;
import se.sics.ledbat.core.RTTEstimator;
import se.sics.ledbat.core.util.ThroughputHandler;
import se.sics.ledbat.ncore.msg.LedbatMsg;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class AppCongestionWindow {

    private static final double BACKPRESSURE_CONST = 0.9;
    private static final long RTO_MIN = 100;
    //**************************************************************************
    private final CongestionWindowHandler ledbatCwnd;
    private final RTTEstimator appRttEstimator;
    private final ConnHistory connHistory; 
    //**************************************************************************
    private double appCwnd;
    private long lastChangeTime;
    /**
     * flightsize is the amount of data outsanding .It is updated after updating
     * cwnd size on each ack by updateFlightsize();
     */
    private long flightSize;

    public AppCongestionWindow(LedbatConfig ledbatConfig, Identifier connectionId) {
        ledbatCwnd = new CongestionWindowHandler(ledbatConfig);
        appRttEstimator = new RTTEstimator(ledbatConfig, RTO_MIN);
        connHistory = new ConnHistory(connectionId);
        flightSize = 0;
        lastChangeTime = 0;
        appCwnd = ledbatCwnd.getCwnd();
    }

    //**************************************************************************
    public void appState(long now, StatusState state) {
        switch (state) {
            case MAINTAIN:
                if (appCwnd > ledbatCwnd.getCwnd()) {
                    appCwnd = ledbatCwnd.getCwnd();
                }
            case SPEED_UP:
                if (change(now)) {
                    appCwnd = ledbatCwnd.getCwnd();
                }
            default: //for unknown state just behave as SLOW_DOWN
            case SLOW_DOWN:
                if (change(now)) {
                    appCwnd = BACKPRESSURE_CONST * appCwnd;
                }
        }
    }
    
    public boolean canSend() {
        if(appCwnd > flightSize) {
            return true;
        }
        return false;
    }
    //**************************************************************************
    public void request(long now, int msgSize) {
        connHistory.request(now, msgSize);
        flightSize += msgSize;
    }

    public void success(long now, int msgSize, LedbatMsg.Response resp) {
        connHistory.received(now, msgSize);
        flightSize -= msgSize;

        int rtt = (int) (now - resp.leecherAppReqSendT);
        appRttEstimator.updateRTO(rtt);

        int oneWayDelay = (int) (resp.leechedNetRespT - resp.seederNetRespSendT);
        ledbatCwnd.updateCWND(oneWayDelay, flightSize, msgSize);
    }

    public void late(long now, int msgSize, LedbatMsg.Response resp) {
        connHistory.late(now, msgSize);

        int rtt = (int) (now - resp.leecherAppReqSendT);
        appRttEstimator.updateRTO(rtt);

        int oneWayDelay = (int) (resp.leechedNetRespT - resp.seederNetRespSendT);
        ledbatCwnd.updateCWND(oneWayDelay, flightSize, msgSize);
    }

    public void timeout(long now, int msgSize) {
        flightSize -= msgSize;
        connHistory.timeout(now, msgSize);

        ledbatCwnd.handleLoss(appRttEstimator.getRetransmissionTimeout());
    }
    
    //**************************************************************************
    public long getRTO() {
        return appRttEstimator.getRetransmissionTimeout();
    }

    public long downloadSpeed(long now) {
        return connHistory.avgThroughput(now);
    }
    //**************************************************************************
    private boolean change(long now) {
        if (now - lastChangeTime > appRttEstimator.getRetransmissionTimeout()) {
            lastChangeTime = now;
            return true;
        }
        return false;
    }
    
    public static class ConnHistory {

        private final ThroughputHandler receivedThroughput;
        private final ThroughputHandler requestedThroughput;
        private final ThroughputHandler lateThroughput;
        private final ThroughputHandler timeoutThroughput;

        public ConnHistory(Identifier connectionId) {
            receivedThroughput = new ThroughputHandler(connectionId.toString() + "rec");
            requestedThroughput = new ThroughputHandler(connectionId.toString() + "req");
            lateThroughput = new ThroughputHandler(connectionId.toString() + "late");
            timeoutThroughput = new ThroughputHandler(connectionId.toString() + "time");
        }
        
        public void request(long now, int msgSize) {
            requestedThroughput.packetReceived(now, msgSize);
        }
        
        public void received(long now, int msgSize) {
            receivedThroughput.packetReceived(now, msgSize);
        }
        
        public void timeout(long now, int msgSize) {
            timeoutThroughput.packetReceived(now, msgSize);
        }
        
        public void late(long now, int msgSize) {
            lateThroughput.packetReceived(now, msgSize);
        }
        
        public long avgThroughput(long now) {
            return receivedThroughput.speed(now);
        }
    }
}

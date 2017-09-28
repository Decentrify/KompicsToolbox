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

import com.google.common.base.Optional;
import se.sics.kompics.id.Identifier;
import se.sics.ktoolbox.util.predict.ExpMovingAvg;
import se.sics.ledbat.core.util.ThroughputHandler;
import se.sics.ledbat.ncore.msg.LedbatMsg;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class AppCongestionWindow {

  private final CongestionWindowHandler ledbatCwnd;
//  private final RTTEstimator appRttEstimator;
  private final ExpMovingAvg appRTTEstimator;
  private final long minRTT;

  private final ConnHistory connHistory;
  //**************************************************************************
  private final Optional<AppCwndTracker> tracker;
  //**************************************************************************
  private double appCwnd;
  /**
   * flightsize is the amount of data outsanding .It is updated after updating
   * cwnd size on each ack by updateFlightsize();
   */
  private long flightSize;
  //*****
  private RTTAdjustCtrl rttCtrl;

  public AppCongestionWindow(LedbatConfig ledbatConfig, Identifier connectionId, long minRTT, Optional<String> reportDir) {
    ledbatCwnd = new CongestionWindowHandler(connectionId, ledbatConfig, reportDir);
    this.minRTT = minRTT;
//    appRttEstimator = new RTTEstimator(ledbatConfig, minRTO);
    appRTTEstimator = new ExpMovingAvg(1000);//start with 1s RTT
    connHistory = new ConnHistory(connectionId);
    flightSize = 0;
    appCwnd = ledbatCwnd.getCwnd();
    if (reportDir.isPresent()) {
      tracker = Optional.of(AppCwndTracker.onDisk(reportDir.get(), connectionId));
    } else {
      tracker = Optional.absent();
    }
    rttCtrl = new RTTAdjustCtrl();
  }

  public void close() {
    ledbatCwnd.close();
    if (tracker.isPresent()) {
      tracker.get().close();
    }
  }

  public long getRTT() {
    return Math.min(Math.max(minRTT, (long) appRTTEstimator.get()), 1000);
  }

  //**************************************************************************
  private static class RTTAdjustCtrl {

    private long lastAdjusted;
    private long rtt;
    private boolean started;
    private boolean onUp;

    public RTTAdjustCtrl() {
      this.started = false;
      this.onUp = false;
    }

    public void start(long now) {
      started = true;
      lastAdjusted = now;
      rtt = 0;
      onUp = true;
    }

    public boolean isStarted() {
      return started;
    }

    public boolean canGoUp(long now) {
      if (now - lastAdjusted > rtt) {
        return true;
      }
      return false;
    }

    public boolean canGoDown(long now) {
      if (onUp) {
        return true;
      }
      if (now - lastAdjusted > rtt / 2) {
        return true;
      }
      return false;
    }

    public void goUp(long now, long newRtt) {
      lastAdjusted = now;
      rtt = newRtt;
      onUp = true;
    }

    public void goDown(long now, long newRtt) {
      lastAdjusted = now;
      rtt = newRtt;
      onUp = false;
    }
  }

  public void adjustState(long now, double adjustment) {
    if (rttCtrl.isStarted()) {
      adjustment = Math.min(adjustment, rttAdjustment());
      if (adjustment <= 0 && rttCtrl.canGoDown(now)) {
        adjust(now, adjustment);
        rttCtrl.goDown(now, (long) appRTTEstimator.get());
      } else if (adjustment > 0 && rttCtrl.canGoUp(now)) {
        adjust(now, adjustment);
        rttCtrl.goUp(now, (long) appRTTEstimator.get());
      } else {
        //nothing
      }
    } else {
      adjust(now, adjustment);
    }
  }

  private void adjust(long now, double adjustment) {
    double multiplier_const = getMultplier(adjustment);
    appCwnd = Math.min(Math.max(multiplier_const * appCwnd, ledbatCwnd.getMinCwnd()), ledbatCwnd.getCwnd());
    reportAdjustment(now, multiplier_const, appCwnd);
  }

  private double rttAdjustment() {
    long trueRTT = (long) appRTTEstimator.get();
    long oneWayDelay = ledbatCwnd.getEstimatedOWD();
    int oneSideExecutionOverhead = 100;
    if (trueRTT > 2 * (oneWayDelay + oneSideExecutionOverhead)) {//100ms
      return -0.7;
    } else if (trueRTT > 2 * (oneWayDelay + oneSideExecutionOverhead / 2)) {//
      return -0.4;
    } else if (trueRTT > 2 * (oneWayDelay + oneSideExecutionOverhead / 4)) {//
      return -0.1;
    } else if (trueRTT > 2 * (oneWayDelay + oneSideExecutionOverhead / 5)) {//
      return 0.1;
    } else if (trueRTT > 2 * (oneWayDelay + oneSideExecutionOverhead / 10)) {//
      return 0.4;
    } else {
      return 0.7;
    }
  }

  private double getMultplier(double adjustment) {
    if (adjustment <= -0.7) {
      return 0.5;
    } else if (adjustment <= -0.4) {
      return 0.7;
    } else if (adjustment <= -0.1) {
      return 0.8;
    } else if (adjustment <= 0) {
      return 1;
    } else if (adjustment <= 0.1) {
      return 1.1;
    } else if (adjustment <= 0.4) {
      return 1.2;
    } else if (adjustment <= 0.7) {
      if(rttCtrl.isStarted()) {
        return 1.3;
      } else {
        return 1.7;
      }
    } else {
      return 2;
    }
  }

  public boolean canSend() {
    if (appCwnd > flightSize) {
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
    appRTTEstimator.update(rtt);
    reportRTT(now, getRTT(), ledbatCwnd.getEstimatedOWD());

    int oneWayDelay = (int) (resp.leecherNetRespT - resp.seederNetRespSendT);
    ledbatCwnd.updateCWND(now, oneWayDelay, flightSize, msgSize);
  }

  public void late(long now, int msgSize, LedbatMsg.Response resp) {
    connHistory.late(now, msgSize);

    int rtt = (int) (now - resp.leecherAppReqSendT);
    appRTTEstimator.update(rtt);
    reportRTT(now, getRTT(), ledbatCwnd.getEstimatedOWD());

    int oneWayDelay = (int) (resp.leecherNetRespT - resp.seederNetRespSendT);
    ledbatCwnd.updateCWND(now, oneWayDelay, flightSize, msgSize);
  }

  public void timeout(long now, int msgSize) {
    flightSize -= msgSize;
    connHistory.timeout(now, msgSize);

    switch (ledbatCwnd.handleLoss(now, getRTT())) {
      case 0:
        //ignore - acceptable
        break;
      case 1:
        //first loss
        rttCtrl.start(now);
      case -1:
        adjust(now, 0); //if ledbat slowed down...it will slow itself
    }

  }

  //**************************************************************************
  public long downloadSpeed(long now) {
    return connHistory.avgThroughput(now);
  }

  public double cwnd() {
    return appCwnd;
  }

  //**************************************************************************
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

    public DownloadThroughput report() {
      long now = System.currentTimeMillis();
      return new DownloadThroughput(requestedThroughput.currentSpeed(now), receivedThroughput.currentSpeed(now),
        lateThroughput.currentSpeed(now), timeoutThroughput.currentSpeed(now));
    }
  }

  public DownloadThroughput report() {
    return connHistory.report();
  }

  public void reportAdjustment(long time, double adjustment, double appCwnd) {
    if (tracker.isPresent()) {
      tracker.get().reportAdjustment(time, adjustment, appCwnd);
    }
  }

  public void reportRTT(long time, long rtt, long owd) {
    if (tracker.isPresent()) {
      tracker.get().reportRTT(time, rtt, owd);
    }
  }
}

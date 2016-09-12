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

import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Handler;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class NetworkQueueLoadProxy {

    private final static Logger LOG = LoggerFactory.getLogger(NetworkQueueLoadProxy.class);
    private String logPrefix = "";

    private final ComponentProxy proxy;
    private final QueueLoad loadTracker;
    private double adjustment;

    public NetworkQueueLoadProxy(String queueName, ComponentProxy proxy, QueueLoadConfig loadConfig) {
        this.proxy = proxy;
        loadTracker = new QueueLoad(loadConfig);
        adjustment = 0.0;
        logPrefix = queueName;
        proxy.subscribe(handleTrackingTimeout, proxy.getNegative(Timer.class).getPair());
        proxy.subscribe(handleTrackingMsg, proxy.getNegative(Network.class).getPair());
    }

    public void start() {
        scheduleLoadCheck();
    }
    
    public void tearDown() {
    }

    public double adjustment() {
        return adjustment;
    }

    public Pair<Integer, Integer> queueDelay() {
        return loadTracker.queueDelay();
    }

    Handler handleTrackingTimeout = new Handler<LoadTrackingTimeout>() {

        @Override
        public void handle(LoadTrackingTimeout event) {
            KHeader header = new BasicHeader(null, null, null);
            KContentMsg msg = new BasicContentMsg(header, new LoadTrackingEvent());
            proxy.trigger(msg, proxy.getNegative(Network.class));
        }
    };

    ClassMatchedHandler handleTrackingMsg
            = new ClassMatchedHandler<LoadTrackingEvent, KContentMsg<KAddress, KHeader<KAddress>, LoadTrackingEvent>>() {
                @Override
                public void handle(LoadTrackingEvent content, KContentMsg<KAddress, KHeader<KAddress>, LoadTrackingEvent> context) {
                    long now = System.currentTimeMillis();
                    int queueDelay = (int)(now - content.sentAt);
                    adjustment = loadTracker.adjustState(queueDelay);
                    LOG.info("{}component adjustment:{} qd:{} avg qd:{}", new Object[]{logPrefix, adjustment, queueDelay, loadTracker.queueDelay()});
                    scheduleLoadCheck();
                }
            };

    private void scheduleLoadCheck() {
        ScheduleTimeout st = new ScheduleTimeout(loadTracker.nextCheckPeriod());
        LoadTrackingTimeout ltt = new LoadTrackingTimeout(st);
        st.setTimeoutEvent(ltt);
        proxy.trigger(st, proxy.getNegative(Timer.class).getPair());
    }

    private static class LoadTrackingTimeout extends Timeout {

        public LoadTrackingTimeout(ScheduleTimeout st) {
            super(st);
        }
    }
}

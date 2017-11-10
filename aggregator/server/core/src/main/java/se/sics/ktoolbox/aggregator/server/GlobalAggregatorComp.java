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
package se.sics.ktoolbox.aggregator.server;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.util.Identifier;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.aggregator.msg.NodeWindow;
import se.sics.ktoolbox.aggregator.server.event.SystemWindow;
import se.sics.ktoolbox.aggregator.util.AggregatorPacket;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;

/**
 * Main global aggregator component.
 *
 * Created by babbarshaer on 2015-09-01.
 */
public class GlobalAggregatorComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalAggregatorComp.class);
    private String logPrefix;

    Positive network = requires(Network.class);
    Positive timer = requires(Timer.class);
    Negative aggregatorPort = provides(GlobalAggregatorPort.class);

    private final SystemKCWrapper systemConfig;
    private final GlobalAggregatorKCWrapper aggregatorConfig;
    
    private UUID aggregationTid;

    private final Table<Identifier, Class, AggregatorPacket> currentWindow = HashBasedTable.create();

    public GlobalAggregatorComp(GlobalAggregatorInit init) {
        systemConfig = new SystemKCWrapper(config());
        aggregatorConfig = new GlobalAggregatorKCWrapper(config());
        logPrefix = "<nid:" + systemConfig.id + ">";
        LOG.info("{}initializing...", logPrefix);

        subscribe(handleStart, control);
        subscribe(handleAggregationTimeout, timer);
        subscribe(handleNodeWindow, network);
    }

    //***************************CONTROL****************************************
    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start start) {
            LOG.info("{}starting...", logPrefix);
            schedulePeriodicAggregation();
        }
    };

    //**************************************************************************
    Handler handleAggregationTimeout = new Handler<AggregationTimeout>() {
        @Override
        public void handle(AggregationTimeout timeout) {
            LOG.trace("{}received:{}", logPrefix, timeout);
            SystemWindow systemWindowEvent = new SystemWindow(currentWindow);
            LOG.trace("{}sending:{}", logPrefix, systemWindowEvent);
            trigger(systemWindowEvent, aggregatorPort);
            currentWindow.clear();
        }
    };

    ClassMatchedHandler handleNodeWindow = new ClassMatchedHandler<NodeWindow, BasicContentMsg<KAddress, BasicHeader<KAddress>, NodeWindow>>() {
        @Override
        public void handle(NodeWindow content, BasicContentMsg<KAddress, BasicHeader<KAddress>, NodeWindow> container) {
            LOG.trace("{}received:{} from:{}", new Object[]{logPrefix, content, container.getSource()});
            for(Map.Entry<Class, AggregatorPacket> packet : content.window.entrySet()) {
                currentWindow.put(container.getSource().getId(), packet.getKey(), packet.getValue());
            }
        }
    };

    public static class GlobalAggregatorInit extends Init<GlobalAggregatorComp> {
        public GlobalAggregatorInit() {
        }
    }

    private void schedulePeriodicAggregation() {
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(aggregatorConfig.aggregationPeriod, aggregatorConfig.aggregationPeriod);
        AggregationTimeout agt = new AggregationTimeout(spt);
        spt.setTimeoutEvent(agt);
        trigger(spt, timer);
        aggregationTid = agt.getTimeoutId();
    }

    public static class AggregationTimeout extends Timeout {

        public AggregationTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }

        @Override
        public String toString() {
            return getClass() + "<" + getTimeoutId() + ">";
        }
    }
}

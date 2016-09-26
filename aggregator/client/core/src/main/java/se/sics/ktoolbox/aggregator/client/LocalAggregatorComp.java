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
package se.sics.ktoolbox.aggregator.client;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.aggregator.client.events.ComponentPacketEvent;
import se.sics.ktoolbox.aggregator.msg.NodeWindow;
import se.sics.ktoolbox.aggregator.util.AggregatorPacket;
import se.sics.ktoolbox.aggregator.util.AggregatorProcessor;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;

/**
 * Main aggregator component used to collect the information from the components
 * locally and then aggregate them.
 *
 * Created by babbar on 2015-08-31.
 */
public class LocalAggregatorComp extends ComponentDefinition {

    private static final Logger LOG = LoggerFactory.getLogger(LocalAggregatorComp.class);
    private String logPrefix;

    Positive network = requires(Network.class);
    Positive timer = requires(Timer.class);
    Negative aggregatorPort = provides(LocalAggregatorPort.class);

    private final SystemKCWrapper systemConfig;
    private final LocalAggregatorKCWrapper aggregatorConfig;
    //<rawPacketClass, packetAggregators>
    private final Multimap<Class, AggregatorProcessor> compPacketAggregators;
    //<aggregatedPacketClass, aggregatedPacket>
    private final Map<Class, AggregatorPacket> currentWindow = new HashMap<>();

    private UUID aggregationTid;

    public LocalAggregatorComp(LocalAggregatorInit init) {
        systemConfig = new SystemKCWrapper(config());
        aggregatorConfig = new LocalAggregatorKCWrapper(config());
        logPrefix = "<nid:" + systemConfig.id + ">";
        LOG.info("{}initializing...", logPrefix);

        compPacketAggregators = init.compPacketAggregators;

        subscribe(handleStart, control);
        subscribe(handleComponentPacket, aggregatorPort);
        subscribe(handleAggregationTimeout, timer);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}started:{}", logPrefix);
            schedulePeriodicAggregation();
        }
    };

    Handler handleComponentPacket = new Handler<ComponentPacketEvent>() {

        @Override
        public void handle(ComponentPacketEvent update) {
            LOG.trace("{}received:{}", logPrefix, update);

            Class rawPacketClass = update.packet.getClass();
            for (AggregatorProcessor ap : compPacketAggregators.get(rawPacketClass)) {
                Optional<AggregatorPacket> oldAggregatedPacket = Optional.fromNullable(currentWindow.get(rawPacketClass));
                AggregatorPacket newAggregatedPacket = ap.process(oldAggregatedPacket, update.packet);
                currentWindow.put(ap.getAggregatedType(), newAggregatedPacket);
            }
        }
    };

    Handler handleAggregationTimeout = new Handler<AggregationTimeout>() {
        @Override
        public void handle(AggregationTimeout timeout) {
            LOG.trace("{}received:{}", logPrefix, timeout);
            sendWindow();
            currentWindow.clear();
        }
    };
    
    private void sendWindow() {
        BasicHeader header = new BasicHeader(aggregatorConfig.localAddress, aggregatorConfig.globalAddress, Transport.UDP);
        NodeWindow content = new NodeWindow(currentWindow);
        BasicContentMsg msg = new BasicContentMsg(header, content);
        LOG.trace("{}sending:{} to:{}", new Object[]{logPrefix, msg.getContent(), msg.getDestination()});
        trigger(msg, network);
    }

    public class LocalAggregatorInit extends Init<LocalAggregatorComp> {

        public final Multimap<Class, AggregatorProcessor> compPacketAggregators;

        public LocalAggregatorInit(Multimap<Class, AggregatorProcessor> compPacketAggregators) {
            this.compPacketAggregators = compPacketAggregators;
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

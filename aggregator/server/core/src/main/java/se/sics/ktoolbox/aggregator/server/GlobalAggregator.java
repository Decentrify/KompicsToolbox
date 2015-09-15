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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.aggregator.util.PacketInfo;
import se.sics.ktoolbox.aggregator.server.event.AggregatedInfo;
import se.sics.ktoolbox.aggregator.server.util.AggregationTimeout;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import se.sics.ktoolbox.aggregator.msg.PacketContainer;

/**
 * Main global aggregator component.
 *
 * Created by babbarshaer on 2015-09-01.
 */
public class GlobalAggregator extends ComponentDefinition {

    Logger logger = LoggerFactory.getLogger(GlobalAggregator.class);
    private long timeout;
    private Map<BasicAddress, List<PacketInfo>> nodePacketMap;

    Positive<Network> network = requires(Network.class);
    Positive<Timer> timer = requires(Timer.class);
    Negative<GlobalAggregatorPort> aggregatorPort = provides(GlobalAggregatorPort.class);


    public GlobalAggregator(GlobalAggregatorInit init){

        doInit(init);
        subscribe(startHandler, control);
        subscribe(timeoutHandler, timer);
        subscribe(packetMsgHandler, network);
    }

    /**
     * Initialize the global aggregator.
     * @param init
     */
    private void doInit(GlobalAggregatorInit init) {

        logger.debug("Initializing the global aggregator.");
        this.timeout = init.timeout;
        this.nodePacketMap = new HashMap<BasicAddress, List<PacketInfo>>();
    }


    /**
     * Start handler for the component. Invoke other components
     * if any in the start handler.
     */
    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start start) {

            logger.debug("Component Started.");
            doStart();
        }
    };

    /**
     * Initiate the start of the system. All the necessary components
     * needs to be booted up before the
     */
    private void doStart() {

        logger.debug("Starting the component.");

        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
        AggregationTimeout agt = new AggregationTimeout(spt);
        spt.setTimeoutEvent(agt);

        trigger(spt, timer);
    }


    /**
     * Handler for aggregation timeout.
     */
    Handler<AggregationTimeout> timeoutHandler = new Handler<AggregationTimeout>() {
        @Override
        public void handle(AggregationTimeout aggregationTimeout) {

            logger.debug("Aggregation timeout handler invoked, forwarding the aggregated information.");
            logger.debug("Node Packet Map :{}", nodePacketMap);

            trigger(new AggregatedInfo(nodePacketMap), aggregatorPort);

//          Clear the map for the next round.
            nodePacketMap = new HashMap<BasicAddress, List<PacketInfo>>();
        }
    };


    /**
     * Network message handler containing the packet information from the
     * node in the system.
     *
     */
    ClassMatchedHandler<PacketContainer, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, PacketContainer>> packetMsgHandler = new ClassMatchedHandler<PacketContainer, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, PacketContainer>>() {
        @Override
        public void handle(PacketContainer packetContainer, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, PacketContainer> event) {

            logger.debug("Handler for the packet container from the node in the system.");
            PacketContainer container = event.getContent();

            BasicAddress sourceAddress = container.sourceAddress.getBase();
            List<PacketInfo> packets = nodePacketMap.get(sourceAddress);

            if(packets == null){
                packets = new ArrayList<PacketInfo>();
                nodePacketMap.put(sourceAddress, packets);
            }

            packets.add(container.packetInfo);
        }
    };

}

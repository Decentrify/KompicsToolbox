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
package se.sics.ktoolbox.aggregator.global.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.aggregator.global.example.system.PseudoPacketInfo;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import se.sics.ktoolbox.aggregator.server.GlobalAggregatorPort;
import se.sics.ktoolbox.aggregator.server.event.AggregatedInfo;
import se.sics.ktoolbox.aggregator.util.PacketInfo;

/**
 * Component impersonating the global aggregator functionality.
 * 
 * Created by babbarshaer on 2015-09-05.
 */
public class PseudoGlobalAggregator extends ComponentDefinition{
    
    Logger logger = LoggerFactory.getLogger(PseudoGlobalAggregator.class);
    InetAddress ipAddress;
    int basePort;
    int baseId;
    Random random = new Random();
    static final int NODES = 10;
    
    Positive<Timer> timer = requires(Timer.class);
    Negative<GlobalAggregatorPort> aggregatorPort = provides(GlobalAggregatorPort.class);
    
    
    public PseudoGlobalAggregator() throws UnknownHostException {
        
        logger.debug("Creating the pseudo aggregator component.");
        ipAddress = InetAddress.getLocalHost();
        basePort = 3000;
        baseId = 0;
        
        subscribe(startHandler, control);
        subscribe(timeoutHandler, timer);
    }
    
    
    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start start) {
            
            logger.debug("Pseudo aggregator component started.");
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(4000, 4000);
            LocalTimeout lt = new LocalTimeout(spt);
            spt.setTimeoutEvent(lt);
            
            trigger(spt, timer);
        }
    };


    /**
     * Handler for the timeout for the
     */
    Handler<LocalTimeout> timeoutHandler = new Handler<LocalTimeout>() {
        @Override
        public void handle(LocalTimeout localTimeout) {
            
            logger.debug("Time to push test data to the visualizer.");
            Map<Integer, List<PacketInfo>> packetMap = createTestData(NODES);
            trigger(new AggregatedInfo(System.currentTimeMillis(), packetMap), aggregatorPort);
        }
    };


    /**
     * Create a map representing the state of the system.
     * The data about system can be fabricated.
     *
     * @return Map.
     */
    private Map<Integer, List<PacketInfo>> createTestData (int nodes){
        
        int port = basePort;
        int identifier = baseId;
        Map<Integer, List<PacketInfo>> nodePacketMap = new HashMap<Integer, List<PacketInfo>>();
        
        while(nodes > 0){
            
            BasicAddress address = new BasicAddress(ipAddress, port, identifier);
            PacketInfo packetInfo = new PseudoPacketInfo(random.nextFloat(), random.nextFloat());
            
            List<PacketInfo> list = new ArrayList<PacketInfo>();
            list.add(packetInfo);
            
            nodePacketMap.put(address.getId(), list);
            
            port ++;
            identifier ++;
            nodes --;
        }
        
        return nodePacketMap;
    }


    /**
     * Timeout informing the application about the
     * pushing of the system packet map to the component 
     * above the aggregator.
     */
    public static class LocalTimeout extends Timeout{

        public LocalTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }
    }

}

package se.sics.ktoolbox.aggregator.global.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.aggregator.common.PacketInfo;
import se.sics.ktoolbox.aggregator.global.example.system.PseudoPacketInfo;
import se.sics.ktoolbox.aggregator.server.api.event.AggregatedInfo;
import se.sics.ktoolbox.aggregator.server.api.ports.GlobalAggregatorPort;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

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
            Map<BasicAddress, List<PacketInfo>> packetMap = createTestData(NODES);
            trigger(new AggregatedInfo(packetMap), aggregatorPort);
        }
    };


    /**
     * Create a map representing the state of the system.
     * The data about system can be fabricated.
     *
     * @return Map.
     */
    private Map<BasicAddress, List<PacketInfo>> createTestData (int nodes){
        
        int port = basePort;
        int identifier = baseId;
        Map<BasicAddress, List<PacketInfo>> nodePacketMap = new HashMap<BasicAddress, List<PacketInfo>>();
        
        while(nodes > 0){
            
            BasicAddress address = new BasicAddress(ipAddress, port, identifier);
            PacketInfo packetInfo = new PseudoPacketInfo(random.nextFloat(), random.nextFloat());
            
            List<PacketInfo> list = new ArrayList<PacketInfo>();
            list.add(packetInfo);
            
            nodePacketMap.put(address, list);
            
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

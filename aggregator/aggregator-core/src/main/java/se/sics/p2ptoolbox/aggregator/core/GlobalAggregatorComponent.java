package se.sics.p2ptoolbox.aggregator.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.timer.SchedulePeriodicTimeout;
import se.sics.gvod.timer.Timeout;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.*;
import se.sics.p2ptoolbox.aggregator.api.model.AggregatedStatePacket;
import se.sics.p2ptoolbox.aggregator.api.msg.AggregatedStateContainer;
import se.sics.p2ptoolbox.aggregator.api.msg.GlobalState;
import se.sics.p2ptoolbox.aggregator.api.port.GlobalAggregatorPort;

import java.util.HashMap;
import java.util.Map;

/**
 * Component Having the God's View of the System.
 * In essence it's a simple data collection service.
 *
 * Created by babbarshaer on 2015-03-15.
 */
public class GlobalAggregatorComponent extends ComponentDefinition{
    
    private long mapCleaningTimeout;
    private Map<VodAddress, AggregatedStatePacket> statePacketMap;
    private Logger logger = LoggerFactory.getLogger(GlobalAggregatorComponent.class);
    private Positive<Timer> timerPort = requires(Timer.class);
    private Positive<VodNetwork> networkPort = requires(VodNetwork.class);
    private Negative<GlobalAggregatorPort> globalAggregatorPort = provides(GlobalAggregatorPort.class);
    
    public GlobalAggregatorComponent(GlobalAggregatorComponentInit init){
        doInit(init);
        subscribe(startHandler, control);
        subscribe(mapCleanTimeout, timerPort);
        subscribe(aggregatedStateMsgHandler, networkPort);
    }
    
    private void doInit(GlobalAggregatorComponentInit init) {
        statePacketMap = new HashMap<VodAddress, AggregatedStatePacket>();
        mapCleaningTimeout = init.getTimeout();
    }
    
    
    private class CleanOldEntries extends Timeout{
        protected CleanOldEntries(SchedulePeriodicTimeout request) {
            super(request);
        }
    }
    
    
    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            logger.info("Started the aggregator component.");
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(mapCleaningTimeout, mapCleaningTimeout);
            spt.setTimeoutEvent(new CleanOldEntries(spt));

            trigger(spt, timerPort);
        }
    };
    
    Handler<CleanOldEntries> mapCleanTimeout = new Handler<CleanOldEntries>() {
        @Override
        public void handle(CleanOldEntries event) {
            logger.info("Clean Map of old entries timeout");
            
            // For now trigger the update here .
            trigger(new GlobalState(statePacketMap), globalAggregatorPort);
        }
    };
    
    Handler<AggregatedStateContainer> aggregatedStateMsgHandler = new Handler<AggregatedStateContainer>() {
        @Override
        public void handle(AggregatedStateContainer event) {
            logger.debug("Received aggregated state message from : " + event.getAddress());
            statePacketMap.put(event.getAddress(), event.getPacketInfo());
        }
    };


}

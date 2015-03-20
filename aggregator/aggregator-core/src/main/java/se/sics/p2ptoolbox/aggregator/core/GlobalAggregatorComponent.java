package se.sics.p2ptoolbox.aggregator.core;

import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.timer.SchedulePeriodicTimeout;
import se.sics.gvod.timer.Timeout;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.*;
import se.sics.p2ptoolbox.aggregator.api.model.AggregatedStatePacket;
import se.sics.p2ptoolbox.aggregator.api.msg.GlobalState;
import se.sics.p2ptoolbox.aggregator.api.msg.Ready;
import se.sics.p2ptoolbox.aggregator.api.port.GlobalAggregatorPort;
import se.sics.p2ptoolbox.aggregator.core.msg.AggregatorNetMsg;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Component Having the God's View of the System.
 * In essence it's a simple data collection service.
 *
 * Created by babbarshaer on 2015-03-15.
 */
public class GlobalAggregatorComponent extends ComponentDefinition{
    
    private long updateTimeout;
    private long windowTimeout;
    private Map<VodAddress, Pair<AggregatedStatePacket, Boolean>> statePacketMap;
    private Logger logger = LoggerFactory.getLogger(GlobalAggregatorComponent.class);
    private Positive<Timer> timerPort = requires(Timer.class);
    private Positive<VodNetwork> networkPort = requires(VodNetwork.class);
    private Negative<GlobalAggregatorPort> globalAggregatorPort = provides(GlobalAggregatorPort.class);
    
    public GlobalAggregatorComponent(GlobalAggregatorComponentInit init){
        doInit(init);
        subscribe(startHandler, control);
        subscribe(pushUpdateHandler, timerPort);
        subscribe(windowRefreshTimeoutHandler, timerPort);
        subscribe(aggregatedStateMsgHandler, networkPort);
    }
    
    private void doInit(GlobalAggregatorComponentInit init) {
        statePacketMap = new HashMap<VodAddress, Pair<AggregatedStatePacket, Boolean>>();
        updateTimeout = init.getTimeout();
        windowTimeout = init.getWindowTimeout();
    }
    
    
    private class UpdateTimeout extends Timeout{
        protected UpdateTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }
    }
    
    private class WindowRefreshTimeout extends Timeout{
        protected WindowRefreshTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }
    }
    
    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            logger.info("Started the aggregator component.");
            
            logger.info("Triggering periodic update timeout.");
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(updateTimeout, updateTimeout);
            spt.setTimeoutEvent(new UpdateTimeout(spt));

            trigger(spt, timerPort);
            
            logger.info("Triggering window timeout.");
            SchedulePeriodicTimeout wrpt = new SchedulePeriodicTimeout(windowTimeout, windowTimeout);
            spt.setTimeoutEvent(new WindowRefreshTimeout(wrpt));
            
            trigger(wrpt, timerPort);
            
            trigger(new Ready(), globalAggregatorPort);
        }
    };
    
    Handler<UpdateTimeout> pushUpdateHandler = new Handler<UpdateTimeout>() {
        @Override
        public void handle(UpdateTimeout event) {

            logger.info("Triggering update to application.");
            Map<VodAddress, AggregatedStatePacket> updatedMap = new HashMap<VodAddress, AggregatedStatePacket>();
            
            for(Map.Entry<VodAddress, Pair<AggregatedStatePacket, Boolean>> entry : statePacketMap.entrySet()){
                updatedMap.put(entry.getKey(), entry.getValue().getValue0());
            }
            
            trigger(new GlobalState(updatedMap), globalAggregatorPort);
        }
    };
    
    
    Handler<WindowRefreshTimeout> windowRefreshTimeoutHandler = new Handler<WindowRefreshTimeout>() {
        @Override
        public void handle(WindowRefreshTimeout event) {

            Iterator<Map.Entry<VodAddress, Pair<AggregatedStatePacket, Boolean>>> iterator = statePacketMap.entrySet().iterator();
            while(iterator.hasNext()){
                
                Map.Entry<VodAddress, Pair<AggregatedStatePacket, Boolean>> entry = iterator.next();

                if(entry.getValue().getValue1()){
                    statePacketMap.put(entry.getKey(), entry.getValue().setAt1(false));     // Reset Refreshed State of Entries.
                }
                else{
                    iterator.remove();
                }
            }
        }
    };
    
    
    
    Handler<AggregatorNetMsg.OneWay> aggregatedStateMsgHandler = new Handler<AggregatorNetMsg.OneWay>() {
        @Override
        public void handle(AggregatorNetMsg.OneWay event) {
            logger.debug("Received aggregated state message from : " + event.getVodSource());
            statePacketMap.put(event.content.getAddress(), Pair.with(event.content.getPacketInfo(), true));
        }
    };


}

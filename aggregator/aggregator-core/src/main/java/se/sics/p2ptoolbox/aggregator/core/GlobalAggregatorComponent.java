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
import se.sics.p2ptoolbox.aggregator.api.msg.GlobalState;
import se.sics.p2ptoolbox.aggregator.api.msg.Ready;
import se.sics.p2ptoolbox.aggregator.api.port.GlobalAggregatorPort;
import se.sics.p2ptoolbox.aggregator.core.msg.AggregatorNetMsg;

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
        subscribe(pushUpdateHandler, timerPort);
        subscribe(aggregatedStateMsgHandler, networkPort);
    }
    
    private void doInit(GlobalAggregatorComponentInit init) {
        statePacketMap = new HashMap<VodAddress, AggregatedStatePacket>();
        mapCleaningTimeout = init.getTimeout();
    }
    
    
    private class UpdateTimeout extends Timeout{
        protected UpdateTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }
    }
    
    
    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            logger.info("Started the aggregator component.");
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(mapCleaningTimeout, mapCleaningTimeout);
            spt.setTimeoutEvent(new UpdateTimeout(spt));

            trigger(spt, timerPort);
            trigger(new Ready(), globalAggregatorPort);
        }
    };
    
    Handler<UpdateTimeout> pushUpdateHandler = new Handler<UpdateTimeout>() {
        @Override
        public void handle(UpdateTimeout event) {

            logger.info("Triggering update to application.");
            trigger(new GlobalState(statePacketMap), globalAggregatorPort);
        }
    };
    
    Handler<AggregatorNetMsg.OneWay> aggregatedStateMsgHandler = new Handler<AggregatorNetMsg.OneWay>() {
        @Override
        public void handle(AggregatorNetMsg.OneWay event) {
            logger.debug("Received aggregated state message from : " + event.getVodSource());
            statePacketMap.put(event.content.getAddress(), event.content.getPacketInfo());
        }
    };


}

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
import se.sics.p2ptoolbox.aggregator.api.msg.AggregatedStateContainer;
import se.sics.p2ptoolbox.aggregator.api.msg.GlobalState;
import se.sics.p2ptoolbox.aggregator.api.msg.Ready;
import se.sics.p2ptoolbox.aggregator.api.port.GlobalAggregatorPort;
import se.sics.p2ptoolbox.aggregator.core.msg.AggregatorNetMsg;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;

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
    private Map<DecoratedAddress, AggregatedStatePacket> statePacketMap;
    private Logger logger = LoggerFactory.getLogger(GlobalAggregatorComponent.class);
    private Positive<Timer> timerPort = requires(Timer.class);
    private Positive<VodNetwork> networkPort = requires(VodNetwork.class);
    private Negative<GlobalAggregatorPort> globalAggregatorPort = provides(GlobalAggregatorPort.class);
    
    public GlobalAggregatorComponent(GlobalAggregatorComponentInit init){
        doInit(init);
        subscribe(startHandler, control);
        subscribe(pushUpdateHandler, timerPort);
        subscribe(aggregatedStateMsgHandler, networkPort);
//        subscribe(aggregatedStateMsgHandler, networkPort);
    }
    
    private void doInit(GlobalAggregatorComponentInit init) {
        statePacketMap = new HashMap<DecoratedAddress, AggregatedStatePacket>();
        updateTimeout = init.getTimeout();
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
            
            logger.info("Triggering periodic update timeout.");
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(updateTimeout, updateTimeout);
            spt.setTimeoutEvent(new UpdateTimeout(spt));

            trigger(spt, timerPort);
            trigger(new Ready(), globalAggregatorPort);
        }
    };
    
    Handler<UpdateTimeout> pushUpdateHandler = new Handler<UpdateTimeout>() {
        @Override
        public void handle(UpdateTimeout event) {

            logger.info("Triggering update to application.");
            Map<DecoratedAddress, AggregatedStatePacket> updatedMap = new HashMap<DecoratedAddress, AggregatedStatePacket>();
            
            for(Map.Entry<DecoratedAddress, AggregatedStatePacket> entry : statePacketMap.entrySet()){
                updatedMap.put(entry.getKey(), entry.getValue());
            }

            trigger(new GlobalState(updatedMap), globalAggregatorPort);
            statePacketMap.clear();
        }
    };


    /**
     * Handler for the aggregated state packet information received from the nodes in the system.
     *
     */
    ClassMatchedHandler aggregatedStateMsgHandler = new ClassMatchedHandler<AggregatedStateContainer, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, AggregatedStateContainer>>() {
        @Override
        public void handle(AggregatedStateContainer aggregatedStateContainer, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, AggregatedStateContainer> event) {

            AggregatedStateContainer container = event.getContent();
            statePacketMap.put(container.getAddress(), container.getPacketInfo());
        }
    };


}

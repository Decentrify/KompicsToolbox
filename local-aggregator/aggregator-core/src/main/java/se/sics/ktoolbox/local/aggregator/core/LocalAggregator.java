package se.sics.ktoolbox.local.aggregator.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.aggregator.local.api.AggregationTimeout;
import se.sics.ktoolbox.aggregator.local.api.ComponentInfo;
import se.sics.ktoolbox.aggregator.local.api.ComponentInfoProcessor;
import se.sics.ktoolbox.aggregator.local.api.events.ComponentInfoEvent;
import se.sics.ktoolbox.aggregator.local.api.ports.LocalAggregatorPort;

import java.util.HashMap;
import java.util.Map;

/**
 * Main aggregator component used to collect the information
 * from the components locally and then aggregate them.
 *
 * Created by babbar on 2015-08-31.
 */
public class LocalAggregator extends ComponentDefinition {

    private Logger logger = LoggerFactory.getLogger(LocalAggregator.class);
    private Map<Class, Map<Integer, ComponentInfo>> componentInfoMap;
    private long aggregationTimeout;
    private Map<Class, ComponentInfoProcessor> componentInfoProcessorMap;

    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);
    private Negative<LocalAggregatorPort> aggregatorPort = provides(LocalAggregatorPort.class);


    public LocalAggregator(LocalAggregatorInit init) {

        doInit(init);
        subscribe(startHandler, control);
        subscribe(infoEventHandler, aggregatorPort);
        subscribe(aggregationTimeoutHandler, timer);
    }

    public void doInit(LocalAggregatorInit init){

        logger.debug("Initializing the component.");
        this.componentInfoMap = new HashMap<Class, Map<Integer, ComponentInfo>>();
        this.aggregationTimeout = init.aggregationTimeout;
        this.componentInfoProcessorMap = init.componentInfoProcessorMap;
    }


    /**
     * Main start handler for the component.
     */
    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            logger.debug("Component started.");

            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(aggregationTimeout, aggregationTimeout);
            AggregationTimeout agt = new AggregationTimeout(spt);
            spt.setTimeoutEvent(agt);

            trigger(spt, timer);
        }
    };

    /**
     * Simply collecting and aggregating the information from the
     * various components in the application.
     *
     */
    Handler<ComponentInfoEvent> infoEventHandler = new Handler<ComponentInfoEvent>() {

        @Override
        public void handle(ComponentInfoEvent event) {

            logger.debug("Handler for the Information event from the component");

            Class componentInfoClass  = event.componentInfo.getClass();
            Map<Integer, ComponentInfo> result = componentInfoMap.get(componentInfoClass);

            if(result == null){
                result = new HashMap<Integer, ComponentInfo>();
                componentInfoMap.put(componentInfoClass, result);
            }

            result.put(event.overlayId, event.componentInfo);
        }
    };


    /**
     * Handler indicating the timeout of the aggregation event.
     * The information aggregated needs to be processed.
     */
    Handler<AggregationTimeout> aggregationTimeoutHandler = new Handler<AggregationTimeout>() {
        @Override
        public void handle(AggregationTimeout event) {

            logger.debug("Timeout handler for the aggregation event.");
            processAggregatedComponentInfo();   // Initiate the processing of the aggregated component information.
        }
    };


    /**
     * Once the aggregation process if over, the information
     * needs to be processed by passing through the various processors registered by the application.
     */
    private void processAggregatedComponentInfo(){
        throw new UnsupportedOperationException("Operation not supported yet.");
    }


}

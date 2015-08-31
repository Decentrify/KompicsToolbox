package se.sics.ktoolbox.aggregator.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.aggregator.local.events.ComponentInfoEvent;
import se.sics.ktoolbox.aggregator.local.ports.LocalAggregatorPort;

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

    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);
    private Negative<LocalAggregatorPort> aggregatorPort = provides(LocalAggregatorPort.class);

    public LocalAggregator(LocalAggregatorInit init) {

        doInit(init);
        subscribe(startHandler, control);
        subscribe(infoEventHandler, aggregatorPort);
    }

    public void doInit(LocalAggregatorInit init){

        logger.debug("Initializing the component.");
        this.componentInfoMap = new HashMap<Class, Map<Integer, ComponentInfo>>();
    }


    /**
     * Main start handler for the component.
     */
    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            logger.debug("Component started.");
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
}

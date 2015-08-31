package se.sics.ktoolbox.local.aggregator.core;

import se.sics.kompics.Init;
import se.sics.ktoolbox.aggregator.local.api.ComponentInfoProcessor;

import java.util.Map;

/**
 * Initialization class for the Local Aggregator Component.
 * Created by babbar on 2015-08-31.
 */
public class LocalAggregatorInit  extends Init<LocalAggregator>{

    public final long aggregationTimeout;
    public final Map<Class, ComponentInfoProcessor> componentInfoProcessorMap;

    public LocalAggregatorInit(long aggregationTimeout, Map<Class, ComponentInfoProcessor> componentInfoProcessorMap){
        this.aggregationTimeout = aggregationTimeout;
        this.componentInfoProcessorMap = componentInfoProcessorMap;
    }

}

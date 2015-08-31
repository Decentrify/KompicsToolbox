package se.sics.ktoolbox.local.aggregator.core;

import se.sics.kompics.Init;
import se.sics.ktoolbox.aggregator.local.api.ComponentInfoProcessor;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.Map;

/**
 * Initialization class for the Local Aggregator Component.
 * Created by babbar on 2015-08-31.
 */
public class LocalAggregatorInit  extends Init<LocalAggregator>{

    public final long aggregationTimeout;
    public final Map<Class, ComponentInfoProcessor> componentInfoProcessorMap;
    public final DecoratedAddress globalAggregatorAddress;
    public final DecoratedAddress selfAddress;

    public LocalAggregatorInit(long aggregationTimeout, Map<Class, ComponentInfoProcessor> componentInfoProcessorMap, DecoratedAddress globalAggregatorAddress, DecoratedAddress selfAddress){

        this.aggregationTimeout = aggregationTimeout;
        this.componentInfoProcessorMap = componentInfoProcessorMap;
        this.globalAggregatorAddress = globalAggregatorAddress;
        this.selfAddress = selfAddress;
    }

}

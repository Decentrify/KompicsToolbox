package se.sics.ktoolbox.aggregator.local.core;

import se.sics.kompics.Init;
import se.sics.ktoolbox.aggregator.global.api.system.ComponentInfo;
import se.sics.ktoolbox.aggregator.local.api.ComponentInfoProcessor;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.Iterator;
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
        assert validateProcessorMap(componentInfoProcessorMap);

        this.aggregationTimeout = aggregationTimeout;
        this.componentInfoProcessorMap = componentInfoProcessorMap;
        this.globalAggregatorAddress = globalAggregatorAddress;
        this.selfAddress = selfAddress;
    }

    /**
     * Check if the map that is being supplied to the local aggregator is correct in terms
     * the processor being set correctly for the correct input class of the component.
     *
     * @param componentInfoProcessorMap map
     * @return valida
     */
    private boolean validateProcessorMap(Map<Class, ComponentInfoProcessor> componentInfoProcessorMap) {
        try {
            Iterator<Map.Entry<Class, ComponentInfoProcessor>> it = componentInfoProcessorMap.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<Class, ComponentInfoProcessor> element = it.next();
                cheatCheck(element.getKey(), element.getValue());
            }
            return true;
        } catch(ClassCastException ex) {
            return false;
        }
    }

    /**
     * It is a cheat checking in which we check that the class for the
     * component processor is correct in regard to the Input Processor.
     *
     * @param ciClass input class
     * @param ciProcessor processor
     * @param <CI_I> Input Component Info
     * @param <CI_O> Output Component Info
     */
    private <CI_I extends ComponentInfo, CI_O extends ComponentInfo> void  cheatCheck(Class<CI_I> ciClass, ComponentInfoProcessor<CI_I, CI_O> ciProcessor) {
    }
}

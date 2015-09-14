package se.sics.ktoolbox.aggregator.local.core;

import se.sics.kompics.Init;
import se.sics.ktoolbox.aggregator.global.api.system.ComponentInfo;
import se.sics.ktoolbox.aggregator.global.api.system.PacketInfo;
import se.sics.ktoolbox.aggregator.local.api.ComponentInfoProcessor;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Initialization class for the Local Aggregator Component.
 * Created by babbar on 2015-08-31.
 */
public class LocalAggregatorInit  extends Init<LocalAggregator>{

    public final long aggregationTimeout;
    public final Map<Class, List<ComponentInfoProcessor>> componentInfoProcessorMap;
    public final DecoratedAddress globalAggregatorAddress;
    public final DecoratedAddress selfAddress;

    public LocalAggregatorInit(long aggregationTimeout, Map<Class, List<ComponentInfoProcessor>> componentInfoProcessorMap, DecoratedAddress globalAggregatorAddress, DecoratedAddress selfAddress){
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
     * @return validated processor map.
     */
    private boolean validateProcessorMap(Map<Class, List<ComponentInfoProcessor>> componentInfoProcessorMap) {
        try {

            for (Map.Entry<Class, List<ComponentInfoProcessor>> element : componentInfoProcessorMap.entrySet()) {
                List<ComponentInfoProcessor> processorList = element.getValue();
                for (ComponentInfoProcessor processor : processorList) {
                    cheatCheck(element.getKey(), processor);
                }
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
    private <CI_I extends ComponentInfo, CI_O extends PacketInfo> void  cheatCheck(Class<CI_I> ciClass, ComponentInfoProcessor<CI_I, CI_O> ciProcessor) {
    }
}

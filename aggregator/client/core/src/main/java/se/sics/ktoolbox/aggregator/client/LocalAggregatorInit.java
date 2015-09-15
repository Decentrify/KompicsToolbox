/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * KompicsToolbox is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.ktoolbox.aggregator.client;

import se.sics.kompics.Init;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.List;
import java.util.Map;
import se.sics.ktoolbox.aggregator.client.util.ComponentInfo;
import se.sics.ktoolbox.aggregator.client.util.ComponentInfoProcessor;
import se.sics.ktoolbox.aggregator.util.PacketInfo;

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

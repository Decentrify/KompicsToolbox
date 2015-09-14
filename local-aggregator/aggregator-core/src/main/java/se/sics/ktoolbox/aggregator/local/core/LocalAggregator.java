package se.sics.ktoolbox.aggregator.local.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.aggregator.global.api.system.PacketContainer;
import se.sics.ktoolbox.aggregator.global.api.system.PacketInfo;
import se.sics.ktoolbox.aggregator.local.api.AggregationTimeout;
import se.sics.ktoolbox.aggregator.global.api.system.ComponentInfo;
import se.sics.ktoolbox.aggregator.local.api.ComponentInfoProcessor;
import se.sics.ktoolbox.aggregator.local.api.events.ComponentInfoEvent;
import se.sics.ktoolbox.aggregator.local.api.events.UpdateEvent;
import se.sics.ktoolbox.aggregator.local.api.ports.LocalAggregatorPort;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;

import java.util.*;

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
    private Map<Class, List<ComponentInfoProcessor>> componentInfoProcessorMap;

    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);
    private Negative<LocalAggregatorPort> aggregatorPort = provides(LocalAggregatorPort.class);
    private DecoratedAddress globalAggregatorAddress;
    private DecoratedAddress selfAddress;

    public LocalAggregator(LocalAggregatorInit init) {

        doInit(init);
        subscribe(startHandler, control);
        subscribe(infoEventHandler, aggregatorPort);
        subscribe(updateEventHandler, aggregatorPort);
        subscribe(aggregationTimeoutHandler, timer);
    }

    public void doInit(LocalAggregatorInit init){

        logger.debug("Initializing the component.");
        this.componentInfoMap = new HashMap<Class, Map<Integer, ComponentInfo>>();
        this.aggregationTimeout = init.aggregationTimeout;
        this.componentInfoProcessorMap = init.componentInfoProcessorMap;
        this.globalAggregatorAddress  = init.globalAggregatorAddress;
        this.selfAddress = init.selfAddress;
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
     * Handler for the update event from the application
     * indicating the change in the self address.
     *
     */
    Handler<UpdateEvent> updateEventHandler = new Handler<UpdateEvent>() {
        @Override
        public void handle(UpdateEvent event) {

            logger.debug("Update event from the application indicating update to the address of the node.");
            selfAddress = event.selfAddress;

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
            ComponentInfo componentInfo = event.componentInfo;

            Map<Integer, ComponentInfo> result = componentInfoMap.get(componentInfoClass);

            if(result == null){
                result = new HashMap<Integer, ComponentInfo>();
                componentInfoMap.put(componentInfoClass, result);
            }

//          Append feature added to allow the application to send information to be aggregated in case
//          the application is sending the data which needs to be aggregated before it could be sent.

            else {

                ComponentInfo cInfo = result.get(event.overlayId);
                if(cInfo != null){
                    componentInfo = cInfo.append(componentInfo);
                }
            }

            result.put(event.overlayId, componentInfo);
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
            Collection<PacketInfo> packets = processAggregatedComponentInfo();   // Initiate the processing of the aggregated component information.

            for(PacketInfo packet : packets){
//              Initiate the transfer to the global aggregator.
                shipPacket(packet);
            }
        }
    };


    /**
     * Once the aggregation process if over, the information
     * needs to be processed by passing through the various processors registered by the application.
     */
    private Collection<PacketInfo> processAggregatedComponentInfo() {

        logger.debug("Initiated with the processing of the aggregated component information.");
        List<PacketInfo> packetInfoList = new ArrayList<PacketInfo>();

        for (Map.Entry<Class, List<ComponentInfoProcessor>> entry : componentInfoProcessorMap.entrySet()) {

            Map<Integer, ComponentInfo> infoOverlayMap = componentInfoMap.get(entry.getKey());

            if (infoOverlayMap != null) {

                List<ComponentInfoProcessor> processorList = entry.getValue();
                for(ComponentInfoProcessor processor : processorList){
                    for (ComponentInfo info : infoOverlayMap.values()) {
                        PacketInfo packet = processor.processComponentInfo(info);
                        packetInfoList.add(packet);
                    }
                }

            }
        }

//      AT THIS PACKET INFO LIST SHOULD BE READY AND CAN BE SHIPPED TO THE GLOBAL AGGREGATOR.
        return packetInfoList;
    }


    /**
     * Once the packet information is generated, it needs to be shipped
     * to the global aggregator.
     *
     * @param packetInfo packet information.
     */
    private void shipPacket(PacketInfo packetInfo){

        logger.debug("Going to ship packet :{}, to the global aggregator.", packetInfo);

        if(globalAggregatorAddress == null){
            logger.warn("No global aggregator address specified, so returning.");
            return;
        }

        PacketContainer container = new PacketContainer(UUID.randomUUID(), selfAddress, packetInfo);
        DecoratedHeader<DecoratedAddress> header = new DecoratedHeader<DecoratedAddress>(selfAddress, globalAggregatorAddress, Transport.UDP);
        BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, PacketContainer> contentMsg = new BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, PacketContainer>(header, container);

        trigger(contentMsg, network);
    }



}
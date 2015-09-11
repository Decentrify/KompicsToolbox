package se.sics.ktoolbox.aggregator.global.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.ktoolbox.aggregator.global.api.event.AggregatedInfo;
import se.sics.ktoolbox.aggregator.global.api.event.WindowProcessing;
import se.sics.ktoolbox.aggregator.global.api.ports.GlobalAggregatorPort;
import se.sics.ktoolbox.aggregator.global.api.ports.VisualizerPort;
import se.sics.ktoolbox.aggregator.global.api.system.DesignInfoContainer;
import se.sics.ktoolbox.aggregator.global.api.system.DesignProcessor;
import se.sics.ktoolbox.aggregator.global.api.system.PacketInfo;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;

import javax.naming.OperationNotSupportedException;
import java.util.*;

/**
 * Visualizer component used for providing the visualizations
 * to the end user.
 *
 * Created by babbar on 2015-09-02.
 */
public class Visualizer extends ComponentDefinition {

    private Logger logger = LoggerFactory.getLogger(Visualizer.class);
    private int maxSnapshots;
    private Map<String, DesignProcessor> designerNameMap;

    Negative<VisualizerPort> visualizerPort = provides(VisualizerPort.class);
    Positive<GlobalAggregatorPort> aggregatorPort = requires(GlobalAggregatorPort.class);
    LinkedList<Map<BasicAddress, List<PacketInfo>>> snapshotList;

    public Visualizer(VisualizerInit init){

        doInit(init);
        subscribe(startHandler, control);
        subscribe(aggregatedInfoHandler, aggregatorPort);
        subscribe(windowProcessRequestHandler, visualizerPort);
    }

    /**
     * Perform the initialization tasks.
     * @param init init
     */
    private void doInit(VisualizerInit init) {

        logger.debug("Performing the initialization tasks.");
        this.maxSnapshots = init.maxSnapshots;
        this.designerNameMap = init.designerNameMap;
        this.snapshotList = new LinkedList<Map<BasicAddress, List<PacketInfo>>>();

    }

    /**
     * Handler to be invoked when the component is booted up.
     * All the component creation and other trigger tasks need to be performed here.
     */
    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            logger.debug("Component booted up.");
        }
    };


    /**
     * Handler invoked when the visualizer receives the
     * aggregated information from the global aggregator.
     *
     */
    Handler<AggregatedInfo> aggregatedInfoHandler = new Handler<AggregatedInfo>() {
        @Override
        public void handle(AggregatedInfo event) {

            logger.debug("Received aggregated information from the global aggregator");

            while(snapshotList.size() >= maxSnapshots){
                snapshotList.removeLast();
            }
            
            Map<BasicAddress, List<PacketInfo>> nodePacketMap = event.getNodePacketMap();
            
            if(nodePacketMap.isEmpty()){
                
                logger.warn("Empty map containing no information from the system. Returning.");
                return;
            }
            
            logger.debug("{}", nodePacketMap);
            snapshotList.addFirst(event.getNodePacketMap());
        }
    };


    /**
     * Handler for processing window request from the application.
     */
    Handler<WindowProcessing.Request> windowProcessRequestHandler = new Handler<WindowProcessing.Request>() {
        @Override
        public void handle(WindowProcessing.Request event) {

            logger.debug("Received a request to handle the window processing.");
            logger.debug("Processing Request {}:", event);
            
            DesignProcessor processor = designerNameMap.get(event.getDesigner());

            if(processor == null) {
                logger.error("Unable to locate the designer for the requested one.");
                throw new RuntimeException("Unable to locate the design processor map.");
            }

            logger.debug("Located the design processor, going ahead with processing.");
            Collection<Map<BasicAddress, List<PacketInfo>>> windows = getWindows(event.getStartLoc(), event.getEndLoc());
            DesignInfoContainer container = processor.process(windows);
            
            logger.debug("Processed Container {}: ", container);
//          We now should have the processed information.
            WindowProcessing.Response response = new WindowProcessing.Response(event.getRequestId(), container);
            trigger(response, visualizerPort);
        }
    };


    /**
     * Based on the parameters for the method, arrange the windows in a list.
     *
     * @param start start point.
     * @param end end point
     * @return Window Collection.
     */
    private Collection<Map<BasicAddress, List<PacketInfo>>> getWindows(int start, int end){

        List<Map<BasicAddress, List<PacketInfo>>> result = new ArrayList<Map<BasicAddress, List<PacketInfo>>>();

        if(start > snapshotList.size()){
            return result;
        }

        if(end > snapshotList.size()){
            end = snapshotList.size();
        }

        result.addAll(snapshotList.subList(start, end));
        return result;
    }


}

package se.sics.ktoolbox.aggregator.global.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.ktoolbox.aggregator.global.api.event.AggregatedInfo;
import se.sics.ktoolbox.aggregator.global.api.ports.GlobalAggregatorPort;
import se.sics.ktoolbox.aggregator.global.api.ports.VisualizerPort;
import se.sics.ktoolbox.aggregator.global.api.system.Designer;
import se.sics.ktoolbox.aggregator.global.api.system.PacketInfo;
import se.sics.p2ptoolbox.util.network.impl.BasicAddress;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Visualizer component used for providing the visualizations
 * to the end user.
 *
 * Created by babbar on 2015-09-02.
 */
public class Visualizer extends ComponentDefinition {

    private Logger logger = LoggerFactory.getLogger(Visualizer.class);
    private int maxSnapshots;
    private Map<String, Designer> designerNameMap;

    Negative<VisualizerPort> visualizerPort = provides(VisualizerPort.class);
    Positive<GlobalAggregatorPort> aggregatorPort = requires(GlobalAggregatorPort.class);
    LinkedList<Map<BasicAddress, List<PacketInfo>>> snapshotList;

    public Visualizer(VisualizerInit init){

        doInit(init);
        subscribe(startHandler, control);
        subscribe(aggregatedInfoHandler, aggregatorPort);
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

            snapshotList.addFirst(event.getNodePacketMap());
        }
    };

}

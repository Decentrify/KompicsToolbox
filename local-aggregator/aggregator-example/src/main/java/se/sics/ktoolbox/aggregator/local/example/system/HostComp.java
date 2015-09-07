package se.sics.ktoolbox.aggregator.local.example.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.aggregator.local.api.ComponentInfoProcessor;
import se.sics.ktoolbox.aggregator.local.api.ports.LocalAggregatorPort;
import se.sics.ktoolbox.aggregator.local.core.LocalAggregator;
import se.sics.ktoolbox.aggregator.local.core.LocalAggregatorInit;
import se.sics.ktoolbox.aggregator.local.example.util.AppComponentInfo;
import se.sics.ktoolbox.aggregator.local.example.util.AppProcessor;
import se.sics.p2ptoolbox.util.config.SystemConfig;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main component to bootup the local aggregator component and
 * the application component.
 *
 * Created by babbar on 2015-08-31.
 */
public class HostComp extends ComponentDefinition {

    private Logger  logger = LoggerFactory.getLogger(HostComp.class);
    private SystemConfig systemConfig;
    Component application;
    Component localAggregator;

    private Positive<Network> network=  requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);

    public HostComp(HostCompInit init){

        doInit(init);
        subscribe(startHandler, control);
    }


    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start start) {

            logger.debug("Starting the host component");
            doStart();
        }
    };


    private void doInit(HostCompInit init) {

        logger.debug("Initializing the component after bootup.");
        this.systemConfig = init.systemConfig;
    }

    private void doStart(){

        logger.debug("Going to boot up other components.");

        bootApplication();
        bootLocalAggregator();

    }


    /**
     * Once the application is booted up, bootup the local aggregator component also.
     * The map indicating the processing of the component information needs to be setup
     * and supplied in the init of the creation part of the aggregator.
     */
    private void bootLocalAggregator() {

        Map<Class, List<ComponentInfoProcessor>> processorMap = new HashMap<Class, List<ComponentInfoProcessor>>();
        List<ComponentInfoProcessor> processorList = new ArrayList<ComponentInfoProcessor>();
        processorList.add(new AppProcessor());
        processorMap.put(AppComponentInfo.class, processorList);

        long aggregationTimeout = 5000;

        DecoratedAddress selfAddress = systemConfig.self;
        DecoratedAddress aggregatorAddress = systemConfig.aggregator.isPresent() ? systemConfig.aggregator.get() : null;

        localAggregator = create(LocalAggregator.class, new LocalAggregatorInit(aggregationTimeout, processorMap, selfAddress, aggregatorAddress));
        connect(localAggregator.getNegative(Network.class), network);
        connect(localAggregator.getNegative(Timer.class), timer);
        connect(localAggregator.getPositive(LocalAggregatorPort.class), application.getNegative(LocalAggregatorPort.class));

        trigger(Start.event, localAggregator.control());
    }


    /**
     * Boot the application which is responsible for
     * sending the data to the local aggregator.
     */
    private void bootApplication(){

        application = create(ApplicationComp.class, new ApplicationCompInit(systemConfig.self));
        connect(application.getNegative(Timer.class), timer);

        trigger(Start.event, application.control());
    }

}

package se.sics.ktoolbox.aggregator.local.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.aggregator.local.api.ports.LocalAggregatorPort;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

/**
 * Main application used to test the local aggregator application.
 * Created by babbar on 2015-08-31.
 */
public class ApplicationComp extends ComponentDefinition {


    private Logger logger = LoggerFactory.getLogger(ApplicationComp.class);
    private DecoratedAddress selfAddress;

    Positive<LocalAggregatorPort> aggregatorPort = requires(LocalAggregatorPort.class);
    Positive<Timer> timer = requires(Timer.class);

    public ApplicationComp(ApplicationCompInit init){

        doInit(init);
        subscribe(startHandler, control);
    }

    private void doInit(ApplicationCompInit init) {

        logger.debug("Initializing the component.");
        this.selfAddress = init.selfAddress;
    }



    Handler<Start> startHandler =  new Handler<Start>() {
        @Override
        public void handle(Start event) {
            logger.debug("Component is being booted up.");
        }
    };


}

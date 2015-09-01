package se.sics.ktoolbox.aggregator.local.example.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

/**
 * Main component to bootup the local aggregator component and
 * the application component.
 *
 * Created by babbar on 2015-08-31.
 */
public class HostComp extends ComponentDefinition {

    private Logger  logger = LoggerFactory.getLogger(HostComp.class);
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

    }

    private void doStart(){
        logger.debug("Going to boot up other components.");
    }

}

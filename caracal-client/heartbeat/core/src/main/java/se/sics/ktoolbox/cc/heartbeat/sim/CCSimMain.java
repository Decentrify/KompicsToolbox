package se.sics.ktoolbox.cc.heartbeat.sim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Start;

/**
 * Main class used to replace the actual caracal client.
 * The simulation class is simply used to store the overlay information
 * along with the address of the node that pinged with this information.\
 *
 * Created by babbar on 2015-08-15.
 */
public class CCSimMain extends ComponentDefinition {

    Logger logger = LoggerFactory.getLogger(CCSimMain.class);

    public CCSimMain(CCSimMainInit init){

        doInit(init);
        subscribe(startHandler, control);
    }

    private void doInit(CCSimMainInit init) {
        logger.debug("Perform the initialization tasks.");
    }

    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            logger.debug("Component booted up.");
        }
    };
}

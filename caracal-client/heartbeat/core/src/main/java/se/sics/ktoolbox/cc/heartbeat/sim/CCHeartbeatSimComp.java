package se.sics.ktoolbox.cc.heartbeat.sim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Start;

/**
 * Simulation version of the Heartbeat component used to
 * contact caracal and request for the nodes addresses.
 *
 * Created by babbar on 2015-08-15.
 */
public class CCHeartbeatSimComp extends ComponentDefinition{

    private Logger logger = LoggerFactory.getLogger(CCHeartbeatSimComp.class);


    public CCHeartbeatSimComp(CCHeartbeatSimInit init){
        doInit(init);
        subscribe(startHandler, control);
    }

    private void doInit(CCHeartbeatSimInit init) {
        logger.debug("Performing the initialization tasks for the Heartbeat simulator component.");
    }


    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            logger.debug("Component booted up.");
        }
    };
}

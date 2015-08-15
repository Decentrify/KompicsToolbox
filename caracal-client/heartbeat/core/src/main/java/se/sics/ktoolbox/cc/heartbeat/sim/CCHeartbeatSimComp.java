package se.sics.ktoolbox.cc.heartbeat.sim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Start;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatPort;
import se.sics.ktoolbox.cc.heartbeat.msg.CCHeartbeat;
import se.sics.ktoolbox.cc.heartbeat.msg.CCOverlaySample;

/**
 * Simulation version of the Heartbeat component used to
 * contact caracal and request for the nodes addresses.
 *
 * Created by babbar on 2015-08-15.
 */
public class CCHeartbeatSimComp extends ComponentDefinition{

    private Logger logger = LoggerFactory.getLogger(CCHeartbeatSimComp.class);
    private Negative<CCHeartbeatPort> heartbeatPort  = provides(CCHeartbeatPort.class);

    public CCHeartbeatSimComp(CCHeartbeatSimInit init){
        doInit(init);
        subscribe(startHandler, control);
        subscribe(startHeartbeatHandler, heartbeatPort);
        subscribe(stopHeartbeatHandler, heartbeatPort);
        subscribe(overlaySampleRequest, heartbeatPort);
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


    Handler<CCHeartbeat.Start> startHeartbeatHandler = new Handler<CCHeartbeat.Start>() {
        @Override
        public void handle(CCHeartbeat.Start event) {
            logger.debug("Received starting of heartbeat request from the application.");
        }
    };


    Handler<CCHeartbeat.Stop> stopHeartbeatHandler = new Handler<CCHeartbeat.Stop>() {
        @Override
        public void handle(CCHeartbeat.Stop event) {
            logger.debug("Received stopping of heartbeat request from the application.");
        }
    };


    Handler<CCOverlaySample.Request> overlaySampleRequest = new Handler<CCOverlaySample.Request>() {
        @Override
        public void handle(CCOverlaySample.Request event) {
            logger.debug("Received overlay sample request from the application.");
        }
    };


}

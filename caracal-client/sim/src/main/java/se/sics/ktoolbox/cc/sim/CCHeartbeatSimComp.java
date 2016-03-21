package se.sics.ktoolbox.cc.sim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatPort;
import se.sics.ktoolbox.cc.sim.msg.OverlaySample;
import se.sics.ktoolbox.cc.sim.msg.PutRequest;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;
import se.sics.p2ptoolbox.util.network.impl.DecoratedHeader;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import se.sics.ktoolbox.cc.heartbeat.event.CCHeartbeat;
import se.sics.ktoolbox.cc.heartbeat.event.CCOverlaySample;

/**
 * Simulation version of the Heartbeat component used to
 * contact caracal and request for the nodes addresses.
 *
 * Created by babbar on 2015-08-15.
 */
public class CCHeartbeatSimComp extends ComponentDefinition{

    private Logger logger = LoggerFactory.getLogger(CCHeartbeatSimComp.class);
    private Negative<CCHeartbeatPort> heartbeatPort  = provides(CCHeartbeatPort.class);
    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);

    private DecoratedAddress caracalClientAddress;
    private DecoratedAddress selfAddress;
    private Set<byte[]> heartbeats;

    public CCHeartbeatSimComp(CCHeartbeatSimInit init){
        doInit(init);
        subscribe(startHandler, control);
        subscribe(startHeartbeatHandler, heartbeatPort);
        subscribe(stopHeartbeatHandler, heartbeatPort);
        subscribe(overlaySampleRequest, heartbeatPort);
        subscribe(overlaySampleResponse, network);
        subscribe(heartbeatTimeout, timer);
    }

    private void doInit(CCHeartbeatSimInit init) {
        logger.debug("Performing the initialization tasks for the Heartbeat simulator component.");
        this.selfAddress = init.selfAddress;
        this.caracalClientAddress = init.caracalClientAddress;
        this.heartbeats = new HashSet<byte[]>();
    }


    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            logger.debug("Component booted up.");
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(3000, 3000);
            spt.setTimeoutEvent(new HeartbeatTimeout(spt));
            trigger(spt, timer);
        }
    };


    /**
     * Initiating the heartbeats for a particular
     * service identified by the overlay identifier.
     */
    Handler<CCHeartbeat.Start> startHeartbeatHandler = new Handler<CCHeartbeat.Start>() {
        @Override
        public void handle(CCHeartbeat.Start event) {
            logger.debug("Received starting of heartbeat request from the application.");
            heartbeats.add(event.overlayId);
        }
    };


    /**
     * Handler indicating the stopping of the heartbeat
     * for a particular overlay identifier.
     */
    Handler<CCHeartbeat.Stop> stopHeartbeatHandler = new Handler<CCHeartbeat.Stop>() {
        @Override
        public void handle(CCHeartbeat.Stop event) {
            logger.debug("Received stopping of heartbeat request from the application.");
            heartbeats.remove(event.overlayId);
        }
    };


    /**
     * Time to send the service identifiers that are collected to the
     * caracal client to be stored there.
     */
    Handler<HeartbeatTimeout> heartbeatTimeout = new Handler<HeartbeatTimeout>() {
        @Override
        public void handle(HeartbeatTimeout event) {
            logger.trace("Triggering registered heartbeats to simulated caracal client.");

            PutRequest request = new PutRequest(selfAddress, new HashSet<byte[]>(heartbeats));
            DecoratedHeader<DecoratedAddress> header = new DecoratedHeader<DecoratedAddress>(selfAddress, caracalClientAddress, Transport.UDP);
            BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, PutRequest> contentMsg = new BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, PutRequest>(header, request);

            trigger(contentMsg, network);
        }
    };


    /**
     * Received overlay sample request from the application, forward it to the
     * caracal client.
     */
    Handler<CCOverlaySample.Request> overlaySampleRequest = new Handler<CCOverlaySample.Request>() {
        @Override
        public void handle(CCOverlaySample.Request event) {

            logger.debug("Received overlay sample request from the application.");
            DecoratedHeader<DecoratedAddress> header = new DecoratedHeader<DecoratedAddress>(selfAddress, caracalClientAddress, Transport.UDP);
            OverlaySample.Request content = new OverlaySample.Request(event.overlayId);
            BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, OverlaySample.Request> contentMsg = new BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, OverlaySample.Request>(header, content);

            logger.debug("Triggering the request to the simulated version of caracal client with address: {} over the network.", caracalClientAddress);
            trigger(contentMsg, network);
        }
    };



    /**
     * Handler for the response over the network from the caracal client
     * containing the information about the additional nodes under the same service.
     */
    ClassMatchedHandler<OverlaySample.Response, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, OverlaySample.Response>> overlaySampleResponse = new ClassMatchedHandler<OverlaySample.Response, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, OverlaySample.Response>>() {
        @Override
        public void handle(OverlaySample.Response content, BasicContentMsg<DecoratedAddress, DecoratedHeader<DecoratedAddress>, OverlaySample.Response> context) {
            logger.debug("Received overlay sample response from the caracal client.");
            
            //TODO Alex - UUID match with request - fix - remember requests
            CCOverlaySample.Request request  = new CCOverlaySample.Request(UUID.randomUUID(), content.overlayIdentifier);
            CCOverlaySample.Response response = request.answer(content.neighbors);
            trigger(response, heartbeatPort);
        }
    };

    /**
     * Timeout class indicating the transfer of heartbeats from the application to
     * the main caracal client.
     */
    public class HeartbeatTimeout extends Timeout {

        public HeartbeatTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }
    }
}

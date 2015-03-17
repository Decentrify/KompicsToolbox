package se.sics.p2ptoolbox.aggregator.example.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.timer.SchedulePeriodicTimeout;
import se.sics.gvod.timer.Timeout;
import se.sics.gvod.timer.Timer;
import se.sics.kompics.*;
import se.sics.p2ptoolbox.aggregator.api.msg.AggregatedStateContainer;
import se.sics.p2ptoolbox.aggregator.core.msg.AggregatorNetMsg;

import java.util.UUID;

/**
 * Peer Component disseminating aggregated information to the aggregator.
 *
 * Created by babbarshaer on 2015-03-17.
 */
public class Peer extends ComponentDefinition{
    
    Logger logger = LoggerFactory.getLogger(Peer.class);
    Positive<Timer> timerPort = requires(Timer.class);
    Positive<VodNetwork> networkPort = requires(VodNetwork.class);
    VodAddress selfAddress;
    VodAddress aggregatorAddress;
    
    private int partitionId = 0;
    private int nodeId;
    private int indexEntries;
    private int partitioningDepth = 0;
    
    private long delay;
    
    public Peer(PeerInit init){
        
        this.delay = init.delay;
        this.selfAddress = init.selfAddress;
        this.aggregatorAddress = init.aggregatorAddress;
        this.nodeId = selfAddress.getId();
        
        subscribe(startHandler, control);
        subscribe(stateTimeoutHandler, timerPort);
    }
    
    public class StateTimeout extends Timeout{

        protected StateTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }
    }

    /**
     * Start Handler for the Peer Component.
     */
    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            logger.debug("Peer Booted Up");
            
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(delay, delay);
            StateTimeout st = new StateTimeout(spt);
            spt.setTimeoutEvent(st);
            
            trigger(spt, timerPort);
        }
    };

    /**
     * Regular Information Update to the Aggregate Handler.
     */
    Handler<StateTimeout> stateTimeoutHandler = new Handler<StateTimeout>() {
        @Override
        public void handle(StateTimeout event) {

            logger.debug(" State Timeout Handler Invoked ");
            
            // FIXME: Redundant Wrapper as message going with the source address. Fix it once the linking is complete.
            PacketSample packetSample = new PacketSample(partitioningDepth, indexEntries++, partitionId, nodeId);
            AggregatedStateContainer container = new AggregatedStateContainer(selfAddress, packetSample);
            
            trigger(new AggregatorNetMsg.OneWay(selfAddress, aggregatorAddress, UUID.randomUUID(), container), networkPort);
        }
    };
    
    public static class PeerInit extends Init<Peer>{
        
        public long delay;
        public VodAddress selfAddress;
        public VodAddress aggregatorAddress;
        
        public PeerInit(long delay, VodAddress selfAddress, VodAddress aggregatorAddress){
            this.delay = delay;
            this.selfAddress = selfAddress;
            this.aggregatorAddress = aggregatorAddress;
        }
    }
}

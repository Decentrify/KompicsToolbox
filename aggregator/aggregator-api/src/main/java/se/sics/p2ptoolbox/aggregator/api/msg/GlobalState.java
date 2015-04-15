package se.sics.p2ptoolbox.aggregator.api.msg;

import se.sics.gvod.net.VodAddress;
import se.sics.kompics.KompicsEvent;
import se.sics.p2ptoolbox.aggregator.api.model.AggregatedStatePacket;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.Map;

/**
 * Event containing the Global State of the System. 
 *
 * Created by babbarshaer on 2015-03-15.
 */
public class GlobalState implements KompicsEvent{
   
    private Map<DecoratedAddress, AggregatedStatePacket> statePacketMap;
    
    public GlobalState(Map<DecoratedAddress, AggregatedStatePacket> statePacketMap){
        this.statePacketMap = statePacketMap;
    }
    
    public Map<DecoratedAddress, AggregatedStatePacket> getStatePacketMap(){
        return this.statePacketMap;
    }
    
}

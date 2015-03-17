package se.sics.p2ptoolbox.aggregator.api.msg;

import se.sics.gvod.net.VodAddress;
import se.sics.kompics.KompicsEvent;
import se.sics.p2ptoolbox.aggregator.api.model.AggregatedStatePacket;

import java.util.Map;

/**
 * Event containing the Global State of the System. 
 *
 * Created by babbarshaer on 2015-03-15.
 */
public class GlobalState implements KompicsEvent{
   
    private Map<VodAddress, AggregatedStatePacket> statePacketMap;
    
    public GlobalState(Map<VodAddress, AggregatedStatePacket> statePacketMap){
        this.statePacketMap = statePacketMap;
    }
    
    public Map<VodAddress, AggregatedStatePacket> getStatePacketMap(){
        return this.statePacketMap;
    }
    
}

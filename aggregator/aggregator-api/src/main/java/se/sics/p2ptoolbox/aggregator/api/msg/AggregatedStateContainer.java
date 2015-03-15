package se.sics.p2ptoolbox.aggregator.api.msg;

import se.sics.gvod.net.VodAddress;
import se.sics.p2ptoolbox.aggregator.api.model.AggregatedStatePacket;

/**
 * Event triggered by the Peers in the system containing
 * condensed information about there state. 
 *
 * Created by babbarshaer on 2015-03-15.
 */
public class AggregatedStateContainer {

    private VodAddress address;
    private AggregatedStatePacket packet;
    
    public AggregatedStateContainer(VodAddress address, AggregatedStatePacket packet){
        this.address = address;
        this.packet = packet;
    }
    
    public String toString(){
        return "Aggregated State For: " + address.getId() + packet.toString();
    }

    public VodAddress getAddress() {
        return address;
    }
    
    public AggregatedStatePacket getPacketInfo(){
        return this.packet;
    }
}

package se.sics.p2ptoolbox.aggregator.api.msg;

import se.sics.gvod.net.VodAddress;
import se.sics.p2ptoolbox.aggregator.api.model.AggregatedStatePacket;
import se.sics.p2ptoolbox.util.network.impl.DecoratedAddress;

import java.util.UUID;

/**
 * Event triggered by the Peers in the system containing
 * condensed information about there state. 
 *
 * Created by babbarshaer on 2015-03-15.
 */
public class AggregatedStateContainer {

    private UUID uuid;
    private DecoratedAddress address;
    private AggregatedStatePacket packet;
    
    public AggregatedStateContainer(UUID uuid, DecoratedAddress address, AggregatedStatePacket packet){

        this.uuid = uuid;
        this.address = address;
        this.packet = packet;
    }

    public UUID getId(){
        return this.uuid;
    }
    public String toString(){
        return "Aggregated State For: " + address.getId() + " Packet: "+ packet.toString();
    }

    public DecoratedAddress getAddress() {
        return address;
    }
    
    public AggregatedStatePacket getPacketInfo(){
        return this.packet;
    }
}

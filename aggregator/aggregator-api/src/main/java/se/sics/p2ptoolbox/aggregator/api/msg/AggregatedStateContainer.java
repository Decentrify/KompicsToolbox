package se.sics.p2ptoolbox.aggregator.api.msg;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AggregatedStateContainer)) return false;

        AggregatedStateContainer that = (AggregatedStateContainer) o;

        if (address != null ? !address.equals(that.address) : that.address != null) return false;
        if (packet != null ? !packet.equals(that.packet) : that.packet != null) return false;
        if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = uuid != null ? uuid.hashCode() : 0;
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (packet != null ? packet.hashCode() : 0);
        return result;
    }

    public UUID getId(){
        return this.uuid;
    }

    public String toString(){
        return "Aggregated State For: " + address.getId() + " Packet: "+ packet.toString() + " ID: "+ uuid.toString();
    }

    public DecoratedAddress getAddress() {
        return address;
    }
    
    public AggregatedStatePacket getPacketInfo(){
        return this.packet;
    }
}

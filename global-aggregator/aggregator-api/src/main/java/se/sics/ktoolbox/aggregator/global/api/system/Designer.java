package se.sics.ktoolbox.aggregator.global.api.system;

import se.sics.p2ptoolbox.util.network.impl.BasicAddress;

import java.util.Collection;

/**
 * Interface indicating the designer used for
 * a particular type of stats collection and visualtion.
 *
 * Created by babbar on 2015-09-02.
 */
public interface Designer {

    /**
     * Run the collection of the packets through the designer.
     * Each designer will invoke a specific process method on the collection.
     *
     * @param packetInfoCollection collection
     */
    public void process (BasicAddress nodeAddress , Collection<PacketInfo> packetInfoCollection);


    /**
     * Once the designer has processed the collection, the entity
     * that needs to be returned to the user needs to be constructed by it.
     *
     * @return entity object.
     */
    public Object getEntity();

}

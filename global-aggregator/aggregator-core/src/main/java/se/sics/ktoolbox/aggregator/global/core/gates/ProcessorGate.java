package se.sics.ktoolbox.aggregator.global.core.gates;

import se.sics.ktoolbox.aggregator.global.api.PacketInfo;

import java.util.Collection;

/**
 * Interface indicating the processor to be applied on
 * the data received from the user.
 *
 * Created by babbarshaer on 2015-09-01.
 */
public interface ProcessorGate {

    /**
     * Process the packet information from
     * the application.
     *
     * @param packetInfo Packet Information
     */
    public void process(Collection<PacketInfo> packetInfo);

}

package se.sics.p2ptoolbox.aggregator.api.net;

import se.sics.p2ptoolbox.aggregator.api.model.AggregatedStatePacket;
import se.sics.p2ptoolbox.serialization.Serializer;

/**
 * Interface for the Aggregated State Packet to be implemented by the Application.
 * Created by babbarshaer on 2015-03-15.
 */
public interface ASPacketSerializer <E extends AggregatedStatePacket> extends Serializer<E> {
}

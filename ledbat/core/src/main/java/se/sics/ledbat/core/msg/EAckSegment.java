package se.sics.ledbat.core.msg;

import java.util.Set;
import java.util.UUID;
import se.sics.kompics.id.Identifier;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * Author : Ahmad & Serveh
 */
public class EAckSegment extends LedbatMsg {
    public final Identifier eventId;
    public final KAddress sender;
    public final KAddress receiver;
    
    //sequence number which all the sequences before have been received by the server.
    private long ackNumber;
    // one_way_delay from client to the server
    private long receiver_senderDiff;
    // List of out_of_order sequence numbers that have been received by the server
    private Set<Long> sequenceList;
    //for application layer round trip time. contains copy of the same property  in SimpleSegment(senderTimestamp)
    //private long senderTime;

    private UUID connectionId;

    public EAckSegment(UUID connectionId, KAddress sender, KAddress receiver) {
        this.eventId = BasicIdentifiers.eventId();
        this.connectionId = connectionId;
        this.sender = sender;
        this.receiver = receiver;
    }

    public long getReceiver_senderDiff() {
        return receiver_senderDiff;
    }

    public void setReceiver_senderDiff(long receiver_senderDiff) {
        this.receiver_senderDiff = receiver_senderDiff;
    }

    public Set<Long> getSequenceList() {
        return sequenceList;
    }

    public void setSequenceList(Set<Long> sequenceList) {
        this.sequenceList = sequenceList;
    }

    public long getAckNumber() {
        return ackNumber;
    }

    public void setAckNumber(long ackNumber) {
        this.ackNumber = ackNumber;
    }

    public UUID getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(UUID connectionId) {
        this.connectionId = connectionId;
    }

    @Override
    public Identifier getId() {
        return eventId;
    }
}

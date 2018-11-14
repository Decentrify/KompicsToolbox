package se.sics.ledbat.core.msg;

import java.util.UUID;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * @author Ahmad & Serveh
 */
public class AckSegment extends LedbatMsg {
    public final Identifier eventId;
    public final KAddress sender;
    public final KAddress receiver;
    
    //sequence number which all the sequences before have been received by the server.
    private long ackNumber;
    // sequence number that is received by the server    
    private long seqNum;
    // one_way_delay from client to the server
    private long receiver_senderDiff;
    //for application layer round trip time. contains copy of the same property  in SimpleSegment(senderTimestamp)
    //private long senderTime;

    private UUID connectionId;
    
//    private int recvAdWin;

    public AckSegment(Identifier eventId, UUID connectionId, KAddress sender, KAddress receiver) {
        this.eventId = eventId;
        this.connectionId = connectionId;
        this.sender = sender;
        this.receiver = receiver;
    }

    public long getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(long seqNum) {
        this.seqNum = seqNum;
    }

    public long getReceiver_senderDiff() {
        return receiver_senderDiff;
    }

    public void setReceiver_senderDiff(long receiver_senderDiff) {
        this.receiver_senderDiff = receiver_senderDiff;
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

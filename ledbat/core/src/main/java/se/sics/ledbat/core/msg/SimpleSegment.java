package se.sics.ledbat.core.msg;

import java.util.UUID;
import se.sics.kompics.util.Identifier;
import se.sics.ktoolbox.util.identifiable.BasicIdentifiers;
import se.sics.ktoolbox.util.network.KAddress;

/**
 * 
 */
public class SimpleSegment extends LedbatMsg {
    public final Identifier eventId;
    public final KAddress sender;
    public final KAddress receiver;
    
    private byte[] payload ;
    //private long senderTimestamp;
    private long seqNum;
    private long senderRTO;
    private UUID connectionId;// required for the ack that sender gets back

    public SimpleSegment(UUID connectionId, KAddress sender, KAddress receiver) {
        this.eventId = BasicIdentifiers.eventId();
        this.connectionId = connectionId;
        this.sender = sender;
        this.receiver = receiver;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    

    public long getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(long seqNum) {
        this.seqNum = seqNum;
    }

    public long getSenderRTO() {
        return senderRTO;
    }

    public void setSenderRTO(long senderRTO) {
        this.senderRTO = senderRTO;
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

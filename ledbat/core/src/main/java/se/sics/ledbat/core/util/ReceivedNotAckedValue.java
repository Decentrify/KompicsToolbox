package se.sics.ledbat.core.util;

/**
 *
 */
public class ReceivedNotAckedValue {
    //one way delay from sender to receiver
    long one_way_delay;
    //Sender's application layer timestamp . This should be copied from the same property in SimpleSegment (senderTime)
    //long appSenderTimestamp;
    //Sender's UDP layer timestamp.This should be copied from the same property in p2pSockMessage (udpSenderTimestamp)
    long udpSenderTimestamp;
    //Receiver's timestamp that shows the time that this entry was put in the ReceivedNotAckedMap.
    long putTimestamp;



     public ReceivedNotAckedValue(long one_way_delay, long udpSenderTimestamp, long putTimestamp ) {
        this.one_way_delay = one_way_delay;
        this.udpSenderTimestamp = udpSenderTimestamp;
          this.putTimestamp = putTimestamp;
    }

    public long getOne_way_delay() {
        return one_way_delay;
    }

    public void setOne_way_delay(long one_way_delay) {
        this.one_way_delay = one_way_delay;
    }

   public long getUdpSenderTimestamp() {
        return udpSenderTimestamp;
    }

    public void setUdpSenderTimestamp(long udpSenderTimestamp) {
        this.udpSenderTimestamp = udpSenderTimestamp;
    }

    public long getPutTimestamp() {
        return putTimestamp;
    }

    public void setPutTimestamp(long putTimestamp) {
        this.putTimestamp = putTimestamp;
    }
}

package se.sics.ledbat.core.util;

/**
 * Author: Ahmad & Serveh
 * This is the type of Value in the sentNotAcked Map.
 * Or sentNotAcked map is <long, SentNotAckedValue >
 */
public class SentNotAckedValue {

    private byte[] payload;  
    // Number of times that this specific seqNum has been transmitted.
    private int numberOfTransmission;
    /**
     * Time of sending this seq for improving retransmit.not sending a sequence that its sendTime was sooner than RTO.
     */
    private long sendTime;

    public SentNotAckedValue(byte[] payload , int numberOfTransmission) {
        this.payload  = payload ;
        this.numberOfTransmission = numberOfTransmission;
        this.sendTime = System.currentTimeMillis();
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public int getNumberOfTransmission() {
        return numberOfTransmission;
    }

    public void setNumberOfTransmission(int numberOfTransmission) {
        this.numberOfTransmission = numberOfTransmission;
    }

    public long getSendTime() {
        return sendTime;
    }

    public void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }
}

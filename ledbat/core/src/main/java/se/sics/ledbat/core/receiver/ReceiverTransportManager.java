package se.sics.ledbat.core.receiver;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ledbat.core.msg.AckSegment;
import se.sics.ledbat.core.msg.EAckSegment;
import se.sics.ledbat.core.msg.LedbatMsg;
import se.sics.ledbat.core.msg.SimpleSegment;
import se.sics.ledbat.core.util.ReceivedNotAckedValue;
import se.sics.ledbat.core.util.ThroughputHandler;

/**
 * This class keeps information about the connection created between this node as a server and another node as a client.
 * Information such as last in order sequence number that has been received from the client, all received messages that
 * are not acked yet and so on. Each node as server(or receiver of files) should have one instance of
 * ServerTransportManager for each client(or sender of a file) that it's going to connect to.
 * Author Ahmad & Serveh
 */
public class ReceiverTransportManager {

    private UUID connectionId;
    /**
     * This is the address of client(sender) which is the other end of this connection.
     */
    private KAddress senderAddress;
    /**
     * This is the address of this server as source of all messages going out.
     */
    private KAddress sourceAddress;
    /**
     * last sequence received from client that all of the sequences before it are also recieved. This sequence number is
     * actually what is expected by the receiver.
     */
    private long lastInOrderSequence;
    /**
     * This keeps all sequences received from client that are not right after lastInOrderSequence.
     */
    private SortedMap<Long, byte[]> outOfOrderList;
    /**
     * This List contains all the received messages in order of receiving.
     */
    // private List<byte[]> receivingQueue;
    /**
     * contains list of received sequences that are accumulated to be sent at once by one acknowledge .
     */
    private Map<Long, ReceivedNotAckedValue> receivedNotAcked; // Map<Seq#, receivedNotAckedValue>

    private boolean firstPacketReceived;
    /**
     * Timer that when expires, all received accumulated packets from client should be acked in an eack.     
     * this should be started when the first packet is received, and restarted every time that an ack or Eack is sent.
     * when timeout: Send all received packets' ack in one ack or Eack then retart the timer.
     */
    //private ScheduleTimeout cumulativeAckTimer;
    /**
     * Amount of time that cumulativeAckTimer may wait until it sends an eack.
     */
    private long cumulativeAckTimeout;

    /**
     * Each receiver connection has only one cumulativeAckTimeout . This timout should be canceled if an ack or eack is sent.
     */
    private UUID cumulativeAckTimeoutId;

    /**
     * byte[] that can be delivered to application layer, because contents are in order.
     */
    private ByteArrayOutputStream canBeDelivered;

    public static double jmxTotalReceivedPAckets;

    /**
     * Calculates and logs throughput.
     */
    private ThroughputHandler throughputHandler;

    private final ReceiverConfig receiverConfig;
    
    private static final Logger logger = LoggerFactory.getLogger(ReceiverTransportManager.class);

    public ReceiverTransportManager(ReceiverConfig receiverConfig, UUID connectionId, KAddress selfAddress, KAddress senderAddress) {
        this.receiverConfig = receiverConfig;
        this.cumulativeAckTimeout = receiverConfig.cumulativeAckTimeout;
        this.connectionId = connectionId;
        this.sourceAddress = selfAddress;
        this.senderAddress = senderAddress;
        
        throughputHandler = new ThroughputHandler(connectionId.toString());
        initialize();
    }

    private void initialize() {

        lastInOrderSequence = 1;
        receivedNotAcked = new HashMap<Long, ReceivedNotAckedValue>();
        canBeDelivered = new ByteArrayOutputStream();
        outOfOrderList = new TreeMap<Long, byte[]>();
        firstPacketReceived = false;

        /*cumulativeAckTimer = new ScheduleTimeout(cumulativeAckTimeout);
        cumulativeAckTimer.setTimeoutEvent(new CumulativeAckTimeout(cumulativeAckTimer, connectionId));*/
    }


    /**
     * This method handles received segment from client. If cumulating acks is disabled, it just returns an ack for the received segment.
     * If cumulating acks is enabled, it accumulates receiving segments informationa untill number of accumulated segments reach
     * max_cumulative_ack and then it returns one eack for all of them. If number of accumulated segments doesn't reach to the
     * max_cumulative_ack, it returns null.
     *
     * @param event       : segment that is received by server
     * @return an AckSegment if cumulateAck option is off(cumulating acks is disabled).
     *         If cumulateAck option is on, it returns an EAckSegment if number of cumulated acks reached max_cumulative_ack.
     *         If number of cumulative acks is not reached max_cumulative_ack, it returns null.
     */
    public LedbatMsg handleReceivedPacket(SimpleSegment event) {

        updateSequenceList(event);

        if (!receiverConfig.cumulateAck) {
            long one_way_delay = event.getServerDeliveredTimestamp() - event.getUdpSenderTimestamp();
            return prepareAck(event.getSeqNum(), one_way_delay, event.getUdpSenderTimestamp());

        } else {
            //todo :  do not update delay, just read it from config
            //todo : divided to 3, for faster acknowledging
            cumulativeAckTimeout = event.getSenderRTO() / 2;


            if (!firstPacketReceived) {
                firstPacketReceived = true;
            }

            Map<Long, ReceivedNotAckedValue> receivedPackets = cumulatePacket(event);

            if (receivedPackets != null) {
                Set<Long> keys = receivedPackets.keySet();
                if (keys.size() > 0) {
                    ReceivedNotAckedValue segmentVal = null;
                    for (Long key : keys) {
                        segmentVal = receivedPackets.get(key);
                        break; // we only need information of one of the packets.
                    }
                    long waitingTime = System.currentTimeMillis() - segmentVal.getPutTimestamp();
                    // because of HashMap$keySet NotSerializable error
                    return prepareEAck(new HashSet(keys), segmentVal.getOne_way_delay(), segmentVal.getUdpSenderTimestamp() + waitingTime);
                }
            }
            return null;
        }
    }



    /**
     * This method is called if cumulateAck option is on.
     * Server accumulates received packet. If number of accumulated packets reaches max_cumulative_ack, it returns all the
     * accumulated segments along with their information such as time of receiving the packet.
     * If the accumulator is not full(number of accumulated segments is less than max_cumulative_ack), it returns null;
     *
     * @param event     : received packet
     * @return a list of  sequence numbers to be sent in an eack if list is complete otherwise returns null.
     */
    public Map<Long, ReceivedNotAckedValue> cumulatePacket(SimpleSegment event) {
        long one_way_delay = event.getServerDeliveredTimestamp() - event.getUdpSenderTimestamp();
        ReceivedNotAckedValue value = new ReceivedNotAckedValue(one_way_delay, event.getUdpSenderTimestamp(), System.currentTimeMillis());
        receivedNotAcked.put(event.getSeqNum(), value);
        if (receivedNotAcked.size() >= receiverConfig.maxCumulativeAck) { // time to send an eack
            Map<Long, ReceivedNotAckedValue> receivedPackets = new HashMap<Long, ReceivedNotAckedValue>(receivedNotAcked);
            receivedNotAcked.clear();
            return receivedPackets;
        } else {
            // cumulative list is not full, send nothing!
            return null;
        }
    }


    private AckSegment prepareAck(long sequenceNumber, long one_way_delay, long udpSenderTime) {

        AckSegment ackMessage = new AckSegment(connectionId, sourceAddress, senderAddress);
        ackMessage.setUdpSenderTimestamp(udpSenderTime);
        ackMessage.setReceiver_senderDiff(one_way_delay);
        ackMessage.setSeqNum(sequenceNumber);
        ackMessage.setAckNumber(getLastInOrderSequence());
        return ackMessage;
    }


    private EAckSegment prepareEAck(Set<Long> sequenceNumbers, long one_way_delay, long udpSenderTime) {

        EAckSegment eackMessage = new EAckSegment(connectionId, sourceAddress, senderAddress);
        eackMessage.setUdpSenderTimestamp(udpSenderTime);
        eackMessage.setReceiver_senderDiff(one_way_delay);
        eackMessage.setSequenceList(sequenceNumbers);
        eackMessage.setAckNumber(getLastInOrderSequence());
        return eackMessage;
    }

    int counter = 0;

    /**
     * This method updates lastInOrderSequence if thisSeq is the sequence which server was waiting for. If thisSeq
     * comes out of order, it'll go in the outOfOrderList.It also keeps the actual contents of received message in
     * receivingQueue , in order to deliver the whole message to upper component.
     *
     * @param event @link{SimpleSegment}
     */
    public void updateSequenceList(SimpleSegment event) {
        long thisSeq = event.getSeqNum();
        byte[] messageContents = event.getPayload();
        throughputHandler.packetReceived(event.getPayload().length);
        jmxTotalReceivedPAckets++;
        //logger.warn("Seq received " + event.getSeqNum() + " what expected : " + lastInOrderSequence);
       // if (!outOfOrderList.containsKey(thisSeq) && thisSeq >= lastInOrderSequence ){

            if (isSeqWhatExpected(thisSeq)) {
                lastInOrderSequence = thisSeq + messageContents.length;
                canBeDelivered.write(messageContents, 0, messageContents.length);

                //whenever lastInOrderSequence is update, the whole outOfOrderList should be checked
                Iterator<Map.Entry<Long, byte[]>> it = outOfOrderList.entrySet().iterator();
                while ((it.hasNext())) {
                    Map.Entry<Long, byte[]> smallestOutofOrder = it.next();
                    if (isSeqWhatExpected(smallestOutofOrder.getKey())) {
                        lastInOrderSequence = smallestOutofOrder.getKey() + smallestOutofOrder.getValue().length;
                        canBeDelivered.write(smallestOutofOrder.getValue(), 0, smallestOutofOrder.getValue().length);
                        //smallestOutofOrder = new AbstractMap.SimpleEntry<Long,byte[]>(0l, new byte[1100]);
                        it.remove();
                    } else {
                        break;
                    }
                }

            } else { // thisSeq is not in order
                if (thisSeq > lastInOrderSequence) { // and it's not a retransmitted packet
                    outOfOrderList.put(thisSeq, messageContents);
                }
            }
        //}
    }

    /**
     * returns chunk of the file that is in order and ready to deliver, then resets the temporary container canBeDelivered.
     * @return byte[] that can be delivered to application layer, because contents are in order
     */
    public byte[] getMessageToDeliver() {
        byte[] output = canBeDelivered.toByteArray();
        //todo : check first if delivery is ok
        canBeDelivered.reset();
        return output;
    }

    /**
     * This method checks if seq is the sequence which server was waiting for.
     *
     * @param seq : the arrived sequence
     * @return true if seq is what server was waiting for (In other words if this seq comes in order), otherwise false.
     */
    private boolean isSeqWhatExpected(long seq) {
        return seq == lastInOrderSequence;
    }

    /**
     * @return lastInOrderSequence
     */
    public long getLastInOrderSequence() {
        return lastInOrderSequence;
    }


    /**
     * returns whatever is accumulated in the receivedNotAcked list in order to be sent to the client in an ack or eack.
     * Then clears the receivedNotAcked list.
     *
     * @return null if there's nothing to send back to client.
     *         an ack if there's only one packet to be acked.
     *         an eack if there's more than one packet to be acked.
     */
    public LedbatMsg handleCumulativeAckTimeout() {
        Map<Long, ReceivedNotAckedValue> receivedPackets = new HashMap<Long, ReceivedNotAckedValue>(receivedNotAcked);
        receivedNotAcked.clear();

        Set<Long> keys = receivedPackets.keySet();
        if (keys.size() > 0) {
            ReceivedNotAckedValue segmentVal = null;
            long seq = 0;
            for (Long key : keys) {
                segmentVal = receivedPackets.get(key);
                seq = key;
                break; // we only need information of one of the packets.
            }
            long waitingTime = System.currentTimeMillis() - segmentVal.getPutTimestamp();

            if (receivedPackets.entrySet().size() == 1) { //send an ack
                return prepareAck(seq, segmentVal.getOne_way_delay()
                        , segmentVal.getUdpSenderTimestamp() + waitingTime);

            } else if (receivedPackets.entrySet().size() > 1) { //send an eack
                // because of HashMap$keySet NotSerializable error
                return prepareEAck(new HashSet(keys), segmentVal.getOne_way_delay(),
                       segmentVal.getUdpSenderTimestamp() + waitingTime);
            }
        }
        return null;
    }

    public boolean isCumulateAck() {
        return receiverConfig.cumulateAck;
    }

    public boolean isFirstPacketIsReceived() {
        return firstPacketReceived;
    }

    public long getCumulativeAckTimeout() {
        return cumulativeAckTimeout;
    }
    //just for testCase

    public void setCanBeDelivered(ByteArrayOutputStream canBeDelivered) {
        this.canBeDelivered = canBeDelivered;
    }

    public void setLastInOrderSequence(long lastInOrderSequence) {
        this.lastInOrderSequence = lastInOrderSequence;
    }

    public void setOutOfOrderList(SortedMap<Long, byte[]> outOfOrderList) {
        this.outOfOrderList = outOfOrderList;
    }

    public void putInOutOfOrderList(long key, byte[] contents) {
        this.outOfOrderList.put(key, contents);
    }

    public ByteArrayOutputStream getCanBeDelivered() {
        return canBeDelivered;
    }

    public UUID getConnectionId() {
        return connectionId;
    }

    public UUID getCumulativeAckTimeoutId() {
        return cumulativeAckTimeoutId;
    }

    public void setCumulativeAckTimeoutId(UUID cumulativeAckTimeoutId) {
        this.cumulativeAckTimeoutId = cumulativeAckTimeoutId;
    }
}

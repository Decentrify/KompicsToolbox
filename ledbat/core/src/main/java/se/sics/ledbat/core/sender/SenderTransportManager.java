//package se.sics.ledbat.core.sender;
//
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.SortedSet;
//import java.util.TreeMap;
//import java.util.TreeSet;
//import java.util.UUID;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import se.sics.kompics.timer.SchedulePeriodicTimeout;
//import se.sics.kompics.timer.ScheduleTimeout;
//import se.sics.ktoolbox.util.network.KAddress;
//import se.sics.ledbat.core.CongestionWindowHandler;
//import se.sics.ledbat.core.RTTEstimator;
//import se.sics.ledbat.core.msg.AckSegment;
//import se.sics.ledbat.core.msg.EAckSegment;
//import se.sics.ledbat.core.msg.SimpleSegment;
//import se.sics.ledbat.core.msg.event.ChangePriority;
//import se.sics.ledbat.core.msg.event.CheckActivityTimeout;
//import se.sics.ledbat.core.util.SentNotAckedValue;
//import se.sics.ledbat.core.util.ThroughputHandler;
//
///**
// * This class keeps information about the connection created between this node as a client and another node as a server.
// * Information such as last sequence number that has been sent to the server, all sent messages that are not acked yet,
// * size of data that is on the fly, and so on. So Each node as client(or sender of a file) should have one instance of
// * ClientTransportManager for each server(or receiver).
// *
// * @author Ahmad & Serveh
// */
//public class SenderTransportManager {
//
//    private static final Logger logger = LoggerFactory.getLogger(SenderTransportManager.class);
//    public static int RETRANSMISSION_SIZE = 5;
//
//    private UUID connectionId;
//    /**
//     * This is the address of server(receiver) which is the other end of this connection.
//     */
//    private KAddress receiverAddress;
//    /**
//     * This is the address of this client as source of all messages going out.
//     */
//    private KAddress sourceAddress;
//    /**
//     * contains all sequnces that are sent, but not acked yet.On each sending and also on each ack should be updated.
//     */
//    private Map<Long, SentNotAckedValue> sentNotAcked; // <seq# , sentNotAckedValue>
//    /**
//     * sequence number of each sending packet is obtained from this property.On each sending should be incremented.
//     */
//    private long lastSentSeqNum;
//    /**
//     * flightsize is the amount of data outsanding .It is updated after updating cwnd size on each ack by updateFlightsize();
//     */
//    private long flightSize;
//    private CongestionWindowHandler congestionWindowHandler;
//    /**
//     * last ackNumber received in an ack or eack.This is actually last_in_order_seq_number received by server that is returned in an ack or eack.
//     */
//    private long lastAckNumber;
//    /**
//     * number of times that the same lastAckNumber is received in an ack or eack.If this number reaches ledbatConfig.maxDuplicateAcks, loss is considered for next seq#.
//     */
//    private int numOfDuplicateAck;
//    /**
//     * A List which contains all the messages that should be sent over this connection.each member lenght is less than MSS.
//     */
//    private List<byte[]> sendingQueue;
//    private RTTEstimator rttEstimator;
//    /**
//     * this should be started for a specific seq#, if it's not already started for another seq#
//     * after a timeout should be restarted for the first sent_not_acked seq# .
//     * when timeout: If the specified seq# is acked, just restart the timer for the first seq# in sent_not_acked.
//     * If not acked, resend this seq# and all the seq# after it from sent_not_acked and then restart timer.
//     */
//    //private ScheduleTimeout retransmissionTimer;
//    /**
//     * this should be started when the first packet is sent, and restarted every time a packet is sent.
//     * when timeout: send a null segment to keep the connection alive, then restart the timer.
//     */
//    private ScheduleTimeout nullSegmentTimer;
//    /**
//     * This is true when a retransmissionTimer is on and working. when a retransmissionTimer expires, this property is false;
//     * One retransmissionTimer should be always running, except when a loss ocures. The reason is in this situation, cwnd
//     * does not let us doing retransmission for lost packets, so if we restrat retransmissionTimer while we didn't even resend
//     * the lost packet, we will get another loss for the packet that we didn't resend.
//     */
//    private boolean retransmissionTimerRunning;
//    /**
//     * contains list of messages that should be retransmitted.
//     * Container should be sorted in order to restart the retransmit timer for the smallest seq which is going to be sent.
//     */
//    private SortedSet<Long> retransmitQueue;
//
//    private SortedSet<Long> fastRetransmitQueue;
//    /**
//     * This periodic timer is for checking if there's enough long time that no ack is received. If so, and if we are
//     * expecting many acks to come, this means that all those acks are lost. We should reset flightSize to 0 and
//     * retransmit as many sequences as cwnd let us.
//     */
//    private SchedulePeriodicTimeout checkActivityTimer;
//    private long lastAckTime;
//    private int numOfSentPackets;
//    private boolean duplicateAckCheckAllowed;
////    private boolean rttNotInitialised = true;
//
//    private long highestAcknowledgedSequence;
//    private long lastDupAckRetryTime;
//
//    private ThroughputHandler throughPutHandler = new ThroughputHandler("0");
//    public static double jmxNumOfTotalPackets;
//    public static double jmxNumOfRetransmit;
//    public static double jmxLossPackets;
//
//    private final SenderConfig senderConfig;
//    
//    public SenderTransportManager(SenderConfig ledbatConfig, UUID connectionId, KAddress selfAddress, KAddress receiverAddress) {
//        this.senderConfig = ledbatConfig;
//        this.connectionId = connectionId;
//        this.sourceAddress = selfAddress;
//        this.receiverAddress = receiverAddress;
//        //nullSegmentTimeout = Long.valueOf(propertyLoader.getProperty(ClientTransportManager.NULL_SEGMENT_TIMEOUT));
//        //sendOnlyFullPackets = Boolean.valueOf(propertyLoader.getProperty(CongestionWindowHandler.PACKET_SEND_MODE));
//        initialize();
//    }
//
//    private void initialize() {
//
//        duplicateAckCheckAllowed = false;
//        highestAcknowledgedSequence = 0;
//        lastDupAckRetryTime = 0;
//        lastSentSeqNum = 1;
//        sentNotAcked = new TreeMap<>();
//        congestionWindowHandler = new CongestionWindowHandler(senderConfig.ledbatConfig);
//        rttEstimator = new RTTEstimator(senderConfig.ledbatConfig, 10);
//        retransmissionTimerRunning = false;
//        sendingQueue = new LinkedList<byte[]>();
//        retransmitQueue = new TreeSet<Long>();
//        fastRetransmitQueue = new TreeSet<Long>();
//        numOfDuplicateAck = 0;
//
//        //retransmissionTimer  should be started with initial RTO as timeout value and seq# 1.
//        // rttEstimator.getInitialRetransmissionTimeout()
//
//        /*retransmissionTimer = new ScheduleTimeout(1000);
//        retransmissionTimer.setTimeoutEvent(new RetransmissionTimeout(retransmissionTimer, 1,
//                connectionId, rttEstimator.getRetransmissionTimeout()));
//*/
//        checkActivityTimer = new SchedulePeriodicTimeout(1000, 2000);
//        checkActivityTimer.setTimeoutEvent(new CheckActivityTimeout(checkActivityTimer, connectionId));
//    }
//
//    /**
//     * @return initial amount of cwnd
//     */
//    public double getInitialCWND() {
//        return congestionWindowHandler.getInitialCwnd();
//    }
//
//    boolean logMore = false;
//
//    /**
//     * prints the state of this conection.
//     */
//    public void dumpState() {
//        logger.error("flight size :" + getFlightSize());
//        logger.error("sent_not_acked size :" + sentNotAcked.size());
//        logger.error("last sent seq Num: " + lastSentSeqNum);
//        logger.error("retransmiting queue length :" + retransmitQueue.size());
//        logger.error("retransmission timeout delay :" + rttEstimator.getRetransmissionTimeout());
//        logger.error("lastAckNum :" + lastAckNumber);
//        logger.error("Highest acknowledged Seq# :" + highestAcknowledgedSequence);
//        //TODO Alex - see if can reenable next line
////        logger.error("nettynetwork seq :" + NettyNetwork.jmx_client_numOfSend * ledbatConfig.mss);
//        //printSentNotAcked();
//        //printRetransmitQueue();
//        congestionWindowHandler.dumpState();
//        logger.error("RTO : " + rttEstimator.getRetransmissionTimeout());
//
//        logMore = true;
//    }
//
//    public boolean isLogMore() {
//        return logMore;
//    }
//
//    /**
//     * This method divides contents into several parts.Each part's ledbatConfig.maxDuplicateAcksimum size is MSS.Then it puts these partitions
//     * in the sendingQueue List.Whenever client gets chance to send a message over this connection, it will read
//     * contents from sendingQueue.
//     *
//     * @param contents
//     */
//    public void putInSendingQueue(byte[] contents) {
//
//        InputStream is = new ByteArrayInputStream(contents);
//        byte[] buffer = new byte[senderConfig.ledbatConfig.mss];
//        byte[] temp;
//        int n;
//        try {
//            while ((n = is.read(buffer)) != -1) { //end of stream is not reached
//                if (n < senderConfig.ledbatConfig.mss) {
//                    temp = new byte[n];
//                    System.arraycopy(buffer, 0, temp, 0, n);
//                    sendingQueue.add(temp);
//                } else {
//                    sendingQueue.add(buffer);
//                }
//                buffer = new byte[senderConfig.ledbatConfig.mss];
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    int numberOfRetransmit = 0;
//    int numberOfTotalSend = 0;
//
//    /**
//     * Asks congestionWindowHandler about number of packets that client is allowed to sent .
//     * numberOfPacketstoSend = min(cwnd,rwnd) + (highest acknowledged seq) : we shouldn't send packets with #seq greater than that.
//     * First it uses this window size to send retransmissions and then if the window still allows,
//     * it will put new packets.
//     *
//     * @return list of SimpleSegments containing both new packets and retransmissions.
//     */
//    public List<SimpleSegment> prepareAllMessagesToBeSent() {
//        List<SimpleSegment> allMessagesTobeSent = new ArrayList<SimpleSegment>();
//        //long allowedSeqNumber = lastAckNumber + (long) congestionWindowHandler.getCwnd();
//        long allowedSeqNumber = highestAcknowledgedSequence + (long) congestionWindowHandler.getCwnd();
//        /*logger.info("highest Acknoledged sequence: " + highestAcknowledgedSequence);
//        logger.info("congestion window:" + congestionWindowHandler.getCwnd());*/
//        if (logMore) {
//            logger.info("max allowed seq num: " + allowedSeqNumber);
//        }
//        Iterator<Long> retransmitIt = retransmitQueue.iterator();
//
//        int numOfRetransmitsAtOnce = 0;
//        while (retransmitIt.hasNext()) {
//
//            long seq = retransmitIt.next();
//            if (seq <= allowedSeqNumber) {
//                //retransmitIt.remove();  //   commented after condition no more than 50 retransmits at once
//
//                // it's good to double check if the entry in the queue is not acked yet ...
//                if (doublecheckForRetransmit(seq)) { // still should be retransmitted
//                    // By the way, This is not a god idea to send more than 50 messages at once!!!
//                    if (numOfRetransmitsAtOnce <= RETRANSMISSION_SIZE) {
//
//                        retransmitIt.remove();//only if enable to retransmit, remove it from the list
//
//                        byte[] payload = getSegmentContent(seq);
//                        allMessagesTobeSent.add(prepareSimpleSegment(seq, payload));
//                        //logger.warn("Seq " + seq + " is retransmitted ...");
//                        //throughPutHandler.packetSend(payload.length);
//                        jmxNumOfRetransmit++;
//                        jmxNumOfTotalPackets++;
//                        numOfRetransmitsAtOnce++;
//                        //addToFlightSize(payload.length);
//                    } else {//if there are more than 50 to retransmit!!!
//                        logger.info("\n         Didn't send more than " + RETRANSMISSION_SIZE + " at once!!!");
//                        break;
//                    }
//                } else {//if not in the sent_not_acked anymore, remove it from the retransmission list
//                    retransmitIt.remove();
//                }
//            } else {
//                break;
//            }
//        }
//        /* if (numOfRetransmitsAtOnce > 5) {
//            logger.warn("num of retrasmit per Acc: " + numOfRetransmitsAtOnce);
//        }*/
//
//        Iterator<byte[]> sendingIt = sendingQueue.iterator();
//        while (sendingIt.hasNext()) { // we still have some cwnd to fill
//
//            byte[] payload = sendingIt.next();
//            //seq# of the packet that we want to send.
//            long seq = lastSentSeqNum;
//            //long seq = prepareNewSendingPacket(payload);
//            if (seq <= allowedSeqNumber) {
//                prepareNewSendingPacket(payload);
//                sendingIt.remove();
//                allMessagesTobeSent.add(prepareSimpleSegment(seq, payload));
//                jmxNumOfTotalPackets++;
//                //throughPutHandler.packetSend(payload.length);
//                //addToFlightSize(payload.length);
//            } else { // once a new sequence is not allowed, all the other(greater) sequences are not allowed
//                break;
//            }
//        }
//        return allMessagesTobeSent;
//    }
//
//    /**
//     * This method is called when program is about to halt, because all sent segments are lost and no ack is rceived in
//     * the last 3 seconds or so.
//     *
//     * @return the list of first size number of retransmits
//     */
//    public List<SimpleSegment> prepareMessagesExceptionally() {
//        List<SimpleSegment> allMessagesTobeSent = new ArrayList<SimpleSegment>();
//        //long allowedSeqNumber = lastAckNumber + (long) congestionWindowHandler.getCwnd();
//
//        //long allowedSeqNumber = lastSentSeqNum + 2200;
//        congestionWindowHandler.setCwnd(lastSentSeqNum - highestAcknowledgedSequence + 2200);
//        //congestionWindowHandler.setDoLogSlowstart(true);
//        logger.warn("\n CWND set manually to " + congestionWindowHandler.getCwnd());
//        long allowedSeqNumber = highestAcknowledgedSequence + (long) congestionWindowHandler.getCwnd();
//
//        logger.info("highest Acknoledged sequence: " + highestAcknowledgedSequence);
//        logger.info("congestion window:" + congestionWindowHandler.getCwnd());
//        logger.info("max allowed seq num: " + allowedSeqNumber);
//        Iterator<Long> retransmitIt = retransmitQueue.iterator();
//        int numOfRetransmitsAtOnce = 0;
//
//        while (retransmitIt.hasNext()) {
//
//            long seq = retransmitIt.next();
//            if (seq <= allowedSeqNumber) {
//                //retransmitIt.remove();
//
//                // it's good to double check if the entry in the queue is not acked yet ...
//                if (doublecheckForRetransmit(seq)) { // still should be retransmitted
//                    if (numOfRetransmitsAtOnce <= RETRANSMISSION_SIZE) {
//                        retransmitIt.remove();
//
//                        byte[] payload = getSegmentContent(seq);
//                        allMessagesTobeSent.add(prepareSimpleSegment(seq, payload));
//                        throughPutHandler.packetSend(payload.length);
//                        jmxNumOfRetransmit++;
//                        jmxNumOfTotalPackets++;
//                        numOfRetransmitsAtOnce++;
//                        addToFlightSize(payload.length);
//                    } else {
//                        logger.info("\n         Didn't send more than " + RETRANSMISSION_SIZE + " at once!!!");
//                        break;
//                    }
//                } else {
//                    retransmitIt.remove();
//                }
//            } else {
//                break;
//            }
//        }
//
//        Iterator<byte[]> sendingIt = sendingQueue.iterator();
//        while (sendingIt.hasNext()) { // we still have some cwnd to fill
//
//            byte[] payload = sendingIt.next();
//            //seq# of the packet that we want to send.
//            long seq = lastSentSeqNum;
//            //long seq = prepareNewSendingPacket(payload);
//            if (seq <= allowedSeqNumber) {
//                prepareNewSendingPacket(payload);
//                sendingIt.remove();
//                allMessagesTobeSent.add(prepareSimpleSegment(seq, payload));
//                jmxNumOfTotalPackets++;
//                throughPutHandler.packetSend(payload.length);
//                addToFlightSize(payload.length);
//            } else { // once a new sequence is not allowed, all the other(greater) sequences are not allowed
//                break;
//            }
//        }
//        return allMessagesTobeSent;
//
//    }
//
//
//    /**
//     * This method asks congestionWindowHandler about amount of bytes that can be sent and returns this as
//     * number of packets that can be sent.
//     *
//     * @return number of packets that client can send.
//     */
//    private int getAllowedNumOfSegmentsToSend() {
//        double numberOfBytesToSend = congestionWindowHandler.getNumberOfByteToSend(getFlightSize());
//        return (int) numberOfBytesToSend / senderConfig.ledbatConfig.mss;
//        /*if (!sendOnlyFullPackets && (int) numberOfBytesToSend % mss != 0) {
//        partialMessageList.add((int) numberOfBytesToSend % mss);
//        }
//         */
//    }
//
//    /**
//     * makes a simpleSegment packet with given seq and payload .
//     *
//     * @param seq     : sequence of packet
//     * @param payload : contents of packet
//     * @return packet to be sent
//     */
//    private SimpleSegment prepareSimpleSegment(long seq, byte[] payload) {
//        SimpleSegment segmentMessage = new SimpleSegment(connectionId, sourceAddress, receiverAddress);
//        //todo : getting RTO from RUDP layer not Netty
//        //segmentMessage.setUdpSenderTimestamp(System.currentTimeMillis());
//
//        segmentMessage.setPayload(payload);
//        segmentMessage.setSeqNum(seq);
//        segmentMessage.setSenderRTO(rttEstimator.getRetransmissionTimeout());
//        return segmentMessage;
//    }
//
//    /**
//     * client calls this method whenever wants to send a new packet.
//     * It updates flight size and returns seq number of the packet.
//     *
//     * @param messagePayload:contents of the packet that is about to send
//     * @return sequence number of the sending packet
//     */
//    public long prepareNewSendingPacket2(byte[] messagePayload) {
//        long seq = getNextSequenceNumber(messagePayload.length);
//        sentNotAcked.put(seq, new SentNotAckedValue(messagePayload, 1));
//        return seq;
//    }
//
//    private long getNextSequenceNumber(int segmentLenght) {
//        long seq = lastSentSeqNum;
//        lastSentSeqNum += segmentLenght;
//        return seq;
//    }
//
//    public void prepareNewSendingPacket(byte[] messagePayload) {
//        sentNotAcked.put(lastSentSeqNum, new SentNotAckedValue(messagePayload, 1));
//        lastSentSeqNum += messagePayload.length;
//    }
//
//
//    /**
//     * subtracts bytes_newly_acked from flightsize.
//     *
//     * @param bytes_newly_acked :number of bytes that are acked recently
//     */
//    private void reduceFlightSize(long bytes_newly_acked) {
//        if (getFlightSize() - bytes_newly_acked > 0) { // flightsize should not be negative
//            flightSize = flightSize - bytes_newly_acked;
//        } else {
//            //logger.trace("flight size was about to be negative !~!");
//            flightSize = 0;
//            //dumpState();
//        }
//    }
//
//    /**
//     * This method adds numberOfBytes to the flightsize. This should be called whenever a new packettransmit or a retransmit is going to be done.
//     *
//     * @param numberOfBytes : size of the packet that is going to be retransmitted.
//     */
//    private void addToFlightSize(long numberOfBytes) {
//        flightSize += numberOfBytes;
//    }
//
//    /**
//     * If everything is allright flight size would be difference of lastSentSeqNum and lastAckNumber.
//     *
//     * @return number of bytes in flight  if (lastSentSeqNum >= lastAckNumber)
//     *         otherwise returns -1
//     */
//    public long getFlightSize() {
//        //return flightSize;
//        return lastSentSeqNum - highestAcknowledgedSequence;
//    }
//
//    /**
//     * obtains the stored contents of the message with the given sequence number.
//     *
//     * @param sequenceNumber : sequence number of the message which is going to be retransmitted
//     * @return contents of the message with the given sequence number
//     */
//    public byte[] getSegmentContent(long sequenceNumber) {
//        return sentNotAcked.get(sequenceNumber).getPayload();
//    }
//
//    /**
//     * @param seqNum : the sequence number of the packet that is going to be retransmitted .
//     * @return true if sentNotAcked list still contains this seqNum, so still not acked, so retransmit this seqNum
//     *         and returns false otherwise
//     */
//    public boolean doublecheckForRetransmit(long seqNum) {
//        if (sentNotAcked.containsKey(seqNum)) {
//            SentNotAckedValue value = sentNotAcked.get(seqNum);
//            if (System.currentTimeMillis() - value.getSendTime() >= rttEstimator.getRetransmissionTimeout()) {
//                if (logMore) {
//                    logger.info("seq# " + seqNum + " allowed to be retransmitted(more than a RTO before aretransmit of this Seq), and number is " + value.getNumberOfTransmission());
//                }
//                //value.setNumberOfTransmission(value.getNumberOfTransmission() + 1);
//                value.setSendTime(System.currentTimeMillis());
//                return true;
//            }
//        }
//        return false;
//    }
//
//    /**
//     * client calls this method whenever receives an ack from server.
//     * This method calls congestionWindowHandler to update cwnd size if this ack is new.
//     * If the ack is a retransmit ack, it doesn't update cwnd.
//     *
//     * @param ack: Ack message received from server
//     */
//    public void handleReceivedAck(AckSegment ack) {
//        if (logMore) {
//            logger.info("Ack received for seq# " + ack.getSeqNum() + " and ack# " + ack.getAckNumber());
//        }
//        lastAckTime = System.currentTimeMillis();
//        //todo:
//        int sampleRTT = (int) (ack.getUdpReceiverTimestamp() - ack.getUdpSenderTimestamp());
//
//        //int sampleRTT = (int) (System.currentTimeMillis() - ack.getUdpSenderTimestamp());
//        rttEstimator.updateRTO(sampleRTT);
//        //logger.debug(" RTT in udp layer is " + sampleRTT);
//        if (sentNotAcked.containsKey(ack.getSeqNum())) {
//            if (ack.getSeqNum() > highestAcknowledgedSequence) {
//                highestAcknowledgedSequence = ack.getSeqNum();
//            }
//            SentNotAckedValue value = sentNotAcked.remove(ack.getSeqNum());
//            long numberOfBytes = value.getPayload().length;
//
//            congestionWindowHandler.updateCWND(ack.getReceiver_senderDiff(), getFlightSize(), numberOfBytes);
//
//            reduceFlightSize(numberOfBytes * value.getNumberOfTransmission());
//            //reduceFlightSize(numberOfBytes);
//
//        } else {  // if this seq num is not in sent_not_acked, It's either retransmit-ack, or it's acknowledged earlier by an ackNumber.
//            //  so no update of cwnd and also no flight size update
//            //logger.warn("Received ack seq#  " + ack.getSeqNum() + " is not in the sent_not_acked list .");
//            //reduceFlightSize(mss);
//        }
//        evaluateAckNumber(ack.getAckNumber());
//    }
//
//    /**
//     * client calls this method whenever receives an eack from server.
//     * This method calls congestionWindowHandler to update cwnd size if there's a new ack in this eack.
//     * If all of the acknowledges in the eack are retransmit ack, it doesn't update cwnd.
//     *
//     * @param eack: Extended Ack message received from server
//     */
//    public void handleReceivedEAck(EAckSegment eack) {
//
//        if (logMore) {
//            logger.info("Ack received for seq# ");
//            for (long seq : eack.getSequenceList()) {
//                logger.info(String.valueOf(seq));
//            }
//            logger.info(" and ack# " + eack.getAckNumber());
//        }
//        lastAckTime = System.currentTimeMillis();
//        int sampleRTT = (int) (eack.getUdpReceiverTimestamp() - eack.getUdpSenderTimestamp());
//        rttEstimator.updateRTO(sampleRTT);
//        logger.trace(" RTT in udp layer is " + sampleRTT);
//
//        long numberOfAckedBytes = 0;  // for updating cwnd
//        long numberOfTransmittedBytes = 0;  // for updating flight size
//        long payloadLength; // size of payload of each ack
//        for (long seqNum : eack.getSequenceList()) {
//            if (sentNotAcked.containsKey(seqNum)) {
//                SentNotAckedValue value = sentNotAcked.remove(seqNum);
//                payloadLength = value.getPayload().length;
//                numberOfAckedBytes += payloadLength;
//                numberOfTransmittedBytes += value.getNumberOfTransmission() * payloadLength;
//                if (seqNum > highestAcknowledgedSequence) {
//                    highestAcknowledgedSequence = seqNum;
//                }
//            }
//        }
//        if (numberOfAckedBytes != 0) { // so there is some new acks
//
//            congestionWindowHandler.updateCWND(eack.getReceiver_senderDiff(), getFlightSize(), numberOfAckedBytes);
//
//            // Just decrease as we just sent one packet
//            reduceFlightSize(numberOfTransmittedBytes);
//
//        } else { //if all of the eack sequences are not in the sent_not_acked, do not update cwnd and do not update flight size
//            logger.trace("All Received eack sequences are not in the sent_not_acked list .");
//
//        }
//        evaluateAckNumber(eack.getAckNumber());
//    }
//
//
//    /**
//     * This method gets the last_in_order_seq_number received by the server, and checks if client received all the acks for sequences smaller than it.
//     * If there's any sequence smaller than ackNumber in sent_not_acked list,it should be considered acked and removed from the list. This method also checks
//     * concept of duplicate ack: If ackNumber is same as the last ackNumber received from this server,it means that server didn't get next sequence(but it
//     * received  another out of sequence packet ).If client receives ledbatConfig.maxDuplicateAcks number of duplicate acks, the next sequence number is considered lost.
//     *
//     * @param ackNumber :  last_in_order_seq_number received by server. All the sequences before this ackNumber are received by the server.
//     */
//    public void evaluateAckNumber(long ackNumber) {
//        //todo: make method private after test
//
//        if (ackNumber > highestAcknowledgedSequence) {
//            highestAcknowledgedSequence = ackNumber;
//        }
//
//
//        if (ackIsDuplicated(ackNumber) >= senderConfig.maxDuplicateAcks && duplicateAckCheckAllowed) {
//
//            // we should not restart numOfDuplicateAck , because probably soon another ledbatConfig.maxDuplicateAcks will be reached by the same lastAckNumber
//            //This loss is not because of congestion, so should not change cwnd size.just put lost seq# in retransmittingQueue .
//            //logger.warn("Loss occured for seq# " + (ackNumber) + " because of duplicate ack .");
//            long now = System.currentTimeMillis();
//            if (now - lastDupAckRetryTime > rttEstimator.getRetransmissionTimeout()) { //This reduces retransmit
//                //logger.warn("duplicate ack "+ (ackNumber) + " went to the retransmit queue. and server expects " + lastAckNumber );
//                retransmitQueue.add(ackNumber); // the next sequence is lost
//                lastDupAckRetryTime = now;
//            }
//            // retransmitQueue is a set: no duplicate sequences, but after a short time(that is smaller than a RTT ) the same sequence will be rteransmitted: Large retransmit Rate
//            //  good thing is after a retransmission if the same ack number comes, it tries again , instead of waiting for a retransmission timeout
//            //  This never lets a retransmission timeout detect a loss... Bad for slow start
//            numOfDuplicateAck = 0;
//            //printRetransmitQueue();
//
//            //if (duplicateAckCheckAllowed) {// before the first RTO loss, we are not allowed to halve cwnd because of a duplicate ack, otherwise ssthreshold would be really small
//            // This is necessary to back-off for TCP
//            logger.trace("Duplicate ack considered as loss");
//            jmxLossPackets++;
//            congestionWindowHandler.handleLoss(rttEstimator.getRetransmissionTimeout());
//            //reduceFlightSize(mss);
//            //}
//        } else { //ackNumber is new
//            acknowledgeAllSmallerSequences(ackNumber);
//            lastDupAckRetryTime = 0; //In order to let new duplicate ack be put in retransmit queue
//        }
//
//    }
//
//    public void setDuplicateAckCheckAllowed(boolean duplicateAckCheckAllowed) {
//        this.duplicateAckCheckAllowed = duplicateAckCheckAllowed;
//    }
//
//    /**
//     * This method checks if this ackNumber has been received earlier.
//     *
//     * @param ackNumber :  last_in_order_seq_number received by server. All the sequences before this ackNumber are received by the server.
//     * @return true if ackNumber has been received earlier, otherwise returns false.
//     */
//    private int ackIsDuplicated(long ackNumber) {
//        if (ackNumber == lastAckNumber) { // duplicated ack
//            numOfDuplicateAck++;
//            return numOfDuplicateAck;
//        } else if (ackNumber < lastAckNumber) {             // this sequence is acknowleged before by a greater ackNumber implicitly, do not ruin lastAckNumber.
//            return 0;
//        } else {
//            lastAckNumber = ackNumber;
//            numOfDuplicateAck = 0;
//            return 0;
//        }
//    }
//
//    /**
//     * If there's any sequences smaller than ackNumber in the sent_not_acked list, they'll be considered acked.
//     * So they are removed from sent_not_acked list and flight size is updated.
//     *
//     * @param ackNumber :  last_in_order_seq_number received by server. All the sequences before this ackNumber are received by the server.
//     */
//    private void acknowledgeAllSmallerSequences(long ackNumber) {
//        Iterator it = sentNotAcked.keySet().iterator();
//        long seq;
//        while (it.hasNext()) {
//            seq = (Long) it.next();
//            if (seq < ackNumber) { // this seq is acked
//                it.remove();
//                //reduceFlightSize(value.getPayload().length);
//                //reduceFlightSize(value.getPayload().length * value.getNumberOfTransmission());
//            } else {
//                break;
//            }
//        }
//    }
//
//    /**
//     * should be called when a retransmission timer for a specific seqNum is timed out.
//     * It checks for ack arrival of the specifed seqNum. if ack is not received yet, it's considered that the specified
//     * seqNum is lost. So it puts all the sequnece numbers after and including this seqNum in the retransmitQueue .
//     *
//     * @param sequenceNumber : the seqNum that last retransmission timer were turned on for.
//     */
//    public void handleRetransmissionTimeout(long sequenceNumber) {
//
//        retransmissionTimerRunning = false;
//        //logger.warn("handleRetransmit: RTO is: " + rttEstimator.getRetransmissionTimeout());
//        if (sentNotAcked.containsKey(sequenceNumber)) { //if not acked, so loss occured for this sequenceNumber !
//            //logger.warn("Loss Occured for sequence  # " + sequenceNumber + " because of congestion ");
//            /*if (sentNotAcked.keySet().iterator().hasNext()) {
//                logger.warn("First seq in sent_not_acked is " + sentNotAcked.keySet().iterator().next());
//                logger.warn("Server expects " + lastAckNumber);
//            }*/
//
//            //Once first RTO-detected-loss occured and ssthreshold is set to a reasonable large value, a duplicate ack can halve cwnd
//            duplicateAckCheckAllowed = true;
//
//            jmxLossPackets++;
//            // loss is because of congestion. all the sentNotAcked sequences should be retransmitted
//            congestionWindowHandler.handleLoss(rttEstimator.getRetransmissionTimeout());
//            reduceFlightSize(senderConfig.ledbatConfig.mss);
//            //but also reduce number of times that this seq has been transmitted
//            /*int n = sentNotAcked.get(sequenceNumber).getNumberOfTransmission() - 1;
//            sentNotAcked.get(sequenceNumber).setNumberOfTransmission(n);
//*/
//            //flight size should be greater than cwnd to not to send more segments for a while after a loss.
//            //After a while some acks come back and update flight size, so flight size would be less than cwnd and then
//            //sending segment starts again.
//            //flightSize = (long)(0.60 * flightSize); // 0.40 packets are lost
//
//            retransmitQueue.addAll(sentNotAcked.keySet());
//            //retransmitQueue.add(sequenceNumber);
//
//            /*if (logMore) {
//                printRetransmitQueue();
//            }*/
//        }
//    }
//
//    private void printRetransmitQueue() {
//        StringBuilder sb = new StringBuilder();
//        for (Long l : retransmitQueue) {
//            sb.append(l).append(",");
//        }
//        logger.warn("Retransmission list size : " + retransmitQueue.size() + " Vals={" + sb.toString() + "}");
//    }
//
//    private void printSentNotAcked() {
//        StringBuilder sb = new StringBuilder();
//        for (Long l : sentNotAcked.keySet()) {
//            sb.append(l).append(": " + sentNotAcked.get(l).getNumberOfTransmission()).append(", ");
//        }
//        logger.warn("sent not acked,  Vals={" + sb.toString() + "}");
//    }
//
//    /**
//     * returns the smallest sequence number in the sentNotAcked list in order to restart the retransmission timer for.
//     *
//     * @return The smallest sequence number in the sentNotAcked list. or -1 if the sentNotAcked list is empty.
//     */
//    public long getSmallestNotAckedSeq() {
//
//        long smallest = -1;
//        if (sentNotAcked.keySet().iterator().hasNext()) {
//            smallest = sentNotAcked.keySet().iterator().next();
//        }
//        return smallest;
//    }
//
//    public long getRTO() {
//        return rttEstimator.getRetransmissionTimeout();
//    }
//
//
//    public void handleNullSegmentTimeout() {
//    }
//
//    /**
//     * checks if there is long enough time  that no ack is received. if so, it resets flightSize to 0 in order to let client
//     * to do as many retransmissions as possible(cwnd lets).
//     *
//     * @return true if a try for retransmission should be done, otherwise false.
//     */
//    public boolean handleCheckActivityTimeout() {
//        long now = System.currentTimeMillis();
//        if (now - lastAckTime > 1000) {
//            dumpState();
//            return true;
//        }
//        return false;
//    }
//
//    /* public ScheduleTimeout getRetransmissionTimer() {
//            return retransmissionTimer;
//        }
//    */
//    public boolean isRetransmissionTimerRunning() {
//        return retransmissionTimerRunning;
//    }
//
//    public void setRetransmissionTimerRunning(boolean retransmissionTimerRunning) {
//        this.retransmissionTimerRunning = retransmissionTimerRunning;
//    }
//
//    public SchedulePeriodicTimeout getCheckActivityTimer() {
//        return checkActivityTimer;
//    }
//
//    //added just for testCases
//    public void setLastAckNumber(long lastAckNumber) {
//        this.lastAckNumber = lastAckNumber;
//    }
//
//    public UUID getConnectionId() {
//        return connectionId;
//    }
//
//    public void changePriority(ChangePriority.PriorityLevel priorityLevel) {
//        congestionWindowHandler.setTarget(priorityLevel.getTarget()); //todo
//        // congestionWindowHandler.setAllowed_increase(0.5);
//    }
//}

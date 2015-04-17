package se.sics.p2ptoolbox.chunkmanager;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.netty.serialization.Serializers;
import se.sics.kompics.timer.Timer;
import se.sics.p2ptoolbox.chunkmanager.msg.Chunk;
import se.sics.p2ptoolbox.chunkmanager.util.ChunkContainer;
import se.sics.p2ptoolbox.util.config.SystemConfig;
import se.sics.p2ptoolbox.util.network.impl.BasicContentMsg;

/**
 * Created by alidar on 10/21/14.
 */
public class ChunkManagerComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(ChunkManagerComp.class);

    private Positive<Network> network = positive(Network.class);
    private Negative<Network> cm = negative(Network.class);
    private Positive<Timer> timer = positive(Timer.class);

    private HashMap<UUID, ChunkContainer> incompleteReceivedMessages = new HashMap<UUID, ChunkContainer>();
    private HashMap<UUID, ChunkContainer> incompleteReceivedMessagesTimeout = new HashMap<UUID, ChunkContainer>();

    private final SystemConfig systemConfig;
    private final ChunkManagerConfig config;
    private final String logPrefix;

    public ChunkManagerComp(CMInit init) {
        this.systemConfig = init.systemConfig;
        this.config = init.config;
        this.logPrefix = systemConfig.self.toString();
        log.info("{} initiating...", logPrefix);

        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleMessageToSend, cm);
        subscribe(handleIncomingMessage, network);
        subscribe(messageReceiveTimeoutHandler, timer);
    }

    //**************************************************************************
    private Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            log.info("{} starting...", logPrefix);
        }
    };
    private Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            log.info("{} stopping...", logPrefix);
        }
    };
    //**************************************************************************
    
    Handler<BasicContentMsg> handleMessageToSend = new Handler<BasicContentMsg>() {
        @Override
        public void handle(BasicContentMsg msg) {
            log.trace("received:{}", msg);
            Assert.assertEquals(Transport.UDP, msg.getHeader().getProtocol());

            ByteBuf content = Unpooled.buffer();
            ByteBuf header = Unpooled.buffer();
            Serializers.toBinary(msg.getContent(), content);
            Serializers.toBinary(msg.getHeader(), header);
            
            //TODO Alex is this the actual size?
            int headerSize = header.readableBytes();

            //we have to accommodate the headers info of the chunked message as well.
            int datagramContentSize = config.datagramUsableSize - headerSize;
            
            //not possible to make chunks. This is a check to prevent things from going wrong if the fragment
            //threshold size is given too low.
            if (datagramContentSize <= 0) {
                throw new RuntimeException("chunk manager is badly configured");
            }

            int chunkId = 0;
            UUID messageId = UUID.randomUUID();
            int lastChunk = (content.readableBytes() % datagramContentSize == 0 ? content.readableBytes() / datagramContentSize - 1 : content.readableBytes() / datagramContentSize);
            while (content.readableBytes() > 0) {
                byte[] chunkContent = new byte[datagramContentSize];
                content.readBytes(chunkContent);
                Chunk chunk = new Chunk(messageId, chunkId, lastChunk, chunkContent);

                UUID chunkedMessageUUID = UUID.randomUUID();
                int chunkID = 0;

                for (byte[] byteData : fragmentedBytesList) {
                    ChunkedMessage chunkedMsg = new ChunkedMessage(msg.getVodSource(), msg.getVodDestination(),
                            chunkedMessageUUID, chunkID++, fragmentedBytesList.size(), byteData);

                    trigger(chunkedMsg, network);
                }
            } else {

                //logger.trace("ChunkManager created no chunks. " + msg.getClass() + " is small");
                trigger(msg, network);
            }
        }
    };

    Handler<ChunkedMessage> handleIncomingMessage = new Handler<ChunkedMessage>() {
        @Override
        public void handle(ChunkedMessage msg) {

            ChunkContainer chunkContainer = addChunkToReceivedMessages(msg);

            log.trace("Chunk #" + msg.getChunkID() + " for message # "
                    + msg.getMessageID() + ". " + chunkContainer.getChunks().size() + " out of "
                    + msg.getTotalChunks() + " received.");

            if (chunkContainer.isComplete()) {

                cancelTimeout(chunkContainer.getTimeoutId());
                removeStateForChunkContainer(chunkContainer);

                try {
                    ByteBuf fullMessageBytes = chunkContainer.getCombinedBytesOfChunks();

                    DirectMsg defragmentedMessage = convertBytesToMessage(fullMessageBytes);
                    defragmentedMessage.rewritePublicSource(msg.getSource());
                    defragmentedMessage.rewriteDestination(msg.getDestination());

                    if (defragmentedMessage != null) {

                        log.trace("ChunkManager combined " + chunkContainer.getChunks().size()
                                + " fragments to create " + defragmentedMessage.getClass());

                        trigger(defragmentedMessage, cm);
                    } else {
                        log.warn("Unable to convert bytes into a message. Bytes might be corrupted or there is some"
                                + "issue in the Frame Decoder");
                    }

                } catch (Exception e) {
                    log.warn("Problem while combining chunks of the fragmented message: Exception : " + e.getMessage());
                }
            }
        }
    };

    private ChunkContainer addChunkToReceivedMessages(ChunkedMessage msg) {
        ChunkContainer chunkContainer = incompleteReceivedMessages.get(msg.getMessageID());

        if (chunkContainer == null) {
            chunkContainer = new ChunkContainer(msg.getMessageID(), msg.getTotalChunks());
            incompleteReceivedMessages.put(msg.getMessageID(), chunkContainer);

            TimeoutId timeoutId = scheduleTimeoutForMessageReceive(chunkContainer);
            chunkContainer.setTimeoutId(timeoutId);
            incompleteReceivedMessagesTimeout.put(timeoutId, chunkContainer);
        }

        try {
            chunkContainer.addChunk(new Chunk(msg.getChunkID(), msg.getChunkData()));
        } catch (Exception e) {
            log.warn("Unable to add chunk the message. Exception: " + e.getMessage());
        }

        return chunkContainer;
    }

    private DirectMsg convertBytesToMessage(ByteBuf byteBuf) throws Exception {

        return (DirectMsg) msgDecoder.parse(byteBuf);
    }

    final Handler<ChunkedMessageReceiveTimeout> messageReceiveTimeoutHandler = new Handler<ChunkedMessageReceiveTimeout>() {
        @Override
        public void handle(ChunkedMessageReceiveTimeout chunkedMessageReceiveTimeout) {

            ChunkContainer chunkContainer = incompleteReceivedMessagesTimeout.get(chunkedMessageReceiveTimeout.getTimeoutId());
            removeStateForChunkContainer(chunkContainer);
        }
    };

    private void removeStateForChunkContainer(ChunkContainer chunkContainer) {

        if (chunkContainer != null) {
            incompleteReceivedMessages.remove(chunkContainer.getMessageID());
            incompleteReceivedMessagesTimeout.remove(chunkContainer.getTimeoutId());
        }
    }

    private TimeoutId scheduleTimeoutForMessageReceive(ChunkContainer chunkContainer) {

        //start timeout to only receive chunks of a message for a certain time
        ScheduleTimeout st = new ScheduleTimeout(config.getReceiveMessageTimeout());
        st.setTimeoutEvent(new ChunkedMessageReceiveTimeout(st, chunkContainer));

        trigger(st, timer);

        return st.getTimeoutEvent().getTimeoutId();
    }

    private void cancelTimeout(TimeoutId timeoutId) {

        CancelTimeout cancelTimeout = new CancelTimeout(timeoutId);
        trigger(cancelTimeout, timer);
    }

    public class CMInit extends Init<ChunkManagerComp> {

        public final SystemConfig systemConfig;
        public final ChunkManagerConfig config;

        public CMInit(SystemConfig systemConfig, ChunkManagerConfig config) {
            this.systemConfig = systemConfig;
            this.config = config;
        }

    }
}

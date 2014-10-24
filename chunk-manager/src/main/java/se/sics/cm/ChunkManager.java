package se.sics.cm;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.cm.events.ChunkedMessage;
import se.sics.cm.model.Chunk;
import se.sics.cm.model.ChunkContainer;
import se.sics.cm.ports.ChunkManagerPort;
import se.sics.cm.timeout.ChunkedMessageReceiveTimeout;
import se.sics.cm.utils.Fragmenter;
import se.sics.gvod.common.msgs.Encodable;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.net.MsgFrameDecoder;
import se.sics.gvod.net.VodNetwork;
import se.sics.gvod.net.msgs.DirectMsg;
import se.sics.gvod.timer.*;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by alidar on 10/21/14.
 */
public class ChunkManager extends ComponentDefinition {

    Positive<VodNetwork> networkPort = positive(VodNetwork.class);
    Negative<ChunkManagerPort> chunkManagerPort = negative(ChunkManagerPort.class);
    Positive<Timer> timerPort = positive(Timer.class);

    HashMap<UUID, ChunkContainer> incompleteReceivedMessages = new HashMap<UUID, ChunkContainer>();
    HashMap<TimeoutId, ChunkContainer> incompleteReceivedMessagesTimeout = new HashMap<TimeoutId, ChunkContainer>();

    private MsgFrameDecoder msgDecoder;
    ChunkManagerConfiguration config;

    private static final Logger logger = LoggerFactory.getLogger(ChunkManager.class);


    public ChunkManager(ChunkManagerInit init) {

        subscribe(handleMessageToSend, chunkManagerPort);
        subscribe(handleIncomingMessage, networkPort);
        subscribe(messageReceiveTimeoutHandler, timerPort);
        doInit(init);
    }

    void doInit(ChunkManagerInit init) {
        config = init.getConfig();
        try {
            msgDecoder = (MsgFrameDecoder)init.getMsgDecoderClass().newInstance();
        } catch (Exception e) {
            logger.warn("Message decoder passed to chunk manager is invalid: Exception: " + e.getMessage());
        }
    }

    Handler<DirectMsg> handleMessageToSend = new Handler<DirectMsg>() {
        @Override
        public void handle(DirectMsg msg) {

            if (!(msg instanceof Encodable))
                throw new Error("ChunkManager can only serialize instances of Encodable. You need to "
                        + "make this class implement Encodable: " + msg.getClass());

            ByteBuf buffer;
            try {
                buffer = ((Encodable) msg).toByteArray();
            } catch (MessageEncodingException ex) {
                logger.warn("Problem trying to send msg of type: "
                        + msg.getClass().getCanonicalName() + " with src address: "
                        + msg.getSource() + " and dest address: " + msg.getDestination()
                        + " Exception: " + ex.getMessage() + " " + ex.getClass());

                return;
            }

            if (buffer.capacity() > config.getFragmentThreshold()) {
                byte[] msgBytes = buffer.array();

                ArrayList<byte[]> fragmentedBytesList = Fragmenter.getFragmentedByteArray(msgBytes,
                        config.getFragmentThreshold());

                UUID chunkedMessageUUID = UUID.randomUUID();
                int chunkID = 0;

                for (byte[] byteData : fragmentedBytesList) {
                    ChunkedMessage chunkedMsg = new ChunkedMessage(msg.getVodSource(), msg.getVodDestination(),
                            chunkedMessageUUID, chunkID++, fragmentedBytesList.size(), byteData);

                    trigger(chunkedMsg, networkPort);
                }
            }
            else {
                trigger(msg, networkPort);
            }
        }
    };

    Handler<DirectMsg> handleIncomingMessage = new Handler<DirectMsg>() {
        @Override
        public void handle(DirectMsg msg) {

            if(msg instanceof ChunkedMessage) {
                ChunkedMessage chunkedMessage = (ChunkedMessage) msg;
                ChunkContainer chunkContainer = addChunkToReceivedMessages(chunkedMessage);

                if (chunkContainer.isComplete()) {

                    cancelTimeout(chunkContainer.getTimeoutId());
                    removeStateForChunkContainer(chunkContainer);

                    try {
                        ByteBuf fullMessageBytes = chunkContainer.getCombinedBytesOfChunks();

                        DirectMsg defragmentedMessage = convertBytesToMessage(fullMessageBytes);

                        if (defragmentedMessage != null)
                            trigger(defragmentedMessage, chunkManagerPort);
                        else
                            logger.warn("Unable to convert bytes into a message. Bytes might be corrupted or there is some" +
                                    "issue in the Frame Decoder");

                    } catch (Exception e) {
                        logger.warn("Problem while combining chunks of the fragmented message: Exception : " + e.getMessage());
                    }
                }
            }
            else {
                trigger(msg, chunkManagerPort);
            }
        }
    };

    private ChunkContainer addChunkToReceivedMessages(ChunkedMessage msg)  {
        ChunkContainer chunkContainer = incompleteReceivedMessages.get(msg.getMessageID());

        if(chunkContainer == null)
        {
            chunkContainer = new ChunkContainer(msg.getMessageID(), msg.getTotalChunks());
            incompleteReceivedMessages.put(msg.getMessageID(), chunkContainer);

            TimeoutId timeoutId = scheduleTimeoutForMessageReceive(chunkContainer);
            chunkContainer.setTimeoutId(timeoutId);
            incompleteReceivedMessagesTimeout.put(timeoutId, chunkContainer);
        }

        try {
            chunkContainer.addChunk(new Chunk(msg.getChunkID(), msg.getChunkData()));
        } catch (Exception e) {
            logger.warn("Unable to add chunk the message. Exception: " + e.getMessage());
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

        if(chunkContainer != null) {
            incompleteReceivedMessages.remove(chunkContainer.getMessageID());
            incompleteReceivedMessagesTimeout.remove(chunkContainer.getTimeoutId());
        }
    }

    private TimeoutId scheduleTimeoutForMessageReceive(ChunkContainer chunkContainer) {

        //start timeout to only receive chunks of a message for a certain time
        ScheduleTimeout st = new ScheduleTimeout(config.getReceiveMessageTimeout());
        st.setTimeoutEvent(new ChunkedMessageReceiveTimeout(st, chunkContainer));

        trigger(st, timerPort);

        return  st.getTimeoutEvent().getTimeoutId();
    }
    private void cancelTimeout(TimeoutId timeoutId) {

        CancelTimeout cancelTimeout = new CancelTimeout(timeoutId);
        trigger(cancelTimeout, timerPort);
    }
}
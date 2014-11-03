package se.sics.cm;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.cm.events.ChunkedMessage;
import se.sics.cm.model.Chunk;
import se.sics.cm.model.ChunkContainer;
import se.sics.cm.ports.ChunkManagerPort;
import se.sics.cm.timeout.ChunkedMessageReceiveTimeout;
import se.sics.cm.utils.Fragmenter;
import se.sics.gvod.common.msgs.DirectMsgNettyFactory;
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
            DirectMsgNettyFactory.Base.setMsgFrameDecoder(init.getMsgDecoderClass());
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
            //int readableBytes = 0;
            try {

                buffer = ((Encodable) msg).toByteArray();

                /*readableBytes = buffer.readableBytes();

                //extract the bytes that actually contain data.
                ByteBuf trimmedBuffer = Unpooled.buffer(readableBytes);
                buffer.readBytes(trimmedBuffer, 0, readableBytes);
                buffer = trimmedBuffer;*/

            } catch (MessageEncodingException ex) {
                logger.warn("Problem trying to send msg of type: "
                        + msg.getClass().getCanonicalName() + " with src address: "
                        + msg.getSource() + " and dest address: " + msg.getDestination()
                        + " Exception: " + ex.getMessage() + " " + ex.getClass());

                return;
            }

            //we have to accommodate the headers info of the chunked message as well.
            int actualThreshold = config.getFragmentThreshold() -
                    ChunkedMessage.getOverhead(msg.getVodSource(), msg.getVodDestination());

            //not possible to make chunks. This is a check to prevent things from going wrong if the fragment
            //threshold size is given too low.
            if(actualThreshold <=0 ) {

                trigger(msg, networkPort);
                return;
            }


            if (buffer.capacity() > actualThreshold) {

                ArrayList<byte[]> fragmentedBytesList = Fragmenter.getFragmentedByteArray(buffer.array(),
                        actualThreshold);

                logger.trace("ChunkManager created " + fragmentedBytesList.size() +
                        " fragments for " + msg.getClass());

                UUID chunkedMessageUUID = UUID.randomUUID();
                int chunkID = 0;

                for (byte[] byteData : fragmentedBytesList) {
                    ChunkedMessage chunkedMsg = new ChunkedMessage(msg.getVodSource(), msg.getVodDestination(),
                            chunkedMessageUUID, chunkID++, fragmentedBytesList.size(), byteData);

                    trigger(chunkedMsg, networkPort);
                }
            }
            else {

                //logger.trace("ChunkManager created no chunks. " + msg.getClass() + " is small");

                trigger(msg, networkPort);
            }
        }
    };

    Handler<ChunkedMessage> handleIncomingMessage = new Handler<ChunkedMessage>() {
        @Override
        public void handle(ChunkedMessage msg) {

            ChunkContainer chunkContainer = addChunkToReceivedMessages(msg);

            logger.trace("Chunk #" + msg.getChunkID() + " for message # " +
                    msg.getMessageID() + ". " + chunkContainer.getChunks().size() + " out of " +
                    msg.getTotalChunks() + " received.");

            if (chunkContainer.isComplete()) {

                cancelTimeout(chunkContainer.getTimeoutId());
                removeStateForChunkContainer(chunkContainer);

                try {
                    ByteBuf fullMessageBytes = chunkContainer.getCombinedBytesOfChunks();

                    DirectMsg defragmentedMessage = convertBytesToMessage(fullMessageBytes);
                    defragmentedMessage.rewritePublicSource(msg.getSource());
                    defragmentedMessage.rewriteDestination(msg.getDestination());

                    if (defragmentedMessage != null) {

                        logger.trace("ChunkManager combined " + chunkContainer.getChunks().size() +
                                " fragments to create " + defragmentedMessage.getClass());

                        trigger(defragmentedMessage, chunkManagerPort);
                    }
                    else {
                        logger.warn("Unable to convert bytes into a message. Bytes might be corrupted or there is some" +
                                "issue in the Frame Decoder");
                    }

                } catch (Exception e) {
                    logger.warn("Problem while combining chunks of the fragmented message: Exception : " + e.getMessage());
                }
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
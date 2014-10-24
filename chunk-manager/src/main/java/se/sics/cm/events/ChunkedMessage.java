package se.sics.cm.events;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.msgs.RewriteableMsg;

import se.sics.gvod.net.util.UserTypesEncoderFactory;

import java.util.UUID;

/**
 * Created by alidar on 10/22/14.
 */
public class ChunkedMessage extends DirectMsgNetty.Oneway {

    private UUID messageID;
    private int chunkID;
    private int totalChunks;
    private byte[] chunkData;

    public ChunkedMessage(VodAddress source, VodAddress destination, UUID messageID, int chunkID,
                             int totalChunks, byte[] chunkData) {
        super(source, destination);

        this.messageID = messageID;
        this.chunkID = chunkID;
        this.totalChunks = totalChunks;
        this.chunkData = chunkData;
    }


    @Override
    public int getSize() {
        return getHeaderSize();
    }

    @Override
    public RewriteableMsg copy() {
        return new ChunkedMessage(vodSrc, vodDest, messageID, chunkID, totalChunks, chunkData);
    }

    @Override
    public ByteBuf toByteArray() throws MessageEncodingException {
        ByteBuf buffer = createChannelBufferWithHeader();
        buffer.writeLong(messageID.getMostSignificantBits());
        buffer.writeLong(messageID.getLeastSignificantBits());
        buffer.writeInt(chunkID);
        buffer.writeInt(totalChunks);
        UserTypesEncoderFactory.writeArrayBytes(buffer, chunkData);

        return buffer;
    }

    @Override
    public byte getOpcode() {
        return ChunkManagerFrameDecoder.CHUNKED_MESSAGE;
    }

    public UUID getMessageID() {
        return messageID;
    }

    public int getChunkID() {
        return chunkID;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public byte[] getChunkData() {
        return chunkData;
    }
}

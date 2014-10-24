package se.sics.cm.model;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import se.sics.gvod.timer.TimeoutId;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by alidar on 10/21/14.
 */
public class ChunkContainer {

    private UUID messageID;
    private int totalExpectedChunks;
    private HashMap<Integer, Chunk> chunks = new HashMap<Integer, Chunk>();
    private TimeoutId timeoutId;

    public ChunkContainer(UUID messageID, int totalExpectedChunks) {

        this.messageID = messageID;
        this.totalExpectedChunks = totalExpectedChunks;
    }

    private boolean isDuplicateChunk(Chunk chunk) {

        Chunk oldChunk = chunks.get(chunk.getChunkID());
        return oldChunk != null;
    }

    public void addChunk(Chunk chunk) throws Exception {

        if(isDuplicateChunk(chunk))
            return;

        if(chunk.getChunkID() >= totalExpectedChunks)
            throw new Exception("Received invalid chunk. " +
                    "The chunkID is greater than the ID of the last expected chunk");

        chunks.put(chunk.getChunkID(), chunk);
    }

    public boolean isComplete()
    {
        if (totalExpectedChunks == chunks.size())
            return true;

        return false;
    }
/*
    public byte[] getCombinedBytesOfChunks throws IOException {

        if(!isComplete())
            return null;

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();



        for(int i = 0; i < totalExpectedChunks; i++) {
            Chunk chunk =  chunks.get(i);

            byteStream.write(chunk.getChunkData());
        }

        return byteStream.toByteArray();
    }
*/
    public ByteBuf getCombinedBytesOfChunks() throws IOException {

        if(!isComplete())
            return null;

        ByteBuf byteBuf = Unpooled.buffer();

        for(int i = 0; i < totalExpectedChunks; i++) {
            Chunk chunk =  chunks.get(i);

            byteBuf.writeBytes(chunk.getBytes());
        }

        return byteBuf;
    }


    public UUID getMessageID() {
        return messageID;
    }

    public int getTotalExpectedChunks() {
        return totalExpectedChunks;
    }

    public HashMap<Integer, Chunk> getChunks() {
        return chunks;
    }

    public TimeoutId getTimeoutId() {
        return timeoutId;
    }

    public void setTimeoutId(TimeoutId timeoutId) {
        this.timeoutId = timeoutId;
    }
}

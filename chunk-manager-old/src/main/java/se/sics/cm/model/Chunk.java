package se.sics.cm.model;

import java.util.UUID;

/**
 * Created by alidar on 10/21/14.
 */
public class Chunk {
    private UUID messageId;
    private int chunkNr;
    private int lastChunk;
    private byte[] chunk;

    public Chunk(UUID messageId, int chunkNr, int lastChunk, byte[] chunk) {
        this.messageId = messageId;
        this.chunkNr = chunkNr;
        this.lastChunk = lastChunk;
        this.chunk = chunk;
    }
}

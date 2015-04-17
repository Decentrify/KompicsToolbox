/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.p2ptoolbox.chunkmanager.util;

import java.util.UUID;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class ChunkedImpl implements Chunked {

    private final UUID messageId;
    private final int chunkNr;
    private final int lastChunk;

    public ChunkedImpl(UUID messageId, int chunkNr, int lastChunk) {
        this.messageId = messageId;
        this.chunkNr = chunkNr;
        this.lastChunk = lastChunk;
    }

    @Override
    public UUID getMessageId() {
        return messageId;
    }

    @Override
    public int getChunkNr() {
        return chunkNr;
    }

    @Override
    public int getLastChunk() {
        return lastChunk;
    }
}

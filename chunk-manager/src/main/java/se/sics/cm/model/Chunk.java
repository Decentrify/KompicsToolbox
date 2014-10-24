package se.sics.cm.model;

import java.util.UUID;

/**
 * Created by alidar on 10/21/14.
 */
public class Chunk {

    private int chunkID;
    private byte[] bytes;

    public Chunk(int chunkID, byte[] bytes) {
        this.chunkID = chunkID;
        this.bytes = bytes;
    }

    public int getChunkID() {
        return chunkID;
    }

    public void setChunkID(int chunkID) {
        this.chunkID = chunkID;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
}

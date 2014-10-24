package se.sics.cm;

import io.netty.buffer.ByteBuf;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.cm.events.ChunkManagerFrameDecoder;
import se.sics.cm.events.ChunkedMessage;
import se.sics.cm.events.ChunkedMessageFactory;
import se.sics.gvod.address.Address;
import se.sics.gvod.common.msgs.DirectMsgNettyFactory;
import se.sics.gvod.common.msgs.Encodable;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.config.VodConfig;
import se.sics.gvod.net.VodAddress;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by alidar on 10/24/14.
 */
public class ChunkManagerTest {

    @BeforeClass
    public static void setupClass() {

        DirectMsgNettyFactory.Base.setMsgFrameDecoder(ChunkManagerFrameDecoder.class);
    }

    @Test
   public void chunkedMessage() {

        InetAddress src = null;
        InetAddress dest = null;
        UUID messageID = UUID.randomUUID();
        int chunkID = 5;
        int totalChunks = 10;
        byte[] chunkData = "This is some data".getBytes();
        try {
            src = InetAddress.getByName("192.168.0.1");
            dest = InetAddress.getByName("192.168.0.2");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        VodAddress vodSrc = new VodAddress(new Address(src, 8081, 1), VodConfig.SYSTEM_OVERLAY_ID);
        VodAddress vodDest = new VodAddress(new Address(dest, 8081, 1), VodConfig.SYSTEM_OVERLAY_ID);

        ChunkedMessage msg = new ChunkedMessage(vodSrc, vodDest, messageID, chunkID, totalChunks, chunkData);

        try {
            ByteBuf buffer = msg.toByteArray();
            opCodeCorrect(buffer, msg);
            ChunkedMessage msgReceived = ChunkedMessageFactory.fromBuffer(buffer);

            assert(msgReceived.getMessageID().equals(messageID));
            assert(msgReceived.getChunkID() == chunkID);
            assert(msgReceived.getTotalChunks() == totalChunks);
            assert(Arrays.equals(msgReceived.getChunkData(), chunkData));

        } catch (MessageEncodingException ex) {
            Logger.getLogger(ChunkManagerTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MessageDecodingException ex) {
            Logger.getLogger(ChunkManagerTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void opCodeCorrect(ByteBuf buffer, Encodable msg) {
        byte type = buffer.readByte();
        assert (type == msg.getOpcode());
    }
}

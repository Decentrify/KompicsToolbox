package se.sics.cm.events;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.msgs.DirectMsgNettyFactory;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.net.msgs.DirectMsg;
import se.sics.gvod.net.util.UserTypesDecoderFactory;

import java.util.UUID;

/**
 * Created by alidar on 10/23/14.
 */
public class ChunkedMessageFactory extends DirectMsgNettyFactory.Oneway {

    public static ChunkedMessage fromBuffer(ByteBuf buffer) throws MessageDecodingException {

        return (ChunkedMessage) new ChunkedMessageFactory().decode(buffer);
    }


    @Override
    protected DirectMsg process(ByteBuf buffer) throws MessageDecodingException {
        long mostSignificantBitsOfMessageUUID = buffer.readLong();
        long leastSignificantBitsOfMessageUUID = buffer.readLong();
        int chunkID = buffer.readInt();
        int totalChunks = buffer.readInt();
        byte[] dataBytes = UserTypesDecoderFactory.readArrayBytes(buffer);

        return new ChunkedMessage(vodSrc,vodDest, new UUID(mostSignificantBitsOfMessageUUID, leastSignificantBitsOfMessageUUID),
                chunkID, totalChunks, dataBytes);
    }
}

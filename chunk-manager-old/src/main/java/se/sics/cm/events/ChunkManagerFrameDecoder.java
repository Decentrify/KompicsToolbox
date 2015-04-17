package se.sics.cm.events;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.net.BaseMsgFrameDecoder;
import se.sics.gvod.net.msgs.DirectMsg;
import se.sics.gvod.net.msgs.RewriteableMsg;

/**
 * Created by alidar on 10/23/14.
 */
public class ChunkManagerFrameDecoder extends BaseMsgFrameDecoder{

    public static final byte CHUNKED_MESSAGE  = -0x05;

    public ChunkManagerFrameDecoder() {
        super();
    }

    @Override
    protected RewriteableMsg decodeMsg(ChannelHandlerContext ctx,
                                       ByteBuf buffer) throws MessageDecodingException {
        // See if msg is part of parent project, if yes then return it.
        // Otherwise decode the msg here.
        RewriteableMsg msg = super.decodeMsg(ctx, buffer);
        if (msg != null) {
            return msg;
        }

        switch (opKod) {
            case CHUNKED_MESSAGE:
                return ChunkedMessageFactory.fromBuffer(buffer);
            default:
                break;
        }

        return null;
    }
}

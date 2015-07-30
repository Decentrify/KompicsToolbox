package se.sics.p2ptoolbox.util.helper;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

/**
 * Main factory containing helper functions for
 * decoding various objects.
 *
 * Created by babbar on 2015-07-30.
 */
public class UserDecoderFactory {

    private static Logger logger = LoggerFactory.getLogger(UserDecoderFactory.class);

    public static String readStringLength65536(ByteBuf buffer)  throws DecodingException {
        int len = readUnsignedIntAsTwoBytes(buffer);
        if (len == 0) {
            return null;
        } else {
            return readString(buffer, len);
        }
    }



    public static int readUnsignedIntAsTwoBytes(ByteBuf buffer) //            throws MessageDecodingException
    {
        byte[] bytes = new byte[2];
        buffer.readBytes(bytes);
        int temp0 = bytes[0] & 0xFF;
        int temp1 = bytes[1] & 0xFF;
        return ((temp0 << 8) + temp1);
    }


    private static String readString(ByteBuf buffer, int len)
            throws DecodingException {
        byte[] bytes = new byte[len];
        buffer.readBytes(bytes);
        try {
            return new String(bytes, Config.STRING_CHARSET);
        } catch (UnsupportedEncodingException ex) {
            logger.warn(ex.toString());
            throw new DecodingException(ex.getMessage());
        }
    }

}
